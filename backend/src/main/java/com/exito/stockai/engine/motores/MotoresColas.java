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

/** Familia de motores de PROCESOS Y COLAS EN PISO DE VENTA. */
@Configuration
public class MotoresColas {

    @Bean
    MotorMatematico colasMm1() {
        return new MotorGenerico("MM1", (p, c) -> {
            double lambda = c.num("lambda", 40);
            double mu = c.num("mu", 60);
            c.validar(lambda > 0 && mu > lambda, "COLAS M/M/1: se requiere lambda > 0 y mu > lambda");
            double rho = lambda / mu;
            double l = rho / (1 - rho);
            double lq = rho * rho / (1 - rho);
            double w = 1 / (mu - lambda);
            double wq = lambda / (mu * (mu - lambda));
            var res = Calc.resultado("utilizacion", c.fmt(rho), "L", c.fmt(l), "Lq", c.fmt(lq),
                    "W", c.fmt(w), "Wq", c.fmt(wq));
            List<Map<String, Object>> serie = new ArrayList<>();
            for (double muIt : new double[]{50, 60, 70, 80, 100}) {
                serie.add(Calc.resultado("mu", c.fmt(muIt), "rho", c.fmt(lambda / muIt), "Lq", c.fmt(lambda * lambda / (muIt * (muIt - lambda)))));
            }
            return ResultadoMotor.of("Cola M/M/1", res, serie,
                    "Con tasas " + c.t(lambda) + "/" + c.t(mu) + " clientes/h, la utilización es "
                            + c.t(rho * 100) + "%, la fila esperada " + c.t(lq) + " clientes y la espera "
                            + c.t(wq) + " horas.");
        });
    }

    @Bean
    MotorMatematico colasMmc() {
        return new MotorGenerico("MMC", (p, c) -> {
            double lambda = c.num("lambda", 120);
            double mu = c.num("mu", 30);
            int c2 = c.entero("c", 6);
            int k = c.entero("K", 40);
            c.validar(lambda > 0 && mu > 0 && c2 >= 1 && k >= c2, "COLAS M/M/c/K: parámetros inválidos");
            double rho = lambda / (c2 * mu);
            double[] pn = new double[k + 1];
            double p0 = 0;
            double[] probs = new double[k + 1];
            double cp = 1;
            for (int n = 0; n <= k; n++) {
                if (n <= c2) {
                    cp = Math.pow(c2 * rho, n) / factorial(n);
                } else {
                    cp = Math.pow(c2 * rho, n) / (factorial(c2) * Math.pow(c2, n - c2));
                }
                probs[n] = cp;
                p0 += cp;
            }
            p0 = 1 / p0;
            double pb = probs[k] * p0;
            double lq = 0;
            double l = 0;
            for (int n = 0; n <= k; n++) {
                double pn2 = probs[n] * p0;
                l += n * pn2;
                if (n >= c2) {
                    lq += (n - c2) * pn2;
                }
            }
            double lambdaEf = lambda * (1 - pb);
            double wq = lambdaEf > 0 ? lq / lambdaEf : 0;
            var res = Calc.resultado("utilizacion", c.fmt(rho), "P_bloqueo", c.fmt(pb),
                    "L", c.fmt(l), "Lq", c.fmt(lq), "Wq", c.fmt(wq));
            List<Map<String, Object>> serie = new ArrayList<>();
            for (int n = 0; n <= Math.min(k, 15); n += 1) {
                serie.add(Calc.resultado("n", n, "Pn", c.fmt(probs[n] * p0)));
            }
            return ResultadoMotor.of("Cola M/M/c/K", res, serie,
                    "Con " + c2 + " servidores y capacidad K=" + k + ", la probabilidad de bloqueo es "
                            + c.t(pb * 100) + "%, la fila esperada " + c.t(lq) + " clientes y la espera "
                            + c.t(wq) + " horas.");
        });
    }

    @Bean
    MotorMatematico colasMmck() {
        return new MotorGenerico("MMCK", (p, c) -> {
            double lambda = c.num("lambda", 120) * c.num("m_rob", 1.0518);
            double mu = c.num("mu", 30);
            int c2 = c.entero("c", 6);
            int k = c.entero("K", 40);
            double rho = lambda / (c2 * mu);
            double[] probs = new double[k + 1];
            double p0 = 0;
            double cp = 1;
            for (int n = 0; n <= k; n++) {
                if (n <= c2) {
                    cp = Math.pow(c2 * rho, n) / factorial(n);
                } else {
                    cp = Math.pow(c2 * rho, n) / (factorial(c2) * Math.pow(c2, n - c2));
                }
                probs[n] = cp;
                p0 += cp;
            }
            p0 = 1 / p0;
            double pb = probs[k] * p0;
            double lq = 0;
            double l = 0;
            for (int n = 0; n <= k; n++) {
                double pn = probs[n] * p0;
                l += n * pn;
                if (n >= c2) {
                    lq += (n - c2) * pn;
                }
            }
            var res = Calc.resultado("P_bloqueo", c.fmt(pb), "L", c.fmt(l), "Lq", c.fmt(lq),
                    "lambda_estres", c.fmt(lambda));
            return ResultadoMotor.of("Cola M/M/c/K robusta", res, List.of(),
                    "Bajo el escenario robusto (lambda escalado por m_rob), el bloqueo sube a "
                            + c.t(pb * 100) + "% y la fila esperada a " + c.t(lq) + " clientes.");
        });
    }

    @Bean
    MotorMatematico colasMg1() {
        return new MotorGenerico("MG1", (p, c) -> {
            double lambda = c.num("lambda", 40);
            double mu = c.num("mu", 50);
            double cv = c.num("Cv", 1.0);
            c.validar(mu > lambda, "COLAS M/G/1: se requiere mu > lambda");
            double rho = lambda / mu;
            double lq = rho * rho * (1 + cv * cv) / (2 * (1 - rho));
            double wq = lq / lambda;
            double l = rho + lq;
            var res = Calc.resultado("utilizacion", c.fmt(rho), "Lq", c.fmt(lq),
                    "Wq", c.fmt(wq), "L", c.fmt(l));
            List<Map<String, Object>> serie = new ArrayList<>();
            for (double cvIt : new double[]{0.5, 1.0, 1.5, 2.0}) {
                serie.add(Calc.resultado("Cv", c.fmt(cvIt), "Lq", c.fmt(rho * rho * (1 + cvIt * cvIt) / (2 * (1 - rho)))));
            }
            return ResultadoMotor.of("Cola M/G/1 (Pollaczek)", res, serie,
                    "Dado el coeficiente de variación del servicio Cv=" + c.t(cv)
                            + ", la fila esperada es " + c.t(lq) + " clientes con tiempo de espera "
                            + c.t(wq) + " horas.");
        });
    }

    @Bean
    MotorMatematico colasBatch() {
        return new MotorGenerico("BATCH_QUEUE", (p, c) -> {
            double lambda = c.num("lambda", 30);
            double mu = c.num("mu", 90);
            double b = c.num("b", 2);
            double lambdaEf = lambda * b;
            c.validar(mu > lambdaEf, "COLAS EN LOTE: la tasa efectiva excede la capacidad");
            double rho = lambdaEf / mu;
            double lq = rho * rho / (1 - rho);
            double wq = lq / lambda;
            var res = Calc.resultado("lambda_efectiva", c.fmt(lambdaEf), "rho", c.fmt(rho),
                    "Lq", c.fmt(lq), "Wq", c.fmt(wq));
            return ResultadoMotor.of("Cola con llegadas en lote", res, List.of(),
                    "Con lote medio b=" + c.t(b) + ", la llegada efectiva es " + c.t(lambdaEf)
                            + " clientes/h y la fila esperada " + c.t(lq) + " (Wq=" + c.t(wq) + " h).");
        });
    }

    @Bean
    MotorMatematico colasLittle() {
        return new MotorGenerico("LITTLE", (p, c) -> {
            double lambda = c.num("lambda", 120);
            double w = c.num("W", 0.25);
            double l = lambda * w;
            var res = Calc.resultado("L", c.fmt(l), "W_estadia", c.fmt(w), "lambda", c.fmt(lambda));
            List<Map<String, Object>> serie = new ArrayList<>();
            for (double wIt : new double[]{0.15, 0.25, 0.5, 1.0}) {
                serie.add(Calc.resultado("W", c.fmt(wIt), "L", c.fmt(lambda * wIt)));
            }
            return ResultadoMotor.of("Ley de Little", res, serie,
                    "Con " + c.t(lambda) + " clientes/h y " + c.t(w) + " h de estadía, en promedio hay "
                            + c.t(l) + " clientes dentro de la tienda.");
        });
    }

    @Bean
    MotorMatematico colasPrioridad() {
        return new MotorGenerico("PRIORIDAD", (p, c) -> {
            double lambda1 = c.num("lambda1", 30);
            double lambda2 = c.num("lambda2", 60);
            double mu = c.num("mu", 160);
            double rho = (lambda1 + lambda2) / mu;
            c.validar(rho < 1, "COLAS CON PRIORIDAD: la carga total debe ser < 1");
            double w1 = 1 / (mu * (1 - lambda1 / mu));
            double w2 = 1 / (mu * (1 - lambda1 / mu) * (1 - rho));
            var res = Calc.resultado("W_express", c.fmt(w1), "W_normal", c.fmt(w2),
                    "utilizacion", c.fmt(rho));
            return ResultadoMotor.of("Colas con prioridad (express)", res, List.of(),
                    "La fila express espera " + c.t(w1) + " h y la normal " + c.t(w2)
                            + " h con utilización total del " + c.t(rho * 100) + "%.");
        });
    }

    @Bean
    MotorMatematico colasCMaximo() {
        return new MotorGenerico("C_MAXIMO", (p, c) -> {
            double lambda = c.num("lambda", 120);
            double mu = c.num("mu", 30);
            double cServidor = c.num("c_s", 25000);
            double cEspera = c.num("c_e", 5000);
            int mejor = 1;
            double mejorTc = Double.MAX_VALUE;
            List<Map<String, Object>> serie = new ArrayList<>();
            for (int s = 1; s <= 12; s++) {
                double rho = lambda / (s * mu);
                if (rho >= 0.999) {
                    continue;
                }
                double p0 = p0ErlangC(lambda, mu, s);
                double lq = lqErlangC(lambda, mu, s, p0);
                double tc = s * cServidor + lambda * cEspera * (lq / lambda);
                serie.add(Calc.resultado("c", s, "TC", c.fmt(tc), "rho", c.fmt(rho)));
                if (tc < mejorTc) {
                    mejorTc = tc;
                    mejor = s;
                }
            }
            var res = Calc.resultado("c*", (double) mejor, "TC", c.fmt(mejorTc));
            return ResultadoMotor.of("Número óptimo de cajas", res, serie,
                    "El óptimo de balance costo de personal vs espera es c*=" + mejor
                            + " cajas con costo total " + c.t(mejorTc) + " COP/h.");
        });
    }

    @Bean
    MotorMatematico colasCapacidad() {
        return new MotorGenerico("CAPACIDAD_COLA", (p, c) -> {
            double capacidad = c.num("capacidad", 50);
            List<Double> demanda = c.lista("demanda", List.of(30d, 45d, 60d, 75d, 55d, 40d, 35d));
            double capacidadEf = c.num("cap_ef", capacidad * 0.85);
            List<Double> gaps = new ArrayList<>();
            double maxGap = -1e9;
            double minGap = 1e9;
            for (double d : demanda) {
                double g = d - capacidadEf;
                gaps.add(c.fmt(g));
                maxGap = Math.max(maxGap, g);
                minGap = Math.min(minGap, g);
            }
            List<Map<String, Object>> serie = new ArrayList<>();
            for (int i = 0; i < demanda.size(); i++) {
                serie.add(Calc.resultado("periodo", i + 1, "demanda", c.fmt(demanda.get(i)), "gap", gaps.get(i)));
            }
            var res = Calc.resultado("gapMaximo", c.fmt(maxGap), "gapMinimo", c.fmt(minGap),
                    "capacidadEfectiva", c.fmt(capacidadEf));
            return ResultadoMotor.of("Capacidad instalada vs demanda", res, serie,
                    "Frente a una capacidad efectiva de " + c.t(capacidadEf) + " clientes/h, el déficit "
                            + "pico es " + c.t(maxGap) + " clientes/h (serie de gaps G_t).");
        });
    }

    @Bean
    MotorMatematico colasSelfcheckout() {
        return new MotorGenerico("SELFCHECKOUT", (p, c) -> {
            double cAuto = c.num("c_auto", 20000);
            double cHum = c.num("c_hum", 35000);
            double lambda = c.num("lambda", 200);
            double mus = c.num("mu_auto", 25);
            double muh = c.num("mu_hum", 40);
            double mejor = 1.0;
            double mejorC = Double.MAX_VALUE;
            List<Map<String, Object>> serie = new ArrayList<>();
            for (double x = 0; x <= 1.0001; x += 0.1) {
                double cap = x * mus + (1 - x) * muh;
                if (cap <= 0) {
                    continue;
                }
                double costo = x * cAuto + (1 - x) * cHum + Math.max(0, lambda - cap * 8) * 2000;
                serie.add(Calc.resultado("x_auto", c.fmt(x), "costo", c.fmt(costo)));
                if (costo < mejorC) {
                    mejorC = costo;
                    mejor = x;
                }
            }
            var res = Calc.resultado("x_auto", c.fmt(mejor), "costoOperativo", c.fmt(mejorC));
            return ResultadoMotor.of("Self-checkout vs caja humana", res, serie,
                    "La mezcla óptima destina el " + c.t(mejor * 100) + "% de la capacidad a "
                            + "autoservicio, minimizando el costo operativo en " + c.t(mejorC) + " COP.");
        });
    }

    @Bean
    MotorMatematico colasCongestion() {
        return new MotorGenerico("CONGESTION", (p, c) -> {
            double flujo = c.num("flujo", 180);
            double anchura = c.num("ancho", 2.0);
            double longPas = c.num("largo", 20);
            double area = anchura * longPas;
            double rho = area > 0 ? flujo / (area * 3600) : 0;
            double v0 = c.num("v0", 1.3);
            double vEf = v0 * (1 - rho / 0.65);
            c.validar(rho < 0.65, "CONGESTION: la densidad supera la saturación");
            var res = Calc.resultado("rho_p", c.fmt(rho), "v_efectiva", c.fmt(vEf));
            return ResultadoMotor.of("Congestión de pasillos", res, List.of(),
                    "Con densidad " + c.t(rho) + " personas/m² (máx 0.65), la velocidad efectiva cae a "
                            + c.t(vEf) + " m/s, limitando la fluidez del pasillo.");
        });
    }

    @Bean
    MotorMatematico colasZonas() {
        return new MotorGenerico("ZONAS", (p, c) -> {
            List<Double> trafico = c.lista("trafico", List.of(0.22d, 0.18d, 0.15d, 0.12d, 0.09d, 0.07d, 0.04d, 0.0d));
            List<Double> rank = new ArrayList<>();
            for (int i = 0; i < trafico.size(); i++) {
                rank.add(c.fmt((double) (i + 1)));
            }
            List<Map<String, Object>> serie = new ArrayList<>();
            for (int i = 0; i < trafico.size(); i++) {
                serie.add(Calc.resultado("zona", "Z" + (i + 1), "participacion", c.fmt(trafico.get(i)), "rank", rank.get(i)));
            }
            var res = Calc.resultado("ranking", rank);
            return ResultadoMotor.of("Tráfico caliente y zonas de exhibición", res, serie,
                    "Las zonas calientes concentran " + c.t(trafico.get(0) * 100)
                            + "% del tráfico (top-Z1); use el ranking r_z para ubicar exhibiciones de alto margen.");
        });
    }

    @Bean
    MotorMatematico colasReneging() {
        return new MotorGenerico("RENEGING", (p, c) -> {
            double lambda = c.num("lambda", 150);
            double mu = c.num("mu", 30);
            int c2 = c.entero("c", 5);
            double ps = c.num("p_unit", 15000);
            double cap = c2 * mu;
            double exceso = Math.max(0, lambda - cap);
            double pr = lambda > 0 ? Math.min(0.4, exceso / lambda) : 0;
            double ls = pr * lambda * ps;
            var res = Calc.resultado("P_abandono", c.fmt(pr), "ventaPerdida", c.fmt(ls),
                    "capacidadInstalada", c.fmt(cap));
            return ResultadoMotor.of("Aglomeración y abandono", res, List.of(),
                    "Con exceso de " + c.t(exceso) + " clientes/h sobre la capacidad, la probabilidad de "
                            + "abandono es " + c.t(pr * 100) + "% y la venta perdida estimada " + c.t(ls) + " COP/h.");
        });
    }

    // ------------------------------------------------------------- helpers

    private double factorial(int n) {
        double r = 1;
        for (int i = 2; i <= n; i++) {
            r *= i;
        }
        return r;
    }

    private double p0ErlangC(double lambda, double mu, int s) {
        double rho = lambda / mu;
        double sum = 0;
        for (int n = 0; n < s; n++) {
            sum += Math.pow(rho, n) / factorial(n);
        }
        double ult = Math.pow(rho, s) / (factorial(s) * (1 - rho / s));
        return 1 / (sum + ult);
    }

    private double lqErlangC(double lambda, double mu, int s, double p0) {
        double rho = lambda / mu;
        double a = s * rho;
        double cE = Math.pow(a, s) / (factorial(s) * (1 - rho));
        return cE * p0 * (rho / s) / (s * (1 - rho / s) * (1 - rho / s));
    }
}