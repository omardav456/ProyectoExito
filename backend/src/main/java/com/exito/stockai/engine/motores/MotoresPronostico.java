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

/**
 * Familia de motores de PRONÓSTICO DE DEMANDA.
 * Consumen la serie bajo la clave "serie" (real si fuente=REAL, o simulada).
 */
@Configuration
public class MotoresPronostico {

    private List<Double> serie(Calc c) {
        return c.lista("serie", c.serieDemanda(24));
    }

    @Bean
    MotorMatematico motorSma() {
        return new MotorGenerico("SMA", true, (p, c) -> {
            int n = c.entero("N", 7);
            List<Double> y = serie(c);
            c.validar(n >= 1 && n <= y.size(),
                    "SMA: la ventana N debe estar entre 1 y el tamaño de la serie (" + y.size() + ")");
            int k = Math.min(n, y.size());
            double pron = 0;
            for (int i = y.size() - k; i < y.size(); i++) {
                pron += y.get(i);
            }
            pron /= k;
            double promHist = c.promedio(y);
            var res = Calc.resultado("pronostico", c.fmt(pron), "N", n, "promedioHistorico", c.fmt(promHist));
            return ResultadoMotor.of("Promedio móvil simple", res,
                    c.seriePronostico(y, List.of(pron)),
                    "Con una ventana de " + n + " periodos, la media móvil simple proyecta "
                            + c.t(pron) + " unidades para el siguiente periodo (promedio histórico " + c.t(promHist) + ").");
        });
    }

    @Bean
    MotorMatematico motorSmaDoble() {
        return new MotorGenerico("SMA_DOBLE", true, (p, c) -> {
            int n = c.entero("N", 7);
            List<Double> y = serie(c);
            c.validar(n >= 1 && n < y.size(), "SMA_DOBLE: la ventana N debe estar entre 1 y el tamaño de la serie");
            List<Double> m1 = new ArrayList<>();
            for (int i = n; i <= y.size(); i++) {
                double s = 0;
                for (int j = i - n; j < i; j++) {
                    s += y.get(j);
                }
                m1.add(s / n);
            }
            int k = m1.size() - n + 1;
            double m2 = 0;
            for (int i = 0; i < n; i++) {
                m2 += m1.get(m1.size() - 1 - i);
            }
            m2 /= n;
            double m1ult = m1.get(m1.size() - 1);
            double b = 2.0 / (n - 1) * (m1ult - m2);
            double pron = 2 * m1ult - m2 + b;
            var res = Calc.resultado("M1", c.fmt(m1ult), "M2", c.fmt(m2), "pendiente", c.fmt(b), "pronostico", c.fmt(pron));
            return ResultadoMotor.of("Media móvil doble", res,
                    c.seriePronostico(y, List.of(pron)),
                    "El suavizado doble separa nivel (" + c.t(m1ult) + ") y tendencia (" + c.t(b)
                            + ") proyectando " + c.t(pron) + " unidades para el siguiente periodo.");
        });
    }

    @Bean
    MotorMatematico motorWma() {
        return new MotorGenerico("WMA", true, (p, c) -> {
            int n = c.entero("N", 7);
            List<Double> y = serie(c);
            List<Double> pesos = c.lista("w_i", pesosLineales(n));
            c.validar(!pesos.isEmpty() && y.size() >= pesos.size(),
                    "WMA: los pesos w_i no deben superar la serie histórica");
            double suma = 0;
            double wsum = 0;
            int k = pesos.size();
            for (int i = 0; i < k; i++) {
                double w = pesos.get(i);
                suma += w * y.get(y.size() - k + i);
                wsum += w;
            }
            c.validar(wsum > 0, "WMA: la suma de pesos debe ser mayor que 0");
            double pron = suma / wsum;
            var res = Calc.resultado("pronostico", c.fmt(pron), "N", k);
            return ResultadoMotor.of("Media móvil ponderada", res,
                    c.seriePronostico(y, List.of(pron)),
                    "La media móvil ponderada asigna mayor peso a los periodos recientes y proyecta "
                            + c.t(pron) + " unidades para el próximo periodo.");
        });
    }

    @Bean
    MotorMatematico motorSes() {
        return new MotorGenerico("SES", true, (p, c) -> {
            double alpha = c.num("alpha", 0.3);
            List<Double> y = serie(c);
            c.validar(alpha > 0 && alpha < 1, "SES: alpha debe estar en (0,1)");
            double nivel = y.get(0);
            for (int i = 1; i < y.size(); i++) {
                nivel = alpha * y.get(i) + (1 - alpha) * nivel;
            }
            var res = Calc.resultado("pronostico", c.fmt(nivel), "nivel", c.fmt(nivel), "alpha", alpha);
            return ResultadoMotor.of("Suavizado exponencial simple", res,
                    c.seriePronostico(y, List.of(nivel)),
                    "Con alpha=" + c.t(alpha) + ", el nivel suavizado es " + c.t(nivel)
                            + " unidades, que es el pronóstico plano para los siguientes periodos.");
        });
    }

    @Bean
    MotorMatematico motorSesOpt() {
        return new MotorGenerico("SES_OPT", true, (p, c) -> {
            List<Double> y = serie(c);
            c.validar(y.size() > 3, "SES_OPT: se requiere una serie histórica de al menos 4 puntos");
            double mejor = 0.3;
            double mejorMse = Double.MAX_VALUE;
            for (double a = 0.01; a <= 0.99; a += 0.01) {
                double nivel = y.get(0);
                double mse = 0;
                for (int i = 1; i < y.size(); i++) {
                    double err = y.get(i) - nivel;
                    mse += err * err;
                    nivel = a * y.get(i) + (1 - a) * nivel;
                }
                mse /= (y.size() - 1);
                if (mse < mejorMse) {
                    mejorMse = mse;
                    mejor = a;
                }
            }
            double nivel = y.get(0);
            for (int i = 1; i < y.size(); i++) {
                nivel = mejor * y.get(i) + (1 - mejor) * nivel;
            }
            var res = Calc.resultado("alpha*", c.fmt(mejor), "MSE", c.fmt(mejorMse), "pronostico", c.fmt(nivel));
            return ResultadoMotor.of("SES optimizado", res,
                    c.seriePronostico(y, List.of(nivel)),
                    "El alpha que minimiza el error cuadrático medio en muestra es "
                            + c.t(mejor) + " (ECM " + c.t(mejorMse) + "), con pronóstico de "
                            + c.t(nivel) + " unidades.");
        });
    }

    @Bean
    MotorMatematico motorHolt() {
        return new MotorGenerico("HOLT", true, (p, c) -> {
            double alpha = c.num("alpha", 0.3);
            double beta = c.num("beta", 0.2);
            List<Double> y = serie(c);
            c.validar(alpha > 0 && alpha < 1 && beta > 0 && beta < 1, "HOLT: alpha y beta deben estar en (0,1)");
            double nivel = y.get(0);
            double tend = y.size() > 1 ? y.get(1) - y.get(0) : 0;
            for (int i = 1; i < y.size(); i++) {
                double nv = alpha * y.get(i) + (1 - alpha) * (nivel + tend);
                tend = beta * (nv - nivel) + (1 - beta) * tend;
                nivel = nv;
            }
            int h = c.entero("h", 1);
            double pron = nivel + h * tend;
            var res = Calc.resultado("nivel", c.fmt(nivel), "tendencia", c.fmt(tend),
                    "pronostico", c.fmt(pron), "h", h);
            return ResultadoMotor.of("Método Holt", res,
                    c.seriePronostico(y, List.of(pron)),
                    "Con nivel " + c.t(nivel) + " y tendencia " + c.t(tend)
                            + " por periodo, el pronóstico a " + h + " (s) es " + c.t(pron) + " unidades.");
        });
    }

    @Bean
    MotorMatematico motorHwAditivo() {
        return new MotorGenerico("HW_ADITIVO", true, (p, c) -> {
            double alpha = c.num("alpha", 0.3);
            double beta = c.num("beta", 0.1);
            double gamma = c.num("gamma", 0.2);
            int m = c.entero("m", 7);
            List<Double> y = serie(c);
            c.validar(m >= 2 && y.size() >= 2 * m, "HW_ADITIVO: m debe ser >= 2 y la serie debe cubrir al menos 2 ciclos");
            List<Double> s = estacionalInicial(y, m, 0);
            double nivel = y.get(0);
            double tend = y.size() > 1 ? y.get(1) - y.get(0) : 0;
            for (int i = 1; i < y.size(); i++) {
                double nv = alpha * (y.get(i) - s.get(i % m)) + (1 - alpha) * (nivel + tend);
                tend = beta * (nv - nivel) + (1 - beta) * tend;
                nivel = nv;
                s.set(i % m, gamma * (y.get(i) - nivel) + (1 - gamma) * s.get(i % m));
            }
            int h = c.entero("h", 1);
            List<Double> pred = new ArrayList<>();
            for (int j = 1; j <= h; j++) {
                pred.add(nivel + j * tend + s.get((y.size() + j - 1) % m));
            }
            var res = Calc.resultado("nivel", c.fmt(nivel), "tendencia", c.fmt(tend),
                    "pronostico", c.fmt(pred.get(pred.size() - 1)), "m", m);
            return ResultadoMotor.of("Holt-Winters aditivo", res,
                    c.seriePronostico(y, pred),
                    "El pronóstico a " + h + " periodo(s) es " + c.t(pred.get(pred.size() - 1))
                            + " unidades, con estacionalidad aditiva de periodo " + m + ".");
        });
    }

    @Bean
    MotorMatematico motorHwMult() {
        return new MotorGenerico("HW_MULT", true, (p, c) -> {
            double alpha = c.num("alpha", 0.3);
            double beta = c.num("beta", 0.1);
            double gamma = c.num("gamma", 0.15);
            int m = c.entero("m", 12);
            List<Double> y = serie(c);
            c.validar(m >= 2 && y.size() >= 2 * m, "HW_MULT: m debe ser >= 2 y la serie debe cubrir al menos 2 ciclos");
            List<Double> s = estacionalInicial(y, m, 1);
            double nivel = y.get(0) / Math.max(1e-6, s.get(0));
            double tend = y.size() > 1 ? y.get(1) - y.get(0) : 0;
            for (int i = 1; i < y.size(); i++) {
                double nv = alpha * (y.get(i) / Math.max(1e-6, s.get(i % m))) + (1 - alpha) * (nivel + tend);
                tend = beta * (nv - nivel) + (1 - beta) * tend;
                nivel = nv;
                s.set(i % m, gamma * (y.get(i) / Math.max(1e-6, nivel)) + (1 - gamma) * s.get(i % m));
            }
            int h = c.entero("h", 1);
            List<Double> pred = new ArrayList<>();
            for (int j = 1; j <= h; j++) {
                pred.add((nivel + j * tend) * s.get((y.size() + j - 1) % m));
            }
            double pron = pred.get(pred.size() - 1);
            var res = Calc.resultado("nivel", c.fmt(nivel), "tendencia", c.fmt(tend),
                    "factorEstacional", c.fmt(s.get((y.size() + h - 1) % m)), "pronostico", c.fmt(pron));
            return ResultadoMotor.of("Holt-Winters multiplicativo", res,
                    c.seriePronostico(y, pred),
                    "El pronóstico a " + h + " periodo(s) es " + c.t(pron)
                            + " unidades; el factor estacional del periodo es " + c.t(s.get((y.size() + h - 1) % m)) + ".");
        });
    }

    @Bean
    MotorMatematico motorCroston() {
        return new MotorGenerico("CROSTON", true, (p, c) -> {
            double alpha = c.num("alpha", 0.1);
            List<Double> y = serie(c);
            c.validar(alpha > 0 && alpha < 1, "CROSTON: alpha debe estar en (0,1)");
            double z = 0;
            double q = 0;
            boolean ini = false;
            int gap = 0;
            for (double v : y) {
                if (v > 0) {
                    if (!ini) {
                        z = v;
                        q = Math.max(1, gap + 1);
                        ini = true;
                    } else {
                        z = alpha * v + (1 - alpha) * z;
                        q = alpha * (gap + 1) + (1 - alpha) * q;
                    }
                    gap = 0;
                } else {
                    gap++;
                }
            }
            double pron = q > 0 ? z / q : 0;
            var res = Calc.resultado("pr_monto", c.fmt(z), "pr_intervalo", c.fmt(q), "pronostico", c.fmt(pron));
            return ResultadoMotor.of("Croston", res,
                    c.seriePronostico(y, List.of(pron)),
                    "La demanda media por periodo con demanda (z=" + c.t(z)
                            + ") y el intervalo medio (q=" + c.t(q) + ") dan un pronóstico de "
                            + c.t(pron) + " unidades/periodo para demanda intermitente.");
        });
    }

    @Bean
    MotorMatematico motorCrostonCeros() {
        return new MotorGenerico("CROSTON_CEROS", true, (p, c) -> {
            double alpha = c.num("alpha", 0.1);
            List<Double> y = serie(c);
            double z = 0;
            double q = 0;
            boolean ini = false;
            int gap = 0;
            for (double v : y) {
                if (v > 0) {
                    if (!ini) {
                        z = v;
                        q = Math.max(1, gap + 1);
                        ini = true;
                    } else {
                        z = alpha * v + (1 - alpha) * z;
                        q = alpha * (gap + 1) + (1 - alpha) * q;
                    }
                    gap = 0;
                } else {
                    gap++;
                }
            }
            long ceros = y.stream().filter(v -> v == 0).count();
            double pron = q > 0 ? z / q * (1 - (double) ceros / y.size() * alpha) : 0;
            var res = Calc.resultado("pronostico", c.fmt(pron), "proporcionCeros", c.fmt((double) ceros / y.size()));
            return ResultadoMotor.of("Croston con ceros", res,
                    c.seriePronostico(y, List.of(pron)),
                    "Corrigiendo por ventas nulas (proporción " + c.t((double) ceros / y.size())
                            + "), el pronóstico ajustado es " + c.t(pron) + " unidades/periodo.");
        });
    }

    @Bean
    MotorMatematico motorSba() {
        return new MotorGenerico("SBA", true, (p, c) -> {
            double alpha = c.num("alpha", 0.1);
            List<Double> y = serie(c);
            double z = 0;
            double q = 0;
            boolean ini = false;
            int gap = 0;
            for (double v : y) {
                if (v > 0) {
                    if (!ini) {
                        z = v;
                        q = Math.max(1, gap + 1);
                        ini = true;
                    } else {
                        z = alpha * v + (1 - alpha) * z;
                        q = alpha * (gap + 1) + (1 - alpha) * q;
                    }
                    gap = 0;
                } else {
                    gap++;
                }
            }
            double k = 1 - alpha / 2;
            double pron = q > 0 ? k * z / q : 0;
            double fC = q > 0 ? z / q : 0;
            var res = Calc.resultado("k", c.fmt(k), "pronosticoCroston", c.fmt(fC), "pronostico", c.fmt(pron));
            return ResultadoMotor.of("SBA (Syntetos-Boylan)", res,
                    c.seriePronostico(y, List.of(pron)),
                    "El factor de corrección SBA " + c.t(k) + " ajusta el pronóstico base (" + c.t(fC)
                            + ") a " + c.t(pron) + " unidades/periodo, reduciendo el sesgo de Croston.");
        });
    }

    @Bean
    MotorMatematico motorTevar() {
        return new MotorGenerico("TEVAR", true, (p, c) -> {
            List<Double> y = serie(c);
            double tau = c.num("tau", 7);
            double prom = c.promedio(y);
            long nulos = y.stream().filter(v -> v == 0).count();
            double prop = (double) nulos / y.size();
            double ajuste = Math.max(0, 1 - tau * prop);
            double pron = prom * ajuste;
            var res = Calc.resultado("pronostico", c.fmt(pron), "umbral", tau, "ajuste", c.fmt(ajuste));
            return ResultadoMotor.of("TEVAR", res,
                    c.seriePronostico(y, List.of(pron)),
                    "Con umbral tau=" + c.t(tau) + " y " + nulos + " ceros en la serie, el factor de "
                            + "corrección por ceros es " + c.t(ajuste) + " y el pronóstico por periodo es "
                            + c.t(pron) + " unidades.");
        });
    }

    @Bean
    MotorMatematico motorAgregacionTemporal() {
        return new MotorGenerico("AGREGACION_TEMPORAL", true, (p, c) -> {
            List<Double> y = serie(c);
            int maxK = Math.min(12, y.size() / 2);
            double mejor = 0;
            double mejorCv = Double.MAX_VALUE;
            for (int k = 1; k <= maxK; k++) {
                List<Double> agg = new ArrayList<>();
                for (int i = 0; i + k <= y.size(); i += k) {
                    double s = 0;
                    for (int j = i; j < i + k; j++) {
                        s += y.get(j);
                    }
                    agg.add(s);
                }
                if (agg.size() < 2) {
                    continue;
                }
                double m = c.promedio(agg);
                double sd = c.desviacion(agg);
                double cv = m > 0 ? sd / m : 0;
                if (cv < mejorCv) {
                    mejorCv = cv;
                    mejor = k;
                }
            }
            int mOpt = (int) Math.round(mejor);
            var res = Calc.resultado("nivelOptimo", mOpt, "CV", c.fmt(mejorCv));
            List<Map<String, Object>> serie = new ArrayList<>();
            for (int i = 0; i < y.size(); i++) {
                serie.add(Calc.resultado("periodo", i + 1, "historico", c.fmt(y.get(i))));
            }
            return ResultadoMotor.of("Agregación temporal (Across-Time)", res, serie,
                    "Agregando la serie en bloques de " + mOpt + " periodos se minimiza la variabilidad "
                            + "(CV=" + c.t(mejorCv) + "), nivel óptimo para pronóstico agregado.");
        });
    }

    @Bean
    MotorMatematico motorDescomposicionAditiva() {
        return new MotorGenerico("DESCOMPOSICION_ADITIVA", true, (p, c) -> {
            int m = c.entero("m", 7);
            List<Double> y = serie(c);
            c.validar(y.size() >= 2 * m, "DESCOMPOSICION: la serie debe cubrir al menos 2 ciclos de largo m");
            List<Double> tend = tendenciaCentrada(y, m);
            List<Double> s = new ArrayList<>();
            for (int i = 0; i < y.size(); i++) {
                s.add(i < tend.size() && !Double.isNaN(tend.get(i)) ? y.get(i) - tend.get(i) : Double.NaN);
            }
            List<Double> ind = new ArrayList<>();
            for (int k = 0; k < m; k++) {
                List<Double> valores = new ArrayList<>();
                for (int i = k; i < s.size(); i += m) {
                    if (!Double.isNaN(s.get(i))) {
                        valores.add(s.get(i));
                    }
                }
                double sum = 0;
                for (double v : valores) {
                    sum += v;
                }
                ind.add(valores.isEmpty() ? 0 : sum / valores.size());
            }
            double sumaInd = 0;
            for (double v : ind) {
                sumaInd += v;
            }
            double mediaInd = sumaInd / m;
            for (int k = 0; k < m; k++) {
                ind.set(k, ind.get(k) - mediaInd);
            }
            int h = c.entero("h", 1);
            double pron = (tend.get(tend.size() - 1) + h * (tend.get(tend.size() - 1) - tend.get(Math.max(0, tend.size() - m))))
                    + ind.get((y.size() + h - 1) % m);
            var res = Calc.resultado("tendencia", c.fmt(tend.get(tend.size() - 1)),
                    "estacionalidad", c.fmt(ind.get((y.size() + h - 1) % m)), "pronostico", c.fmt(pron));
            List<Map<String, Object>> serie = new ArrayList<>();
            for (int i = 0; i < y.size(); i++) {
                serie.add(Calc.resultado("periodo", i + 1, "observado", c.fmt(y.get(i)),
                        i < tend.size() ? "tendencia" : "tendencia", i < tend.size() ? c.fmt(tend.get(i)) : null));
            }
            return ResultadoMotor.of("Descomposición clásica aditiva", res, serie,
                    "La tendencia del último periodo es " + c.t(tend.get(tend.size() - 1))
                            + " y el índice estacional correspondiente " + c.t(ind.get((y.size() + h - 1) % m))
                            + "; el pronóstico por descomposición es " + c.t(pron) + " unidades.");
        });
    }

    @Bean
    MotorMatematico motorStl() {
        return new MotorGenerico("STL", true, (p, c) -> {
            int m = c.entero("m", 7);
            List<Double> y = serie(c);
            c.validar(y.size() >= 2 * m, "STL: la serie debe cubrir al menos 2 ciclos de largo m");
            List<Double> tend = tendenciaCentrada(y, m);
            List<Double> resid = new ArrayList<>();
            for (int i = 0; i < y.size(); i++) {
                resid.add(i < tend.size() && !Double.isNaN(tend.get(i)) ? y.get(i) - tend.get(i) : 0);
            }
            double mediana = mediana(resid);
            double mad = 1.4826 * medianaAbs(resid, mediana);
            List<Double> w = new ArrayList<>();
            double maxW = 0;
            for (double r : resid) {
                double u = mad > 0 ? Math.abs(r - mediana) / (6 * mad) : 0;
                double wt = Math.max(0, 1 - u * u);
                wt *= wt;
                w.add(wt);
                maxW = Math.max(maxW, wt);
            }
            double peso = maxW;
            int h = c.entero("h", 1);
            double promedio = c.promedio(y.subList(Math.max(0, y.size() - m), y.size()));
            double pron = promedio;
            var res = Calc.resultado("tendencia", c.fmt(tend.get(tend.size() - 1)),
                    "pesoRobusto", c.fmt(peso), "pronostico", c.fmt(pron));
            List<Map<String, Object>> serie = new ArrayList<>();
            for (int i = 0; i < y.size(); i++) {
                serie.add(Calc.resultado("periodo", i + 1, "observado", c.fmt(y.get(i)),
                        "peso", c.fmt(w.get(i))));
            }
            return ResultadoMotor.of("Descomposición STL", res, serie,
                    "El peso robusto máximo (bisquare) es " + c.t(peso)
                            + "; la tendencia STL del último periodo es " + c.t(tend.get(tend.size() - 1))
                            + " y el pronóstico a " + h + " periodo(s) es " + c.t(pron) + " unidades.");
        });
    }

    @Bean
    MotorMatematico motorEstacionalidadCal() {
        return new MotorGenerico("ESTACIONALIDAD_CAL", true, (p, c) -> {
            List<Double> y = serie(c);
            double[] dia = new double[7];
            int[] nDia = new int[7];
            for (int i = 0; i < y.size(); i++) {
                int dow = i % 7;
                dia[dow] += y.get(i);
                nDia[dow]++;
            }
            double prom = c.promedio(y);
            List<Double> iDia = new ArrayList<>();
            for (int d = 0; d < 7; d++) {
                iDia.add(nDia[d] > 0 ? dia[d] / nDia[d] / prom : 1);
            }
            double iMes = iDia.get(0);
            var res = Calc.resultado("indiceDiario", iDia, "indiceMensual", c.fmt(iMes));
            List<Map<String, Object>> serie = c.serieXY("dia", List.of(1d, 2d, 3d, 4d, 5d, 6d, 7d), "indice", iDia);
            return ResultadoMotor.of("Tendencias estacionales calendario", res, serie,
                    "Los índices diarios (dom=1) son " + formatArr(iDia, c)
                            + "; el índice mensual representativo es " + c.t(iMes) + ".");
        });
    }

    @Bean
    MotorMatematico motorArima() {
        return new MotorGenerico("ARIMA", true, (p, c) -> {
            int pOrd = c.entero("p", 1);
            int dOrd = c.entero("d", 1);
            int qOrd = c.entero("q", 1);
            List<Double> y = serie(c);
            List<Double> w = new ArrayList<>(y);
            for (int i = 0; i < dOrd && w.size() > 2; i++) {
                List<Double> d = new ArrayList<>();
                for (int j = 1; j < w.size(); j++) {
                    d.add(w.get(j) - w.get(j - 1));
                }
                w = d;
            }
            c.validar(w.size() > Math.max(pOrd, qOrd), "ARIMA: la serie es demasiado corta para (p,d,q)");
            double phi = 0;
            if (pOrd >= 1) {
                double s1 = 0;
                double s2 = 0;
                for (int i = 1; i < w.size(); i++) {
                    s1 += (w.get(i - 1) - c.promedio(w.subList(0, w.size() - 1))) * (w.get(i) - c.promedio(w.subList(1, w.size())));
                    s2 += Math.pow(w.get(i - 1) - c.promedio(w.subList(0, w.size() - 1)), 2);
                }
                phi = s2 > 0 ? s1 / s2 : 0;
            }
            double theta = qOrd >= 1 ? 0.2 : 0;
            double resid = 0;
            List<Double> residuos = new ArrayList<>();
            for (int i = 1; i < w.size(); i++) {
                resid = w.get(i) - phi * w.get(i - 1) + theta * resid;
                residuos.add(resid);
            }
            double mse = 0;
            for (double r : residuos) {
                mse += r * r;
            }
            mse = residuos.isEmpty() ? 0 : mse / residuos.size();
            double wPron = phi * w.get(w.size() - 1) - theta * resid;
            double nivelBase = y.get(y.size() - 1);
            double pron = nivelBase + (wPron - w.get(w.size() - 1));
            var res = Calc.resultado("phi", c.fmt(phi), "theta", c.fmt(theta), "MSE", c.fmt(mse),
                    "pronostico", c.fmt(pron));
            return ResultadoMotor.of("ARIMA(1,1,1)", res,
                    c.seriePronostico(y, List.of(pron)),
                    "Con phi=" + c.t(phi) + " y theta=" + c.t(theta) + " (ECM " + c.t(mse)
                            + "), el pronóstico de un paso es " + c.t(pron) + " unidades.");
        });
    }

    @Bean
    MotorMatematico motorSarima() {
        return new MotorGenerico("SARIMA", true, (p, c) -> {
            int m = c.entero("m", 7);
            int P = c.entero("P", 1);
            List<Double> y = serie(c);
            double phi = 0;
            if (y.size() > 1) {
                double s1 = 0;
                double s2 = 0;
                double m0 = c.promedio(y.subList(0, y.size() - 1));
                double m1 = c.promedio(y.subList(1, y.size()));
                for (int i = 1; i < y.size(); i++) {
                    s1 += (y.get(i - 1) - m0) * (y.get(i) - m1);
                    s2 += Math.pow(y.get(i - 1) - m0, 2);
                }
                phi = s2 > 0 ? s1 / s2 : 0;
            }
            double phiS = 0;
            if (y.size() > m + 1) {
                double s1 = 0;
                double s2 = 0;
                for (int i = m; i < y.size(); i++) {
                    s1 += (y.get(i - m) - c.promedio(y)) * (y.get(i) - c.promedio(y));
                    s2 += Math.pow(y.get(i - m) - c.promedio(y), 2);
                }
                phiS = s2 > 0 ? s1 / s2 : 0;
            }
            double resid = 0;
            for (int i = 1; i < y.size(); i++) {
                double esperado = phi * y.get(i - 1) + (i >= m ? phiS * (y.get(i - m) - (phi * y.get(i - m - 1 < 0 ? 0 : i - m - 1))) : 0);
                resid = y.get(i) - esperado;
            }
            double pron = phi * y.get(y.size() - 1) + (y.size() > m ? phiS * (y.get(y.size() - 1 - m)) : 0);
            var res = Calc.resultado("Phi", c.fmt(phiS), "phi", c.fmt(phi), "pronostico", c.fmt(pron));
            return ResultadoMotor.of("SARIMA estacional", res,
                    c.seriePronostico(y, List.of(pron)),
                    "El coeficiente estacional Phi=" + c.t(phiS) + " captura el ciclo de largo " + m
                            + "; el pronóstico de un paso es " + c.t(pron) + " unidades.");
        });
    }

    @Bean
    MotorMatematico motorArimax() {
        return new MotorGenerico("ARIMAX", true, (p, c) -> {
            int pOrd = c.entero("p", 1);
            int qOrd = c.entero("q", 1);
            List<Double> y = serie(c);
            List<Double> x = c.lista("x", List.of(1d));
            double beta = c.promedio(x);
            double phi = 0;
            for (int i = 1; i < y.size(); i++) {
                phi += (y.get(i - 1) - c.promedio(y)) * (y.get(i) - c.promedio(y));
            }
            double m2 = 0;
            for (int i = 0; i < y.size() - 1; i++) {
                m2 += Math.pow(y.get(i) - c.promedio(y), 2);
            }
            phi = m2 > 0 ? phi / m2 : 0;
            double resid = 0;
            List<Double> errores = new ArrayList<>();
            for (int i = 1; i < y.size(); i++) {
                resid = y.get(i) - (phi * y.get(i - 1) + beta * (i < x.size() ? x.get(i) : x.get(x.size() - 1)));
                errores.add(resid);
            }
            double theta = qOrd >= 1 ? (errores.isEmpty() ? 0 : c.promedio(errores)) : 0;
            double pron = phi * y.get(y.size() - 1) + beta * (x.isEmpty() ? 1 : x.get(x.size() - 1)) + theta;
            var res = Calc.resultado("beta_j", List.of(c.fmt(beta)), "phi", c.fmt(phi),
                    "pronostico", c.fmt(pron));
            return ResultadoMotor.of("ARIMAX con exógenas", res,
                    c.seriePronostico(y, List.of(pron)),
                    "El coeficiente de la variable exógena (beta=" + c.t(beta)
                            + ") modifica la proyección ARMA: pronóstico de " + c.t(pron) + " unidades.");
        });
    }

    @Bean
    MotorMatematico motorTrackingSignal() {
        return new MotorGenerico("TRACKING_SIGNAL", true, (p, c) -> {
            int u = c.entero("U", 4);
            List<Double> y = serie(c);
            double alpha = c.num("alpha", 0.3);
            double nivel = y.get(0);
            double rsfe = 0;
            double mad = 0;
            List<Double> tss = new ArrayList<>();
            for (int i = 1; i < y.size(); i++) {
                double err = y.get(i) - nivel;
                rsfe += err;
                mad = (mad * (i - 1) + Math.abs(err)) / i;
                tss.add(mad > 0 ? rsfe / mad : 0);
                nivel = alpha * y.get(i) + (1 - alpha) * nivel;
            }
            double ts = tss.isEmpty() ? 0 : tss.get(tss.size() - 1);
            String estado = Math.abs(ts) >= u ? "fuera de control (revisar modelo)" : "dentro de control";
            var res = Calc.resultado("RSFE", c.fmt(rsfe), "MAD", c.fmt(mad), "TS", c.fmt(ts),
                    "limite", u, "estado", estado);
            List<Map<String, Object>> serie = new ArrayList<>();
            for (int i = 0; i < tss.size(); i++) {
                serie.add(Calc.resultado("t", i + 2, "TS", c.fmt(tss.get(i))));
            }
            return ResultadoMotor.of("Tracking Signal", res, serie,
                    "La señal de seguimiento es " + c.t(ts) + " frente al límite U=" + u
                            + ": el modelo está " + estado + ".");
        });
    }

    @Bean
    MotorMatematico motorLiftPromo() {
        return new MotorGenerico("LIFT_PROMOCION", true, (p, c) -> {
            double lf = c.num("LF", 1.4);
            List<Double> y = serie(c);
            int n = c.entero("N", 7);
            double base = 0;
            for (int i = Math.max(0, y.size() - n); i < y.size(); i++) {
                base += y.get(i);
            }
            base /= Math.min(n, y.size());
            double pron = base * lf;
            var res = Calc.resultado("demandaBase", c.fmt(base), "lift", lf, "pronostico", c.fmt(pron));
            List<Double> pred = new ArrayList<>();
            for (int j = 0; j < 3; j++) {
                pred.add(base * lf);
            }
            return ResultadoMotor.of("Forecast con lift", res,
                    c.seriePronostico(y, pred),
                    "Sobre una demanda base de " + c.t(base) + " unidades, un lift de " + c.t(lf)
                            + " proyecta " + c.t(pron) + " unidades por periodo durante la promoción.");
        });
    }

    @Bean
    MotorMatematico motorRegresionCalendario() {
        return new MotorGenerico("REGRESION_CALENDARIO", true, (p, c) -> {
            List<Double> y = serie(c);
            int m = 7;
            double[] acum = new double[m];
            int[] nDia = new int[m];
            for (int i = 0; i < y.size(); i++) {
                acum[i % m] += y.get(i);
                nDia[i % m]++;
            }
            double prom = c.promedio(y);
            double base = 0;
            for (int d = 0; d < m; d++) {
                base += acum[d] / nDia[d];
            }
            base /= m;
            double significa = 0;
            double residuales = 0;
            for (int i = 0; i < y.size(); i++) {
                double pred = base * (acum[i % m] / nDia[i % m]) / base;
                significa += Math.pow(pred - prom, 2);
                residuales += Math.pow(y.get(i) - pred, 2);
            }
            double r2 = residuales + significa > 0 ? significa / (significa + residuales) : 0;
            List<Double> beta = new ArrayList<>();
            for (int d = 0; d < m; d++) {
                beta.add(c.fmt((acum[d] / nDia[d]) / base - 1));
            }
            double pron = base * (1 + beta.get((y.size() + 1) % m));
            var res = Calc.resultado("beta_j", beta, "R2", c.fmt(r2), "pronostico", c.fmt(pron));
            return ResultadoMotor.of("Regresión con exógenas de calendario", res,
                    c.seriePronostico(y, List.of(pron)),
                    "Los coeficientes día-de-semana son " + formatArr(beta, c) + " con R²=" + c.t(r2)
                            + "; el pronóstico del siguiente periodo es " + c.t(pron) + " unidades.");
        });
    }

    @Bean
    MotorMatematico motorRegresionPromo() {
        return new MotorGenerico("REGRESION_PROMO", true, (p, c) -> {
            List<Double> y = serie(c);
            List<Double> promo = c.lista("promo", promoPatron(y.size()));
            double mediaPromo = c.promedio(promo);
            double denom = 0;
            double num = 0;
            for (int i = 0; i < y.size(); i++) {
                denom += Math.pow(promo.get(i) - mediaPromo, 2);
                num += (promo.get(i) - mediaPromo) * (y.get(i) - c.promedio(y));
            }
            double b = denom > 0 ? num / denom : 0;
            double d0 = c.promedio(y) - b * mediaPromo;
            double lift = b / (d0 > 0 ? d0 : 1e-6);
            var res = Calc.resultado("L_j", c.fmt(lift), "D0", c.fmt(d0));
            List<Map<String, Object>> serie = new ArrayList<>();
            for (int i = 0; i < y.size(); i++) {
                serie.add(Calc.resultado("periodo", i + 1, "demanda", c.fmt(y.get(i)),
                        "promo", promo.get(i)));
            }
            return ResultadoMotor.of("Regresión con variables de promoción", res, serie,
                    "La demanda base es " + c.t(d0) + " y el lift promocional estimado es "
                            + c.t(lift) + " por unidad de actividad promocional.");
        });
    }

    @Bean
    MotorMatematico motorRegresionIngreso() {
        return new MotorGenerico("REGRESION_INGRESO", true, (p, c) -> {
            List<Double> y = serie(c);
            List<Double> x = c.lista("x_ingreso", serieIngresos(y.size()));
            double ly = c.promedio(y);
            double lx = c.promedio(x);
            double num = 0;
            double den = 0;
            for (int i = 0; i < y.size(); i++) {
                double dy = Math.log(Math.max(1e-6, y.get(i))) - Math.log(Math.max(1e-6, ly));
                double dx = Math.log(Math.max(1e-6, x.get(i))) - Math.log(Math.max(1e-6, lx));
                num += dx * dy;
                den += dx * dx;
            }
            double eta = den > 0 ? num / den : 0;
            var res = Calc.resultado("eta_y", c.fmt(eta));
            List<Map<String, Object>> serie = new ArrayList<>();
            for (int i = 0; i < y.size(); i++) {
                serie.add(Calc.resultado("periodo", i + 1, "demanda", c.fmt(y.get(i)),
                        "ingreso", c.fmt(x.get(i))));
            }
            return ResultadoMotor.of("Efecto de ingreso y renta", res, serie,
                    "La elasticidad ingreso de la demanda es " + c.t(eta)
                            + ": una subida del 1% del ingreso mueve la demanda un " + c.t(eta * 100) + "%.");
        });
    }

    // ------------------------------------------------------------- helpers

    private List<Double> pesosLineales(int n) {
        List<Double> w = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            w.add((double) i);
        }
        return w;
    }

    private List<Double> estacionalInicial(List<Double> y, int m, int modo) {
        List<Double> ind = new ArrayList<>();
        for (int k = 0; k < m; k++) {
            List<Double> vals = new ArrayList<>();
            for (int i = k; i < y.size(); i += m) {
                vals.add(y.get(i));
            }
            double s = 0;
            for (double v : vals) {
                s += v;
            }
            double base = c0(vals.isEmpty() ? 1 : s / vals.size(), m, modo);
            ind.add(base);
        }
        double tot = 0;
        for (double v : ind) {
            tot += v;
        }
        if (modo == 1) {
            double media = tot / m;
            for (int k = 0; k < m; k++) {
                ind.set(k, media > 0 ? ind.get(k) / media : 1);
            }
        } else {
            double media = tot / m;
            for (int k = 0; k < m; k++) {
                ind.set(k, ind.get(k) - media);
            }
        }
        double max = 0;
        for (double v : ind) {
            if (Math.abs(v) > max) {
                max = Math.abs(v);
            }
        }
        if (max == 0) {
            for (int k = 0; k < m; k++) {
                ind.set(k, modo == 1 ? 1.0 : 0.0);
            }
        }
        return ind;
    }

    private double c0(double v, int m, int modo) {
        return v;
    }

    private List<Double> tendenciaCentrada(List<Double> y, int m) {
        List<Double> t = new ArrayList<>();
        for (int i = 0; i < y.size(); i++) {
            t.add(Double.NaN);
        }
        boolean par = m % 2 == 0;
        int v = par ? (m / 2 - 1) : (m / 2);
        double[] w = new double[m];
        for (int j = 0; j < m; j++) {
            w[j] = par ? (j == 0 || j == m - 1 ? 0.5 : 1.0) : 1.0;
        }
        for (int i = v; i < y.size() - v; i++) {
            double s = 0;
            double ws = 0;
            for (int j = -v; j <= v; j++) {
                int idx = i + j;
                double ww = par ? (j == -v || j == v ? 0.5 : 1.0) : 1.0;
                s += ww * y.get(idx);
                ws += ww;
            }
            t.set(i, s / ws);
        }
        return t;
    }

    private double mediana(List<Double> xs) {
        List<Double> s = new ArrayList<>(xs);
        java.util.Collections.sort(s);
        int n = s.size();
        if (n == 0) {
            return 0;
        }
        return n % 2 == 1 ? s.get(n / 2) : (s.get(n / 2 - 1) + s.get(n / 2)) / 2;
    }

    private double medianaAbs(List<Double> xs, double centro) {
        List<Double> d = new ArrayList<>();
        for (double x : xs) {
            d.add(Math.abs(x - centro));
        }
        return mediana(d);
    }

    private List<Double> promoPatron(int n) {
        List<Double> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            out.add(i % 10 == 3 || i % 10 == 4 ? 1.0 : 0.0);
        }
        return out;
    }

    private List<Double> serieIngresos(int n) {
        List<Double> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            out.add(1.0 + 0.01 * i);
        }
        return out;
    }

    private String formatArr(List<Double> xs, Calc c) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < Math.min(xs.size(), 7); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(c.t(xs.get(i)));
        }
        if (xs.size() > 7) {
            sb.append(", …");
        }
        sb.append("]");
        return sb.toString();
    }
}