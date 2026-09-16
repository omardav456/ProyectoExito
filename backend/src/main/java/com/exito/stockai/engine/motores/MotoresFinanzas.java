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

/** Familia de motores de FINANZAS Y MARGEN. */
@Configuration
public class MotoresFinanzas {

    @Bean
    MotorMatematico finMargen() {
        return new MotorGenerico("MARGEN", (p, c) -> {
            double costo = c.num("costo", 12000);
            double precio = c.num("precio", 18900);
            double mb = (precio - costo) / precio;
            var res = Calc.resultado("MB", c.fmt(mb), "precio", c.fmt(precio), "costo", c.fmt(costo));
            List<Map<String, Object>> serie = new ArrayList<>();
            for (double pv : new double[]{16900, 18900, 21900}) {
                serie.add(Calc.resultado("precio", c.fmt(pv), "MB", c.fmt((pv - costo) / pv)));
            }
            return ResultadoMotor.of("Margen bruto por categoría", res, serie,
                    "El margen bruto de la categoría es del " + c.t(mb * 100)
                            + "% (precio " + c.t(precio) + " COP, costo " + c.t(costo) + " COP).");
        });
    }

    @Bean
    MotorMatematico finContribucion() {
        return new MotorGenerico("CONTRIBUCION", (p, c) -> {
            List<Double> margen = c.lista("margen_unit", List.of(0.35d, 0.30d, 0.42d));
            List<Double> venta = c.lista("venta_frente", List.of(4000000d, 3200000d, 2100000d));
            double totalInventario = c.num("inventario", 15000000);
            double contribucion = 0;
            for (int i = 0; i < Math.min(margen.size(), venta.size()); i++) {
                contribucion += margen.get(i) * venta.get(i);
            }
            double cf = contribucion / totalInventario;
            List<Map<String, Object>> serie = new ArrayList<>();
            for (int i = 0; i < venta.size(); i++) {
                serie.add(Calc.resultado("frente", "F" + (i + 1), "c_f", c.fmt(margen.get(i) * venta.get(i) / totalInventario)));
            }
            var res = Calc.resultado("c_f", c.fmt(cf), "contribucionTotal", c.fmt(contribucion));
            return ResultadoMotor.of("Contribución marginal del frente", res, serie,
                    "El frente aporta " + c.t(contribucion) + " COP de contribución, es decir "
                            + c.t(cf * 100) + "% de su costo de inventario.");
        });
    }

    @Bean
    MotorMatematico finGmroi() {
        return new MotorGenerico("GMROI", (p, c) -> {
            double margen = c.num("margen", 0.38);
            double ventas = c.num("ventas", 90000000);
            double invProm = c.num("inv_prom", 30000000);
            double gmroi = ventas * margen / invProm;
            var res = Calc.resultado("GMROI", c.fmt(gmroi), "ventas", c.fmt(ventas), "inv_prom", c.fmt(invProm));
            List<Map<String, Object>> serie = new ArrayList<>();
            for (double m : new double[]{0.30, 0.38, 0.45}) {
                serie.add(Calc.resultado("margen", c.fmt(m), "GMROI", c.fmt(ventas * m / invProm)));
            }
            return ResultadoMotor.of("GMROI", res, serie,
                    "Por cada COP invertido en inventario se generan " + c.t(gmroi)
                            + " COP de utilidad bruta anual (umbral retail típico: 1.0).");
        });
    }

    @Bean
    MotorMatematico finDpp() {
        return new MotorGenerico("DPP", (p, c) -> {
            double margen = c.num("margen", 0.38);
            double ventas = c.num("ventas", 90000000);
            double cOps = c.num("c_ops", 4000000);
            double cEsp = c.num("c_esp", 1500000);
            double cInv = c.num("c_inv", 6000000);
            double dpp = margen * ventas - cOps - cEsp - cInv;
            var res = Calc.resultado("DPP", c.fmt(dpp), "margenBruto", c.fmt(margen * ventas),
                    "c_esp", c.fmt(cEsp));
            return ResultadoMotor.of("DPP (direct product profit)", res, List.of(),
                    "El producto genera " + c.t(dpp) + " COP de rentabilidad directa tras "
                            + "descontar operación, espacio (" + c.t(cEsp) + ") e inventario.");
        });
    }

    @Bean
    MotorMatematico finPricing() {
        return new MotorGenerico("PRICING", (p, c) -> {
            double costo = c.num("costo", 12000);
            double eta = c.num("eta", -1.5);
            double pmax = c.num("p_max", 22000);
            double demandaRef = c.num("q_ref", 1000);
            double pRef = c.num("p_ref", 16000);
            double pEta = pRef * (eta / (eta + 1));
            double pOpt = Math.max(costo * 1.02, Math.min(pmax, pEta));
            double q = demandaRef * Math.pow(pOpt / pRef, eta);
            double pi = (pOpt - costo) * q;
            var res = Calc.resultado("p*", c.fmt(pOpt), "q*", c.fmt(q), "Pi*", c.fmt(pi));
            List<Map<String, Object>> serie = new ArrayList<>();
            for (double pv : new double[]{14000, 16000, pOpt, 20000}) {
                if (pv <= 0) {
                    continue;
                }
                double qv = demandaRef * Math.pow(pv / pRef, eta);
                serie.add(Calc.resultado("precio", c.fmt(pv), "q", c.fmt(qv), "Pi", c.fmt((pv - costo) * qv)));
            }
            return ResultadoMotor.of("Precio óptimo por margen", res, serie,
                    "Con elasticidad η=" + c.t(eta) + ", el precio óptimo es p*=" + c.t(pOpt)
                            + " COP con utilidad " + c.t(pi) + " COP.");
        });
    }

    @Bean
    MotorMatematico finMarkdown() {
        return new MotorGenerico("MARKDOWN", (p, c) -> {
            int t = c.entero("T", 8);
            double precio = c.num("precio", 25000);
            double costo = c.num("costo", 12000);
            double tasa = c.num("descuento_paso", 0.05);
            List<Double> precios = new ArrayList<>();
            List<Double> q = new ArrayList<>();
            double md = precio;
            double ventas = 0;
            for (int i = 0; i < t; i++) {
                md = i == 0 ? precio : md * (1 - tasa);
                double unidades = 900 - i * 80;
                double qd = Math.max(0, unidades);
                precios.add(c.fmt(md));
                q.add(c.fmt(qd));
                ventas += qd * md;
            }
            double liq = 0.3 * precio;
            List<Map<String, Object>> serie = new ArrayList<>();
            for (int i = 0; i < t; i++) {
                serie.add(Calc.resultado("periodo", i + 1, "p_t", precios.get(i), "q_t", q.get(i)));
            }
            var res = Calc.resultado("L", c.fmt(liq), "p_t", precios, "ingreso", c.fmt(ventas));
            return ResultadoMotor.of("Markdown óptimo de ciclo", res, serie,
                    "La rampa de descuentos p_t deja una liquidación residual de " + c.t(liq)
                            + " COP y unos ingresos de ciclo de " + c.t(ventas) + " COP.");
        });
    }
}