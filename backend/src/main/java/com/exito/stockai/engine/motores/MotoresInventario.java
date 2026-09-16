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

/** Familia de motores de INVENTARIO Y ALMACENAMIENTO. */
@Configuration
public class MotoresInventario {

    @Bean
    MotorMatematico invEoqDescuentos() {
        return new MotorGenerico("EOQ_DESCUENTOS", (p, c) -> {
            double d = c.num("D", 1200);
            double s = c.num("S", 50000);
            double i = c.num("i", 0.25);
            List<Double> tabla = c.lista("Q_i_C_i", List.of(0d, 4200d, 300d, 4200d, 650d, 4200d));
            c.validar(tabla.size() >= 2 && tabla.size() % 2 == 0, "EOQ_DESCUENTOS: la tabla (Q,C) debe tener pares");
            int tramos = tabla.size() / 2;
            double mejorQ = 0;
            double mejorTC = Double.MAX_VALUE;
            int mejorTramo = 0;
            double mejorPrecio = tabla.get(1);
            for (int t = 0; t < tramos; t++) {
                double qMin = tabla.get(2 * t);
                double precio = tabla.get(2 * t + 1);
                double q = Math.sqrt(2 * d * s / (i * precio));
                double qEf = Math.max(qMin, q);
                double tc = d * precio + (d / qEf) * s + (qEf / 2) * i * precio;
                if (tc < mejorTC) {
                    mejorTC = tc;
                    mejorQ = qEf;
                    mejorTramo = t + 1;
                    mejorPrecio = precio;
                }
            }
            var res = Calc.resultado("Q*", c.fmt(mejorQ), "TC*", c.fmt(mejorTC),
                    "tramo", mejorTramo, "precio", c.fmt(mejorPrecio));
            List<Map<String, Object>> serie = new ArrayList<>();
            for (int t = 0; t < tramos; t++) {
                serie.add(Calc.resultado("tramo", t + 1, "precio", c.fmt(tabla.get(2 * t + 1)),
                        "Qmin", c.fmt(tabla.get(2 * t))));
            }
            return ResultadoMotor.of("EOQ con descuentos por volumen", res, serie,
                    "El mejor tramo de descuento es el #" + mejorTramo + " (precio " + c.t(mejorPrecio)
                            + "), con lote " + c.fmt(mejorQ) + " y costo total anual " + c.t(mejorTC) + ".");
        });
    }

    @Bean
    MotorMatematico invEoqFaltantes() {
        return new MotorGenerico("EOQ_FALTANTES", (p, c) -> {
            double d = c.num("D", 1200);
            double s = c.num("S", 50000);
            double h = c.num("h", 3000);
            double b = c.num("b", 1500);
            c.validar(h > 0 && b > 0, "EOQ_FALTANTES: h y b deben ser > 0");
            double q = Math.sqrt(2 * d * s * (h + b) / (h * b));
            double back = h / (h + b) * q;
            double imax = q - back;
            double tc = d / q * s + (imax * imax / (2 * q)) * h + (back * back / (2 * q)) * b;
            var res = Calc.resultado("Q*", c.fmt(q), "faltanteMaximo", c.fmt(back),
                    "inventarioMaximo", c.fmt(imax), "TC", c.fmt(tc));
            List<Map<String, Object>> serie = new ArrayList<>();
            for (double f : new double[]{0.6, 0.8, 1.0, 1.2, 1.5}) {
                double qv = q * f;
                serie.add(Calc.resultado("q", c.fmt(qv), "costo", c.fmt(d / qv * s + (qv / 2) * h)));
            }
            return ResultadoMotor.of("EOQ con faltantes", res, serie,
                    "Con costo de faltante b=" + c.t(b) + ", el lote óptimo es " + c.fmt(q)
                            + " unidades con un faltante máximo planeado de " + c.t(back)
                            + " y costo total anual " + c.t(tc) + ".");
        });
    }

    @Bean
    MotorMatematico invEoqPerdible() {
        return new MotorGenerico("EOQ_PERDIBLE", (p, c) -> {
            double dDia = c.num("d_dia", 30);
            double t = c.num("T", 14);
            double rho = c.num("rho", 0.05);
            double costo = c.num("C", 4600);
            double q = dDia * t;
            double merma = q * rho;
            double cm = merma * costo;
            var res = Calc.resultado("Q*", c.fmt(q), "mermaEsperada", c.fmt(merma),
                    "costoMerma", c.fmt(cm), "vidaUtil", t);
            List<Map<String, Object>> serie = c.serieXY("dia", List.of(1d, 4d, 7d, 10d, 14d),
                    "stock", List.of(q, q * 0.7, q * 0.45, q * 0.2, 0d));
            return ResultadoMotor.of("EOQ perdible", res, serie,
                    "Para un ciclo de " + c.t(t) + " días se pide " + c.fmt(q) + " unidades; con una "
                            + "caducidad rho=" + c.t(rho) + " la merma esperada es " + c.t(merma)
                            + " unidades (" + c.t(cm) + " COP).");
        });
    }

    @Bean
    MotorMatematico invEoqEpq() {
        return new MotorGenerico("EOQ_EPQ", (p, c) -> {
            double d = c.num("D", 1200);
            double pDia = c.num("p", 40);
            double k = c.num("K", 15000);
            double h = c.num("h", 2500);
            double pAnual = pDia * 250;
            c.validar(pAnual > d, "EOQ_EPQ: la tasa de producción anual debe superar la demanda");
            double q = Math.sqrt(2 * d * k / (h * (1 - d / pAnual)));
            double imax = q * (1 - d / pAnual);
            double tc = d / q * k + (imax / 2) * h;
            var res = Calc.resultado("Q*", c.fmt(q), "inventarioMaximo", c.fmt(imax), "TC", c.fmt(tc));
            return ResultadoMotor.of("EPQ lote de producción", res, List.of(),
                    "El lote de producción óptimo es " + c.fmt(q) + " unidades; el inventario máximo "
                            + "alcanzado es " + c.t(imax) + " con costo total anual " + c.t(tc) + ".");
        });
    }

    @Bean
    MotorMatematico invEoqCapacidad() {
        return new MotorGenerico("EOQ_CAPACIDAD", (p, c) -> {
            List<Double> d = c.lista("D_i", List.of(1200d, 800d, 500d));
            List<Double> s = c.lista("S_i", List.of(15000d, 12000d, 10000d));
            List<Double> pu = c.lista("C_i", List.of(4200d, 4600d, 7600d));
            List<Double> w = c.lista("w_i", List.of(1d, 1d, 0.5d));
            double h = c.num("h", 0.25);
            double cap = c.num("W", 500);
            int n = d.size();
            List<Double> q = new ArrayList<>();
            double sum = 0;
            for (int j = 0; j < n; j++) {
                double qj = Math.sqrt(2 * d.get(j) * s.get(j) / (h * pu.get(j)));
                q.add(qj);
                sum += w.get(j) * qj;
            }
            double lambda = sum > 0 ? Math.min(1, cap / sum) : 1;
            for (int j = 0; j < n; j++) {
                q.set(j, q.get(j) * lambda);
            }
            var res = Calc.resultado("lambda", c.fmt(lambda), "lotes", q.stream().map(c::fmt).toList());
            List<Map<String, Object>> serie = new ArrayList<>();
            for (int j = 0; j < n; j++) {
                serie.add(Calc.resultado("articulo", j + 1, "demanda", c.fmt(d.get(j)), "lote", c.fmt(q.get(j))));
            }
            return ResultadoMotor.of("EOQ multi-producto con capacidad", res, serie,
                    "Sin restricción, la suma ponderada de lotes exige " + c.t(sum)
                            + " unidades; el multiplicador de Lagrange es lambda=" + c.t(lambda)
                            + ", por lo que los lotes ajustados son " + q.stream().map(c::fmt).toList() + ".");
        });
    }

    @Bean
    MotorMatematico invEoqImportacion() {
        return new MotorGenerico("EOQ_IMPORTACION", (p, c) -> {
            double d = c.num("D", 1200);
            double s = c.num("S", 50000);
            double cFob = c.num("C_fob", 20000);
            double flete = c.num("F", 800);
            double tau = c.num("tau", 0.07);
            double h = c.num("h", 0.25);
            int lt = c.entero("LT", 45);
            double cLanded = cFob * (1 + tau) + flete;
            double q = Math.sqrt(2 * d * s / (h * cLanded));
            double dDia = d / 365;
            double sigmaD = c.num("sigma_d", 3);
            double z = c.qnorm(c.num("CSL", 0.95));
            double rop = dDia * lt + z * sigmaD * Math.sqrt(lt);
            var res = Calc.resultado("Q*", c.fmt(q), "C_landed", c.fmt(cLanded),
                    "ROP", c.fmt(rop), "LT", lt);
            return ResultadoMotor.of("EOQ con lead time de importación", res, List.of(),
                    "El costo aterrizado es " + c.t(cLanded) + " COP, el lote óptimo " + c.fmt(q)
                            + " unidades y el punto de reorden con LT=" + lt + " días es " + c.t(rop) + ".");
        });
    }

    @Bean
    MotorMatematico invEoqProntoPago() {
        return new MotorGenerico("EOQ_PRONT_PAGO", (p, c) -> {
            double d = c.num("D", 1200);
            double s = c.num("S", 50000);
            double c0 = c.num("C", 100000);
            double desc = c.num("d", 0.02);
            double i = c.num("i", 0.12);
            int n = c.entero("n", 20);
            double cEf = c0 * (1 - desc);
            double hEf = i * cEf * n / 365 + 0.05 * cEf;
            double q = hEf > 0 ? Math.sqrt(2 * d * s / hEf) : 0;
            var res = Calc.resultado("Q*", c.fmt(q), "C_efectivo", c.fmt(cEf), "ahorro", c.fmt(c0 * desc));
            var serie = c.serieXY("q", List.of(q * 0.7, q, q * 1.3), "costo",
                    List.of(d / (q * 0.7) * s, d / q * s, d / (q * 1.3) * s));
            return ResultadoMotor.of("EOQ con pronto pago", res, serie,
                    "Al descontar " + c.t(desc * 100) + "% por pago a " + n
                            + " días, el costo efectivo baja a " + c.t(cEf) + " y el lote óptimo es "
                            + c.fmt(q) + " unidades.");
        });
    }

    @Bean
    MotorMatematico invEoqEstocastico() {
        return new MotorGenerico("EOQ_ESTOCASTICO", (p, c) -> {
            double d = c.num("D", 1200);
            double s = c.num("S", 50000);
            double h = c.num("h", 2500);
            double csl = c.num("CSL", 0.95);
            double sigma = c.num("sigma", 4);
            int lt = c.entero("LT", 7);
            double z = c.qnorm(csl);
            double q = Math.sqrt(2 * d * s / h);
            double ss = z * sigma * Math.sqrt(lt);
            double dDia = d / 365;
            double rop = dDia * lt + ss;
            var res = Calc.resultado("Q*", c.fmt(q), "stockSeguridad", c.fmt(ss),
                    "ROP", c.fmt(rop), "z", c.fmt(z));
            return ResultadoMotor.of("EOQ estocástico con nivel de servicio", res, List.of(),
                    "Con CSL=" + c.t(csl) + " (z=" + c.t(z) + ") y desviación de demanda " + c.t(sigma)
                            + ", el stock de seguridad es " + c.fmt(ss) + " unidades y el punto de reorden "
                            + c.t(rop) + ".");
        });
    }

    @Bean
    MotorMatematico invEoqFaltanteLineal() {
        return new MotorGenerico("EOQ_FALTANTE_LINEAL", (p, c) -> {
            double d = c.num("D", 1200);
            double s = c.num("S", 50000);
            double h = c.num("h", 3000);
            double b = c.num("p", 1500);
            double q = Math.sqrt(2 * d * s / h * (h + b) / b);
            double falt = h / (h + b) * q;
            var res = Calc.resultado("Q*", c.fmt(q), "z*", c.fmt(falt));
            return ResultadoMotor.of("EOQ con costo de faltante lineal", res, List.of(),
                    "El lote óptimo es " + c.fmt(q) + " y el inventario negativo máximo planeado "
                            + "(faltante z*) es " + c.t(falt) + " unidades.");
        });
    }

    @Bean
    MotorMatematico invEoqCapacidadProv() {
        return new MotorGenerico("EOQ_CAPACIDAD_PROV", (p, c) -> {
            double d = c.num("D", 1200);
            double s = c.num("S", 50000);
            double h = c.num("h", 2500);
            int k = c.entero("k", 50);
            double qBase = Math.sqrt(2 * d * s / h);
            double q = Math.ceil(qBase / k) * k;
            var res = Calc.resultado("Q*", c.fmt(q), "base", c.fmt(qBase), "multiplo", k);
            return ResultadoMotor.of("EOQ con capacidad de proveedor", res, List.of(),
                    "El lote base es " + c.fmt(qBase) + ", y redondeado al múltiplo del proveedor (k="
                            + k + ") queda en " + c.fmt(q) + " unidades.");
        });
    }

    @Bean
    MotorMatematico invPoliticaRq() {
        return new MotorGenerico("POLITICA_RQ", (p, c) -> {
            double d = c.num("d", 30);
            double sigma = c.num("sigma_d", 4);
            int lt = c.entero("LT", 7);
            double csl = c.num("CSL", 0.9);
            double q = c.num("Q", 120);
            double z = c.qnorm(csl);
            double r = d * lt + z * sigma * Math.sqrt(lt);
            var res = Calc.resultado("r", c.fmt(r), "Q", c.fmt(q), "CSL", c.fmt(csl));
            return ResultadoMotor.of("Política (r,Q) de reorden", res, List.of(),
                    "Con demanda diaria " + c.t(d) + ", LT de " + lt + " días y CSL " + c.t(csl)
                            + " (z=" + c.t(z) + "), el punto de reorden es r=" + c.fmt(r)
                            + " y el lote fijo Q=" + c.t(q) + ".");
        });
    }

    @Bean
    MotorMatematico invPoliticaSs() {
        return new MotorGenerico("POLITICA_SS", (p, c) -> {
            double d = c.num("d", 30);
            double sigma = c.num("sigma_d", 4);
            int lt = c.entero("LT", 7);
            double csl = c.num("CSL", 0.9);
            int t = c.entero("T", 30);
            double z = c.qnorm(csl);
            int n = Math.min(t, 5);
            double muLt = d * lt;
            double s = muLt + z * sigma * Math.sqrt(lt + (double) t / 30 * t);
            double sObj = s + Math.sqrt(2 * d * c.num("S", 50000) / c.num("h", 2500));
            var res = Calc.resultado("s", c.fmt(s), "S", c.fmt(sObj), "CSimplificado", c.fmt(muLt + z * sigma * Math.sqrt(lt)));
            return ResultadoMotor.of("Política (s,S) nivel min-max", res, List.of(),
                    "El umbral mínimo s es " + c.fmt(s) + " unidades y el objetivo máximo S es "
                            + c.t(sObj) + "; se ordena cuando el inventario cae bajo s.");
        });
    }

    @Bean
    MotorMatematico invPoliticaRs() {
        return new MotorGenerico("POLITICA_RS", (p, c) -> {
            double d = c.num("d", 30);
            double sigma = c.num("sigma_d", 4);
            int lt = c.entero("LT", 7);
            int rInt = c.entero("R", 7);
            double csl = c.num("CSL", 0.95);
            double z = c.qnorm(csl);
            double sigmaR = sigma * Math.sqrt(lt + rInt);
            double ss = z * sigmaR;
            double s = d * (lt + rInt) + ss;
            var res = Calc.resultado("S", c.fmt(s), "stockSeguridad", c.fmt(ss),
                    "R", rInt, "z", c.fmt(z));
            return ResultadoMotor.of("Política (R,S) revisión periódica", res, List.of(),
                    "Con revisión cada R=" + rInt + " días y LT=" + lt + ", el nivel objetivo es S="
                            + c.fmt(s) + " con stock de seguridad " + c.t(ss) + " unidades.");
        });
    }

    @Bean
    MotorMatematico invPoliticaTs() {
        return new MotorGenerico("POLITICA_TS", (p, c) -> {
            double sObj = c.num("S", 500);
            double fp = c.num("f_p", 0.15);
            double s1 = sObj;
            double s2 = sObj * (1 - fp);
            var res = Calc.resultado("S1", c.fmt(s1), "S2", c.fmt(s2), "f_p", c.fmt(fp));
            return ResultadoMotor.of("Política (T,S) dos niveles", res, List.of(),
                    "El nivel alto (S1) es " + c.fmt(s1) + " y el nivel bajo (S2) " + c.fmt(s2)
                            + ", activando reposición cada ciclo fijo cuando se llega al nivel bajo.");
        });
    }

    @Bean
    MotorMatematico invUpToLevel() {
        return new MotorGenerico("UP_TO_LEVEL", (p, c) -> {
            double s = c.num("S", 500);
            double ip = c.num("IP", 180);
            double q = Math.max(0, s - ip);
            var res = Calc.resultado("Q_t", c.fmt(q), "S", c.fmt(s), "IP", c.fmt(ip));
            return ResultadoMotor.of("Pedido ascendente (up-to-level)", res, List.of(),
                    "Con posición de inventario IP=" + c.t(ip) + " y objetivo S=" + c.fmt(s)
                            + ", se ordenan " + c.fmt(q) + " unidades (pedido de relleno).");
        });
    }

    @Bean
    MotorMatematico invPoliticaSemanal() {
        return new MotorGenerico("POLITICA_SEMANAL", (p, c) -> {
            double dW = c.num("d_w", 210);
            int lt = c.entero("LT", 7);
            double sigmaW = c.num("sigma_w", 20);
            double csl = c.num("CSL", 0.9);
            double z = c.qnorm(csl);
            double r = dW * (lt / 7.0) + z * sigmaW * Math.sqrt(lt / 7.0);
            var res = Calc.resultado("r", c.fmt(r), "d_semanal", c.fmt(dW));
            return ResultadoMotor.of("Punto de pedido con revisión semanal", res, List.of(),
                    "Con demanda semanal " + c.t(dW) + " y LT=" + lt + " días, el gatillo semanal es r="
                            + c.fmt(r) + " unidades.");
        });
    }

    @Bean
    MotorMatematico invStockSegFr() {
        return new MotorGenerico("STOCK_SEG_FR", (p, c) -> {
            double fr = c.num("FR", 0.98);
            double sigmaL = c.num("sigma_L", 4);
            double q = c.num("Q", 120);
            double objetivo = fr;
            double z = 1.0;
            if (objetivo >= 0.9999) {
                z = 3.5;
            } else {
                boolean ok = false;
                for (double zi = 0.0; zi <= 4.0; zi += 0.01) {
                    double esc = sigmaL * (c.phi(zi) - zi * (1 - c.phi(zi)));
                    double frCalc = 1 - esc / q;
                    if (frCalc >= objetivo - 0.005) {
                        z = zi;
                        ok = true;
                        break;
                    }
                }
                if (!ok) {
                    z = 4;
                }
            }
            double ss = z * sigmaL;
            c.validar(ss >= 0, "STOCK_SEG_FR: no se pudo resolver el stock de seguridad");
            var res = Calc.resultado("SS", c.fmt(ss), "z", c.fmt(z), "FR", c.fmt(fr));
            return ResultadoMotor.of("Stock de seguridad por fill rate", res, List.of(),
                    "Para un fill rate objetivo de " + c.t(fr * 100) + "% con desviación " + c.t(sigmaL)
                            + ", el stock de seguridad es " + c.fmt(ss) + " unidades (z=" + c.t(z) + ").");
        });
    }

    @Bean
    MotorMatematico invStockSegLt() {
        return new MotorGenerico("STOCK_SEG_LT", (p, c) -> {
            double csl = c.num("CSL", 0.95);
            double z = c.qnorm(csl);
            double muD = c.num("mu_d", 30);
            double sigmaD = c.num("sigma_d", 4);
            int lt = c.entero("LT", 7);
            double sigmaL = c.num("sigma_L", 2);
            double var = lt * sigmaD * sigmaD + muD * muD * sigmaL * sigmaL;
            double ss = z * Math.sqrt(var);
            var res = Calc.resultado("SS", c.fmt(ss), "sigma_lead", c.fmt(sigmaL), "z", c.fmt(z));
            return ResultadoMotor.of("Stock de seguridad con lead time variable", res, List.of(),
                    "Al incorporar la variabilidad del lead time (sigma=" + c.t(sigmaL) + "), el stock "
                            + "de seguridad sube a " + c.fmt(ss) + " unidades para CSL " + c.t(csl) + ".");
        });
    }

    @Bean
    MotorMatematico invHedgeStock() {
        return new MotorGenerico("HEDGE_STOCK", (p, c) -> {
            double de = c.num("D_e", 200);
            double ke = c.num("k_e", 0.35);
            double hs = de * ke;
            var res = Calc.resultado("HS", c.fmt(hs), "k_e", c.fmt(ke));
            return ResultadoMotor.of("Hedge stock (política crítica)", res, List.of(),
                    "Frente a un evento con demanda D_e=" + c.t(de) + ", la cobertura crítica es HS="
                            + c.fmt(hs) + " unidades (fracción " + c.t(ke) + ").");
        });
    }

    @Bean
    MotorMatematico invNewsvendor() {
        return new MotorGenerico("NEWSVENDOR", (p, c) -> {
            double costo = c.num("c", 3000);
            double precio = c.num("p", 7900);
            double salvage = c.num("s", 1500);
            double mu = c.num("mu", 200);
            double sigma = c.num("sigma", 40);
            double cri = (precio - costo) / (precio - salvage);
            double z = c.qnorm(cri);
            double q = mu + z * sigma;
            double epi = (precio - salvage) * (c.phi(z) * sigma + (q - mu) * (1 - c.phi(Math.min(z, 4)))) - (precio - salvage) * c.phi(Math.min(z, 4)) * (q - mu) - (precio - costo) * mu;
            var res = Calc.resultado("Q*", c.fmt(q), "probCritica", c.fmt(cri), "utilidadEsperada", c.fmt(epi));
            List<Map<String, Object>> serie = new ArrayList<>();
            for (double zz = -2; zz <= 1.5; zz += 0.5) {
                serie.add(Calc.resultado("z", c.fmt(zz), "Q", c.fmt(mu + zz * sigma)));
            }
            return ResultadoMotor.of("Newsvendor", res, serie,
                    "La probabilidad crítica es " + c.t(cri) + " (z=" + c.t(z)
                            + "); la cantidad óptima de temporada es " + c.fmt(q)
                            + " unidades con utilidad esperada de " + c.t(epi) + ".");
        });
    }

    @Bean
    MotorMatematico invNewsvendorMulti() {
        return new MotorGenerico("NEWSVENDOR_MULTI", (p, c) -> {
            List<Double> dem = c.lista("d_i", List.of(180d, 220d, 160d));
            double costo = c.num("c", 3200);
            double precio = c.num("p", 7900);
            double salvage = c.num("s", 1500);
            double mu = c.promedio(dem);
            double sigma = c.desviacion(dem);
            double cri = (precio - costo) / (precio - salvage);
            double z = c.qnorm(cri);
            double q = mu + z * sigma;
            double perdida = q * (costo - salvage) * c.phi(0);
            var res = Calc.resultado("Q*", c.fmt(q), "perdidaLiquidacion", c.fmt(perdida),
                    "probCritica", c.fmt(cri));
            return ResultadoMotor.of("Newsvendor multi-periodo", res, List.of(),
                    "Con demanda media " + c.t(mu) + " y desviación " + c.t(sigma)
                            + ", la política óptima acumula " + c.fmt(q) + " unidades y la pérdida por "
                            + "liquidación esperada es " + c.t(perdida) + " COP.");
        });
    }

    @Bean
    MotorMatematico invSilverMeal() {
        return new MotorGenerico("SILVER_MEAL", (p, c) -> {
            List<Double> dem = c.lista("d_i", List.of(50d, 60d, 70d, 80d, 50d, 60d));
            double a = c.num("A", 150);
            double h = c.num("h", 2);
            int mejorT = 1;
            double mejorC = Double.MAX_VALUE;
            double[][] tabla = new double[dem.size() + 1][dem.size() + 1];
            List<Double[]> costos = new ArrayList<>();
            for (int t = 1; t <= dem.size(); t++) {
                double acum = 0;
                for (int j = t; j <= dem.size(); j++) {
                    acum += dem.get(j - 1);
                    double inc = 0;
                    for (int kk = t; kk < j; kk++) {
                        inc += (kk - t + 1) * dem.get(kk);
                    }
                    double ct = (a + h * inc) / (j - t + 1);
                    if (ct < mejorC) {
                        mejorC = ct;
                        mejorT = j - t + 1;
                    }
                    costos.add(new Double[]{inc, ct});
                    tabla[t - 1][j - 1] = ct;
                }
            }
            double q = 0;
            for (int j = 0; j < mejorT && j < dem.size(); j++) {
                q += dem.get(j);
            }
            var res = Calc.resultado("T*", (double) mejorT, "Q*", c.fmt(q), "costoPorPeriodo", c.fmt(mejorC));
            return ResultadoMotor.of("Silver-Meal", res, List.of(),
                    "El horizonte óptimo del pedido es T*=" + mejorT + " periodos con cantidad "
                            + c.fmt(q) + " y costo promedio por periodo de " + c.t(mejorC) + ".");
        });
    }

    @Bean
    MotorMatematico invWagnerWhitin() {
        return new MotorGenerico("WAGNER_WHITIN", (p, c) -> {
            List<Double> dem = c.lista("d_i", List.of(50d, 60d, 70d, 80d, 50d, 60d, 70d));
            double a = c.num("A", 150);
            double h = c.num("h", 2);
            int n = dem.size();
            double[] costo = new double[n + 1];
            int[] sol = new int[n + 1];
            double inf = 1e18;
            for (int i = 1; i <= n; i++) {
                costo[i] = inf;
            }
            for (int i = 0; i < n; i++) {
                double subtotal = 0;
                for (int j = i; j < n; j++) {
                    subtotal += dem.get(j);
                    double hold = 0;
                    for (int hh = i; hh < j; hh++) {
                        hold += (hh - i + 1) * dem.get(hh + 1);
                    }
                    double val = costo[i] + a + h * hold;
                    if (val < costo[j + 1]) {
                        costo[j + 1] = val;
                        sol[j + 1] = i;
                    }
                }
            }
            List<Double> politica = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                politica.add(0.0);
            }
            int j = n;
            while (j > 0) {
                int start = sol[j];
                double q = 0;
                for (int k = start; k < j; k++) {
                    q += dem.get(k);
                }
                politica.set(start, q);
                j = start;
            }
            var res = Calc.resultado("politica", politica, "TC*", c.fmt(costo[n]));
            List<Map<String, Object>> serie = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                serie.add(Calc.resultado("periodo", i + 1, "demanda", c.fmt(dem.get(i)),
                        "pedido", c.fmt(politica.get(i))));
            }
            return ResultadoMotor.of("Wagner-Whitin dinámico", res, serie,
                    "La política de pedidos por programación dinámica alcanza un costo total mínimo "
                            + "de " + c.t(costo[n]) + " (setup A=" + c.t(a) + ", holding h=" + c.t(h) + ").");
        });
    }

    @Bean
    MotorMatematico invJrp() {
        return new MotorGenerico("JRP", (p, c) -> {
            List<Double> dem = c.lista("D_i", List.of(1200d, 800d, 500d));
            List<Double> sMinor = c.lista("S_i", List.of(5000d, 4000d, 3000d));
            List<Double> costo = c.lista("C_i", List.of(4200d, 4600d, 7600d));
            double s0 = c.num("S0", 80000);
            double h = c.num("h", 0.25);
            int n = dem.size();
            double sumMinor = 0;
            double sumDemH = 0;
            for (int i = 0; i < n; i++) {
                sumMinor += sMinor.get(i);
                sumDemH += dem.get(i) * costo.get(i) * h;
            }
            double t = sumDemH > 0 ? Math.sqrt(2 * sumMinor / sumDemH) : 0;
            List<Double> mult = new ArrayList<>();
            List<Double> qs = new ArrayList<>();
            double tc = 0;
            for (int i = 0; i < n; i++) {
                double qi = Math.sqrt(2 * dem.get(i) * sMinor.get(i) / (costo.get(i) * h));
                double ki = Math.max(1, Math.round(qi / (t * dem.get(i))));
                mult.add(ki);
                double k = Math.max(1, Math.round(qi / t / dem.get(i)));
                mult.set(i, k);
                double q = t * k * dem.get(i);
                if (q <= 0) {
                    q = qi;
                }
                qs.add(q);
                tc += (s0 / k + sMinor.get(i)) / t + t * costo.get(i) * h * dem.get(i) * k / 2;
            }
            var res = Calc.resultado("T*", c.fmt(t), "multiplos", mult, "lotes", qs, "TC", c.fmt(tc));
            List<Map<String, Object>> serie = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                serie.add(Calc.resultado("articulo", i + 1, "multiplo", mult.get(i), "lote", c.fmt(qs.get(i))));
            }
            return ResultadoMotor.of("JRP reposición conjunta", res, serie,
                    "El ciclo básico conjunto es T*=" + c.t(t) + " años con multiplicadores enteros "
                            + mult + "; el costo total conjunto es " + c.t(tc) + ".");
        });
    }

    @Bean
    MotorMatematico invGrupoReposicion() {
        return new MotorGenerico("GRUPO_REPOSICION", (p, c) -> {
            List<Double> dem = c.lista("D_i", List.of(1200d, 800d, 500d, 300d));
            List<Double> s = c.lista("S_i", List.of(15000d, 12000d, 10000d, 8000d));
            double h = c.num("h", 0.25);
            double base = 0;
            List<Double> ratios = new ArrayList<>();
            for (int i = 0; i < dem.size(); i++) {
                double q = Math.sqrt(2 * dem.get(i) * s.get(i) / (h * 5000));
                double qi = dem.get(i) > 0 ? q : 1;
                ratios.add(qi);
                base = Math.max(base, 1);
            }
            double minRatio = ratios.stream().min(Double::compare).orElse(1d);
            List<Integer> grupos = new ArrayList<>();
            for (double r : ratios) {
                grupos.add(Math.max(1, (int) Math.round(r / minRatio)));
            }
            var res = Calc.resultado("grupos", grupos);
            return ResultadoMotor.of("Grupos de reposición por proveedor", res, List.of(),
                    "Los artículos se agrupan por cadencia: multiplicadores enteros " + grupos
                            + " respecto al ciclo más frecuente del proveedor.");
        });
    }

    @Bean
    MotorMatematico invSourcingLocalImp() {
        return new MotorGenerico("SOURCING_LOCAL_IMP", (p, c) -> {
            double d = c.num("D", 1200);
            double cL = c.num("C_L", 5000);
            double cI = c.num("C_I", 4200);
            double fL = c.num("FL", 800);
            double fI = c.num("FI", 1200);
            double tau = c.num("tau", 0.05);
            double ltL = c.num("LT_L", 2);
            double ltI = c.num("LT_I", 45);
            double tcL = d * cL + fL + d * 0.02 * ltL;
            double tcI = d * cI * (1 + tau) + fI + d * 0.02 * ltI;
            String dec = tcL <= tcI ? "LOCAL" : "IMPORTADO";
            var res = Calc.resultado("TC_L", c.fmt(tcL), "TC_I", c.fmt(tcI), "decision", dec);
            return ResultadoMotor.of("Compra local vs importado", res, List.of(),
                    "El costo total local es " + c.t(tcL) + " y el importado " + c.t(tcI)
                            + "; la decisión óptima de abastecimiento es " + dec + ".");
        });
    }

    @Bean
    MotorMatematico invDualSourcing() {
        return new MotorGenerico("DUAL_SOURCING", (p, c) -> {
            double d = c.num("D", 1200);
            double c1 = c.num("C1", 4400);
            double c2 = c.num("C2", 5000);
            double p1 = c.num("p1", 0.95);
            double p2 = c.num("p2", 0.99);
            double x1 = c.num("x_1", 0.6);
            double x2 = 1 - x1;
            double costoEsperado = (c1 * x1 / p1 + c2 * x2 / p2) * d;
            var res = Calc.resultado("x_1", c.fmt(x1), "x_2", c.fmt(x2), "costoEsperado", c.fmt(costoEsperado));
            return ResultadoMotor.of("Dual sourcing (diversificación)", res, List.of(),
                    "Con la alocación x1=" + c.t(x1) + "/x2=" + c.t(x2) + " y confiabilidades "
                            + c.t(p1) + "/" + c.t(p2) + ", el costo esperado anual es " + c.t(costoEsperado) + " COP.");
        });
    }

    @Bean
    MotorMatematico invHoldingCost() {
        return new MotorGenerico("HOLDING_COST", (p, c) -> {
            double c0 = c.num("C", 4600);
            double i = c.num("i", 0.12);
            double b = c.num("b", 0.08);
            double o = c.num("o", 0.05);
            double v = c.num("V", 100000000);
            double tasa = i + b + o;
            double h = c0 * tasa;
            double costoTotal = v * tasa;
            var res = Calc.resultado("tasaHolding", c.fmt(tasa), "costoUnitario", c.fmt(h),
                    "costoTotal", c.fmt(costoTotal));
            return ResultadoMotor.of("Costo de mantener inventario", res, List.of(),
                    "El costo de mantenimiento suma capital (" + c.t(i * 100) + "%), bodega (" + c.t(b * 100)
                            + "%) y obsolescencia (" + c.t(o * 100) + "%) = " + c.t(tasa * 100)
                            + "% anual; sobre la base unitaria " + c.t(c0) + " representa " + c.t(h)
                            + " COP/unidad y sobre el inventario total " + c.t(costoTotal) + " COP.");
        });
    }

    @Bean
    MotorMatematico invDsi() {
        return new MotorGenerico("DSI", (p, c) -> {
            double i = c.num("I", 1200);
            double cogs = c.num("cogs", 300);
            double dsi = cogs > 0 ? i / cogs : 0;
            var res = Calc.resultado("DSI", c.fmt(dsi), "dias", c.fmt(dsi));
            return ResultadoMotor.of("DSI días de inventario", res, List.of(),
                    "Con inventario promedio de " + c.t(i) + " y costo de ventas diario de " + c.t(cogs)
                            + ", la rotación en días es DSI=" + c.t(dsi) + ".");
        });
    }

    @Bean
    MotorMatematico invWacc() {
        return new MotorGenerico("WACC", (p, c) -> {
            double v = c.num("V", 100000000);
            double r = c.num("r", 0.1);
            double cc = v * r;
            var res = Calc.resultado("CC", c.fmt(cc), "tasa", c.fmt(r));
            return ResultadoMotor.of("Costo de capital del inventario", res, List.of(),
                    "El capital comprometido en inventario (" + c.t(v) + " COP) a una tasa WACC de "
                            + c.t(r * 100) + "% genera un costo anual de " + c.t(cc) + " COP.");
        });
    }

    @Bean
    MotorMatematico invReemplazo() {
        return new MotorGenerico("REEMPLAZO", (p, c) -> {
            double stock = c.num("Q", 1000);
            double cr = c.num("C_r", 10000);
            double inflacion = c.num("inf", 0.06);
            double obs = c.num("obs", 0.02);
            double vr = stock * cr * (1 + inflacion) / (1 + obs);
            double ajuste = vr - stock * cr;
            var res = Calc.resultado("VR", c.fmt(vr), "ajuste", c.fmt(ajuste));
            return ResultadoMotor.of("Valuación a reposición", res, List.of(),
                    "Al reponer " + c.fmt(stock) + " unidades al costo actual " + c.t(cr)
                            + " (inflación " + c.t(inflacion * 100) + "%, obsolescencia " + c.t(obs * 100)
                            + "%), el valor de reposición es " + c.t(vr) + " COP, " + c.t(ajuste) + " sobre el libro.");
        });
    }
}