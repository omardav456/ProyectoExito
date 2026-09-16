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

/** Familia de motores de CLASIFICACION DE INVENTARIO. */
@Configuration
public class MotoresClasificacion {

    @Bean
    MotorMatematico clasLorenz() {
        return new MotorGenerico("LORENZ", (p, c) -> {
            List<Double> valor = c.lista("valor", List.of(50000000d, 30000000d, 18000000d, 9000000d, 4000000d, 2500000d, 1000000d));
            List<Double> ordenado = new ArrayList<>(valor);
            ordenado.sort(java.util.Comparator.reverseOrder());
            double total = 0;
            for (double v : ordenado) {
                total += v;
            }
            double acum = 0;
            double gini = 0;
            int n = ordenado.size();
            double prevLorenz = 0;
            List<Map<String, Object>> serie = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                acum += ordenado.get(i);
                double lorenz = acum / total;
                double share = (i + 1.0) / n;
                gini += share - lorenz;
                serie.add(Calc.resultado("cuantil", c.fmt(share), "Lorenz", c.fmt(lorenz)));
                prevLorenz = lorenz;
            }
            double g = n > 0 ? gini / n * 2 : 0;
            var res = Calc.resultado("G", c.fmt(g), "concentracionTop20", c.fmt(0.0 + (ordenado.get(0) + (n > 1 ? ordenado.get(1) : 0)) / total));
            return ResultadoMotor.of("Curva de Lorenz de inventario", res, serie,
                    "El Gini del inventario es G=" + c.t(g) + "; el top de la cartera concentra "
                            + c.t(100 * (ordenado.get(0) + (n > 1 ? ordenado.get(1) : 0)) / total) + "% del valor.");
        });
    }

    @Bean
    MotorMatematico clasXyz() {
        return new MotorGenerico("XYZ", (p, c) -> {
            List<Double> cvs = c.lista("CV", List.of(0.20d, 0.42d, 0.85d, 0.30d, 0.65d, 0.15d, 1.30d));
            double cX = c.num("cX", 0.5);
            double cY = c.num("cY", 1.0);
            List<String> clases = new ArrayList<>();
            String ejemplo = "";
            for (double cv : cvs) {
                String cl = cv <= cX ? "X" : (cv <= cY ? "Y" : "Z");
                clases.add(cl);
            }
            List<Map<String, Object>> serie = new ArrayList<>();
            for (int i = 0; i < cvs.size(); i++) {
                serie.add(Calc.resultado("sku", "S" + (i + 1), "CV", c.fmt(cvs.get(i)), "clase", clases.get(i)));
            }
            var res = Calc.resultado("clase", clases, "clase_ejemplo", clases.get(0));
            return ResultadoMotor.of("Clasificación X/Y/Z", res, serie,
                    "Con umbrales cX=" + c.t(cX) + " y cY=" + c.t(cY) + ", la demanda estable (X) "
                            + "usa pronóstico SES/ARIMA y la volátil (Y/Z) políticas con colchón.");
        });
    }

    @Bean
    MotorMatematico clasAbcXyz() {
        return new MotorGenerico("ABC_XYZ", (p, c) -> {
            List<Double> valor = c.lista("valor", List.of(0.60d, 0.23d, 0.10d, 0.05d, 0.02d, 0.0d));
            List<Double> cv = c.lista("CV", List.of(0.2d, 0.9d, 0.4d, 1.2d, 0.7d, 0.3d));
            List<String> celdas = new ArrayList<>();
            for (int i = 0; i < valor.size(); i++) {
                String aB = valor.get(i) <= 0.65 ? "A" : (valor.get(i) <= 0.90 ? "B" : "C");
                String xyz = cv.get(i) <= 0.5 ? "X" : (cv.get(i) <= 1.0 ? "Y" : "Z");
                celdas.add(aB + xyz);
            }
            List<Map<String, Object>> serie = new ArrayList<>();
            for (int i = 0; i < celdas.size(); i++) {
                serie.add(Calc.resultado("celda", i + 1, "celda", celdas.get(i)));
            }
            var res = Calc.resultado("celda", celdas, "politica", "AX: JIT", "politica2", "BZ: doble fuente");
            return ResultadoMotor.of("Matriz ABC-XYZ integrada", res, serie,
                    "Cada celda dicta la política: AX/JIT, AY/pronóstico fino, BZ/colchón y doble fuente, "
                            + "CZ/obsolescencia controlada.");
        });
    }

    @Bean
    MotorMatematico clasConteoCiclico() {
        return new MotorGenerico("CONTEO_CICLICO", (p, c) -> {
            double epsilon = c.num("epsilon", 0.02);
            double h = c.num("H", 200);
            List<Double> rotacion = c.lista("rotacion", List.of(10d, 6d, 3d, 1d, 0.5d));
            List<Double> frec = new ArrayList<>();
            double sumaF = 0;
            for (double r : rotacion) {
                double f = Math.max(1, Math.ceil(h / (r > 0 ? r : 1)));
                frec.add(c.fmt(f));
                sumaF += f;
            }
            List<Map<String, Object>> serie = new ArrayList<>();
            for (int i = 0; i < rotacion.size(); i++) {
                serie.add(Calc.resultado("item", "I" + (i + 1), "f_i", frec.get(i), "rotacion", c.fmt(rotacion.get(i))));
            }
            var res = Calc.resultado("f_i", frec, "totalConteos", c.fmt(sumaF), "eps_val", c.fmt(epsilon));
            return ResultadoMotor.of("Conteo cíclico con clasificación", res, serie,
                    "Los ítems de mayor rotación se cuentan cada " + c.t(h / Math.max(1, frec.size() * 1.0))
                            + " días hábiles (f_i); tolerancia de error ε=" + c.t(epsilon * 100) + "%.");
        });
    }

    @Bean
    MotorMatematico clasMerma() {
        return new MotorGenerico("MERMA", (p, c) -> {
            double bu = c.num("BU", 5);
            double t = c.num("T", 30);
            double rho = bu > 0 ? 1.0 / bu : 0;
            double dDiaria = c.num("d_diaria", 40);
            double precio = c.num("precio", 8500);
            double eW = dDiaria * t * (rho * t) / 2;
            double cm = eW * precio;
            var res = Calc.resultado("E[W]", c.fmt(eW), "CM", c.fmt(cm), "rho", c.fmt(rho));
            List<Map<String, Object>> serie = new ArrayList<>();
            for (double b : new double[]{3, 5, 7, 10}) {
                double r = 1 / b;
                serie.add(Calc.resultado("BU", c.fmt(b), "E[W]", c.fmt(dDiaria * t * r * t / 2)));
            }
            return ResultadoMotor.of("Merma por caducidad", res, serie,
                    "Con vida útil BU=" + c.t(bu) + " días, la merma esperada es " + c.t(eW)
                            + " unidades (" + c.t(cm) + " COP) en " + c.t(t) + " días.");
        });
    }
}