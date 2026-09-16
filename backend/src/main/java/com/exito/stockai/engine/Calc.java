package com.exito.stockai.engine;

import com.exito.stockai.exception.BadRequestException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * Utilidades de cálculo para los motores genéricos del catálogo.
 * Lee parámetros por símbolo (con búsqueda insensible a mayúsculas),
 * construye series y resultados, y provee estadísticos comunes.
 */
public final class Calc {

    private final Map<String, Object> p;

    public Calc(Map<String, Object> p) {
        this.p = p == null ? Map.of() : p;
    }

    public Object raw(String k) {
        if (p.containsKey(k)) {
            return p.get(k);
        }
        for (Map.Entry<String, Object> e : p.entrySet()) {
            if (e.getKey().equalsIgnoreCase(k)) {
                return e.getValue();
            }
        }
        return null;
    }

    public boolean tiene(String k) {
        return raw(k) != null;
    }

    public double num(String k, double def) {
        Object v = raw(k);
        if (v == null) {
            return def;
        }
        if (v instanceof Number n) {
            return n.doubleValue();
        }
        String s = v.toString().trim();
        if (s.isEmpty()) {
            return def;
        }
        try {
            return Double.parseDouble(s.replace(",", "."));
        } catch (NumberFormatException e) {
            throw new BadRequestException("El parámetro '" + k + "' debe ser numérico (recibido: " + v + ")");
        }
    }

    public double num(String k) {
        Object v = raw(k);
        if (v == null) {
            throw new BadRequestException("El parámetro obligatorio '" + k + "' no fue suministrado");
        }
        return num(k, 0);
    }

    public int entero(String k, int def) {
        return (int) Math.round(num(k, def));
    }

    public int entero(String k) {
        return (int) Math.round(num(k));
    }

    public boolean bool(String k, boolean def) {
        Object v = raw(k);
        if (v == null) {
            return def;
        }
        if (v instanceof Boolean b) {
            return b;
        }
        return Boolean.parseBoolean(v.toString());
    }

    public List<Double> lista(String k) {
        Object v = raw(k);
        if (v instanceof List<?> l) {
            List<Double> out = new ArrayList<>();
            for (Object o : l) {
                out.add(((Number) o).doubleValue());
            }
            return out;
        }
        if (v instanceof String s && !s.isBlank()) {
            return csv(s);
        }
        throw new BadRequestException("El parámetro '" + k + "' debe ser una lista o CSV de números");
    }

    public List<Double> lista(String k, List<Double> def) {
        return raw(k) == null ? def : lista(k);
    }

    public double[] arr(String k, double[] def) {
        return lista(k, aList(def)).stream().mapToDouble(Double::doubleValue).toArray();
    }

    public List<Double> csv(String s) {
        List<Double> out = new ArrayList<>();
        for (String token : s.split("[,;\\s]+")) {
            if (token.isBlank()) {
                continue;
            }
            out.add(Double.parseDouble(token.replace(",", ".")));
        }
        return out;
    }

    public List<Double> serieSimulada(int n) {
        List<Double> s = new ArrayList<>();
        Random rnd = new Random(42);
        for (int i = 0; i < n; i++) {
            double v = 40 + i * 0.5 + 6 * Math.sin(2 * Math.PI * i / 7)
                    + (rnd.nextDouble() - 0.5) * 6;
            s.add(Math.max(0, v));
        }
        return s;
    }

    /** Serie simulada de demanda: tendencia + estacionalidad semanal. */
    public List<Double> serieDemanda(int n) {
        List<Double> s = new ArrayList<>();
        Random rnd = new Random(7);
        for (int i = 0; i < n; i++) {
            double v = 30 + i * 0.8 + 5 * Math.sin(2 * Math.PI * i / 7)
                    + (rnd.nextDouble() - 0.5) * 4;
            s.add(Math.max(0, v));
        }
        return s;
    }

    public double promedio(List<Double> xs) {
        if (xs == null || xs.isEmpty()) {
            return 0;
        }
        double s = 0;
        for (double x : xs) {
            s += x;
        }
        return s / xs.size();
    }

    public double desviacion(List<Double> xs) {
        if (xs == null || xs.size() < 2) {
            return 0;
        }
        double m = promedio(xs);
        double s = 0;
        for (double x : xs) {
            s += (x - m) * (x - m);
        }
        return Math.sqrt(s / (xs.size() - 1));
    }

    public double fmt(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    public String t(double v) {
        return String.format(Locale.ROOT, "%.2f", v);
    }

    public void validar(boolean ok, String mensaje) {
        if (!ok) {
            throw new BadRequestException(mensaje);
        }
    }

    /** CDF acumulada normal estándar P(Z <= z). */
    public double phi(double z) {
        return 0.5 * (1 + erf(z / Math.sqrt(2)));
    }

    /** Cuantil normal estándar (inversa de la CDF) por aproximación de Acklam. */
    public double qnorm(double p) {
        if (p <= 0.000000001) {
            return -5.0;
        }
        if (p >= 0.999999999) {
            return 5.0;
        }
        double[] a = {-3.969683028665376e+01, 2.209460984245205e+02, -2.759285104469687e+02,
                      1.383577518672690e+02, -3.066479806614716e+01, 2.506628277459239e+00};
        double[] b = {-5.447609879822406e+01, 1.615858368580409e+02, -1.556989798598866e+02,
                      6.680131188771972e+01, -1.328068155288572e+01};
        double[] c = {-7.784894002430293e-03, -3.223964580411365e-01, -2.400758277161838e+00,
                      -2.549732539343734e+00, 4.374664141464968e+00, 2.938163982698783e+00};
        double[] d = {7.784695709041462e-03, 3.224671290700398e-01, 2.445134137142996e+00,
                      3.754408661907416e+00};
        double plow = 0.02425;
        double phigh = 1 - plow;
        double q;
        if (p < plow) {
            q = Math.sqrt(-2 * Math.log(p));
            double x = ((c[0] * q + c[1]) * q + c[2]) * q + c[3];
            double y = ((d[0] * q + d[1]) * q + d[2]) * q + d[3];
            q = x / y;
        } else if (p <= phigh) {
            double x = p - 0.5;
            double r = x * x;
            q = x * (((a[0] * r + a[1]) * r + a[2]) * r + a[3]) * r + a[4];
            q /= (((b[0] * r + b[1]) * r + b[2]) * r + b[3]) * r + 1;
        } else {
            q = Math.sqrt(-2 * Math.log(1 - p));
            double x = ((c[0] * q + c[1]) * q + c[2]) * q + c[3];
            double y = ((d[0] * q + d[1]) * q + d[2]) * q + d[3];
            q = -(x / y);
        }
        // Refinamiento de una iteración de Halley.
        double e = 0.5 * erf(-q / Math.sqrt(2)) - p + 0.5;
        e = e * Math.sqrt(2 * Math.PI) * Math.exp(q * q / 2);
        q -= e / (1 + q * e / 2);
        return q;
    }

    private static double erf(double x) {
        double t = 1.0 / (1.0 + 0.3275911 * Math.abs(x));
        double y = 1.0 - (((((1.061405429 * t - 1.453152027) * t) + 1.421413741) * t - 0.284496736) * t + 0.254829592) * t
                * Math.exp(-x * x);
        return Math.signum(x) * y;
    }

    public static List<Double> aList(double[] v) {
        List<Double> out = new ArrayList<>(v.length);
        for (double x : v) {
            out.add(x);
        }
        return out;
    }

    // -------------------------------------------------------------
    // Constructores de resultados y series
    // -------------------------------------------------------------

    public static Map<String, Object> resultado(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put(String.valueOf(kv[i]), kv[i + 1]);
        }
        return m;
    }

    public static List<Map<String, Object>> filas(Object... columnas) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (int i = 0; i < columnas.length; i += 3) {
            Map<String, Object> f = new LinkedHashMap<>();
            f.put("t", columnas[i]);
            f.put(String.valueOf(columnas[i + 1]), columnas[i + 2]);
            out.add(f);
        }
        return out;
    }

    /** Serie para pronóstico: histórico (t=1..n) + pronóstico (t=n+1..n+h). */
    public List<Map<String, Object>> seriePronostico(List<Double> hist, List<Double> pred) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (int i = 0; i < hist.size(); i++) {
            out.add(resultado("periodo", i + 1, "historico", fmt(hist.get(i))));
        }
        for (int j = 0; j < pred.size(); j++) {
            out.add(resultado("periodo", hist.size() + j + 1, "pronostico", fmt(pred.get(j))));
        }
        return out;
    }

    /** Serie de pares (x, y) para un eje numérico. */
    public List<Map<String, Object>> serieXY(String xKey, List<Double> xs, String yKey, List<Double> ys) {
        List<Map<String, Object>> out = new ArrayList<>();
        int n = Math.min(xs.size(), ys.size());
        for (int i = 0; i < n; i++) {
            out.add(resultado(xKey, fmt(xs.get(i)), yKey, fmt(ys.get(i))));
        }
        return out;
    }
}