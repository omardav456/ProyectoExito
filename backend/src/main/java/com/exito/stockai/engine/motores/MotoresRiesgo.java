package com.exito.stockai.engine.motores;

import com.exito.stockai.engine.Calc;
import com.exito.stockai.engine.MotorGenerico;
import com.exito.stockai.engine.MotorMatematico;
import com.exito.stockai.engine.ResultadoMotor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Familia de motores de RIESGO E INCERTIDUMBRE. */
@Configuration
public class MotoresRiesgo {

    @Bean
    MotorMatematico riesVaR() {
        return new MotorGenerico("VAR", (p, c) -> {
            double alpha = c.num("alpha", 0.95);
            double h = c.num("h", 7);
            double mu = c.num("mu", 0.004);
            double sigma = c.num("sigma", 0.02);
            double z = c.qnorm(alpha);
            double varPct = z * sigma * Math.sqrt(h / 7) - mu * (h / 7);
            double varCop = c.num("V", 150000000) * varPct;
            var res = Calc.resultado("VaR", c.fmt(varCop), "VaR_pct", c.fmt(varPct), "z", c.fmt(z));
            List<Map<String, Object>> serie = new ArrayList<>();
            for (double al : new double[]{0.90, 0.95, 0.99}) {
                serie.add(Calc.resultado("alpha", c.fmt(al), "VaR", c.fmt(c.num("V", 150000000) * (c.qnorm(al) * sigma * Math.sqrt(h / 7) - mu * (h / 7)))));
            }
            return ResultadoMotor.of("VaR a " + c.t(Math.round(h)) + " días", res, serie,
                    "Con α=" + c.t(alpha) + " y horizonte de " + c.t(h) + " días, la pérdida máxima "
                            + "esperada es " + c.t(varCop) + " COP (" + c.t(varPct * 100) + "% del portafolio).");
        });
    }

    @Bean
    MotorMatematico riesCVaR() {
        return new MotorGenerico("CVAR", (p, c) -> {
            double alpha = c.num("alpha", 0.95);
            double mu = c.num("mu", 0.004);
            double sigma = c.num("sigma", 0.02);
            double h = c.num("h", 7);
            double vaR = c.num("VaR", 0.042);
            double phiZ = Math.exp(-0.5 * c.qnorm(alpha) * c.qnorm(alpha)) / Math.sqrt(2 * Math.PI);
            double cvarPct = -sigma * Math.sqrt(h / 7) * (phiZ / (1 - alpha)) - mu * (h / 7);
            double cvarCop = c.num("V", 150000000) * cvarPct;
            var res = Calc.resultado("CVaR", c.fmt(cvarCop), "CVaR_pct", c.fmt(cvarPct), "VaR", c.fmt(vaR));
            return ResultadoMotor.of("CVaR condicional", res, List.of(),
                    "La pérdida esperada condicional al peor (1-α)% es CVaR=" + c.t(cvarCop)
                            + " COP, que excede el VaR=" + c.t(vaR) + " al capturar la cola.");
        });
    }

    @Bean
    MotorMatematico riesMonteCarlo() {
        return new MotorGenerico("MONTE_CARLO", (p, c) -> {
            int n = c.entero("N", 1000);
            double mu = c.num("mu", 5000);
            double sigma = c.num("sigma", 800);
            java.util.Random rnd = c.num("semilla", 42) != 42 ? new java.util.Random(42) : new java.util.Random(7);
            double[] samples = new double[n];
            for (int i = 0; i < n; i++) {
                samples[i] = mu + sigma * rnd.nextGaussian();
            }
            java.util.Arrays.sort(samples);
            double p05 = samples[(int) Math.max(0, Math.ceil(0.05 * n) - 1)];
            double media = 0;
            for (double s : samples) {
                media += s;
            }
            media /= n;
            List<Map<String, Object>> serie = new ArrayList<>();
            for (double pct : new double[]{0.05, 0.25, 0.5, 0.75, 0.95}) {
                int idx = Math.min(n - 1, (int) Math.ceil(pct * n) - 1);
                serie.add(Calc.resultado("q", c.fmt(pct), "valor", c.fmt(samples[idx])));
            }
            var res = Calc.resultado("P05", c.fmt(p05), "mu", c.fmt(media), "N", (double) n);
            return ResultadoMotor.of("Monte Carlo de demanda", res, serie,
                    "Con " + n + " escenarios, el percentil 5 es " + c.t(p05)
                            + " unidades y la media simulada " + c.t(media));
        });
    }

    @Bean
    MotorMatematico riesEscenarios() {
        return new MotorGenerico("ESCENARIOS", (p, c) -> {
            double base = c.num("R_base", 0.18);
            double pes = c.num("R_pes", -0.05);
            double opt = c.num("R_opt", 0.34);
            double pPes = c.num("p_pes", 0.25);
            double pOpt = c.num("p_opt", 0.25);
            double pBase = 1 - pPes - pOpt;
            double eR = pBase * base + pPes * pes + pOpt * opt;
            double varR = pBase * Math.pow(base - eR, 2) + pPes * Math.pow(pes - eR, 2) + pOpt * Math.pow(opt - eR, 2);
            double sR = Math.sqrt(varR);
            var res = Calc.resultado("E[R]", c.fmt(eR), "sigma_R", c.fmt(sR), "p_base", c.fmt(pBase));
            List<Map<String, Object>> serie = new ArrayList<>();
            serie.add(Calc.resultado("escenario", "pesimista", "R", c.fmt(pes)));
            serie.add(Calc.resultado("escenario", "base", "R", c.fmt(base)));
            serie.add(Calc.resultado("escenario", "optimista", "R", c.fmt(opt)));
            return ResultadoMotor.of("Escenarios base/pesimista/optimista", res, serie,
                    "La rentabilidad esperada es E[R]=" + c.t(eR * 100) + "% con desviación entre "
                            + "escenarios de " + c.t(sR * 100) + "%.");
        });
    }

    @Bean
    MotorMatematico riesStress() {
        return new MotorGenerico("STRESS", (p, c) -> {
            double base = c.num("base", 50000);
            double caida = c.num("caida_ventas", 0.30);
            double marge = c.num("margen", 0.38);
            double perdida = base * caida * marge;
            double rMax = c.num("R_max", 15000000);
            double riesgo = Math.min(1.0, perdida / rMax);
            var res = Calc.resultado("L", c.fmt(perdida), "R_max", c.fmt(rMax), "riesgo", c.fmt(riesgo));
            List<Map<String, Object>> serie = new ArrayList<>();
            for (double ca : new double[]{0.15, 0.30, 0.50}) {
                serie.add(Calc.resultado("caida_ventas", c.fmt(ca), "L", c.fmt(base * ca * marge),
                        "riesgo", c.fmt(Math.min(1.0, base * ca * marge / rMax))));
            }
            return ResultadoMotor.of("Stress test de cadena", res, serie,
                    "Ante una caída del " + c.t(caida * 100) + "% en ventas, la pérdida sería "
                            + c.t(perdida) + " COP (" + c.t(riesgo * 100) + "% del riesgo tolerable).");
        });
    }

    @Bean
    MotorMatematico riesProveedor() {
        return new MotorGenerico("RIESGO_PROV", (p, c) -> {
            List<Double> probFalla = c.lista("p_falla", List.of(0.02d, 0.09d, 0.04d, 0.15d, 0.03d));
            List<Double> impacto = c.lista("impacto", List.of(0.3d, 0.7d, 0.5d, 1.0d, 0.4d));
            List<Double> scores = new ArrayList<>();
            double s = 0;
            for (int i = 0; i < Math.min(probFalla.size(), impacto.size()); i++) {
                double sc = probFalla.get(i) * impacto.get(i);
                scores.add(c.fmt(sc));
                s += sc;
            }
            List<Map<String, Object>> serie = new ArrayList<>();
            for (int i = 0; i < scores.size(); i++) {
                serie.add(Calc.resultado("proveedor", "P" + (i + 1), "p_i", c.fmt(probFalla.get(i)),
                        "impacto", c.fmt(impacto.get(i)), "score", scores.get(i)));
            }
            var res = Calc.resultado("S", c.fmt(s), "scoreProv", scores);
            return ResultadoMotor.of("Riesgo de fuente de proveedor", res, serie,
                    "El puntaje de riesgo acumulado es S=" + c.t(s)
                            + "; priorice a los proveedores de mayor p_i·impacto (P4 en el ejemplo).");
        });
    }

    @Bean
    MotorMatematico riesStockout() {
        return new MotorGenerico("STOCKOUT", (p, c) -> {
            double d = c.num("demanda_dia", 120);
            double lt = c.num("LT", 7);
            double sigma = c.num("sigma", 30);
            double rop = c.num("ROP", 940);
            double z = lt > 0 && sigma > 0 ? (rop - d * lt) / (sigma * Math.sqrt(lt)) : 0;
            double csl = c.phi(z);
            double alfa = 1 - csl;
            var res = Calc.resultado("alfa", c.fmt(alfa), "CSL", c.fmt(csl), "z", c.fmt(z));
            List<Map<String, Object>> serie = new ArrayList<>();
            for (double zz : new double[]{z, 1.28, 1.645, 2.33}) {
                serie.add(Calc.resultado("z", c.fmt(zz), "CSL", c.fmt(c.phi(zz))));
            }
            return ResultadoMotor.of("Probabilidad de agotamiento", res, serie,
                    "Con ROP=" + c.t(rop) + " sobre " + c.t(d * lt) + " de demanda esperada en el LT, "
                            + "CSL=" + c.t(csl * 100) + "% (riesgo de quiebre α=" + c.t(alfa * 100) + "%).");
        });
    }

    @Bean
    MotorMatematico riesEsc() {
        return new MotorGenerico("ESC", (p, c) -> {
            double d = c.num("demanda_dia", 120);
            double lt = c.num("LT", 7);
            double sigma = c.num("sigma", 30);
            double rop = c.num("ROP", 940);
            double z = lt > 0 && sigma > 0 ? (rop - d * lt) / (sigma * Math.sqrt(lt)) : 0;
            double fz = Math.exp(-0.5 * z * z) / Math.sqrt(2 * Math.PI);
            double esc = sigma * Math.sqrt(lt) * (fz - z * (1 - c.phi(z)));
            var res = Calc.resultado("ESC", c.fmt(esc), "Z", c.fmt(z));
            return ResultadoMotor.of("Faltante esperado (ESC)", res, List.of(),
                    "El faltante esperado por ciclo es ESC=" + c.t(esc)
                            + " unidades dado ROP=" + c.t(rop) + ".");
        });
    }

    @Bean
    MotorMatematico riesSensibilidad() {
        return new MotorGenerico("SENSIBILIDAD", (p, c) -> {
            double x = c.num("x0", 10000);
            double ef = c.num("costo_fijo", 3500000);
            double pv = c.num("precio", 18900);
            double cv = c.num("costo_var", 12000);
            double q = c.num("q", 800);
            double dx = c.num("dx", x * 0.01);
            double fx = (pv - cv) * x - ef;
            double pendiente = dx > 0 ? (((pv - cv) * (x + dx) - ef) - fx) / dx : 0;
            double actual = fx;
            var res = Calc.resultado("dR/dx", c.fmt(pendiente), "R_actual", c.fmt(actual));
            List<Map<String, Object>> serie = new ArrayList<>();
            for (double factor : new double[]{0.95, 1.0, 1.05, 1.1}) {
                serie.add(Calc.resultado("factor", c.fmt(factor), "R", c.fmt((pv - cv) * (x * factor) - ef)));
            }
            return ResultadoMotor.of("Sensibilidad univariada", res, serie,
                    "Cada unidad adicional de x aporta " + c.t(pendiente) + " COP de utilidad "
                            + "(dR/dx = margen de contribución).");
        });
    }
}