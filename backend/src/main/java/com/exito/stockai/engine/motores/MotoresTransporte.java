package com.exito.stockai.engine.motores;

import com.exito.stockai.engine.Calc;
import com.exito.stockai.engine.MotorGenerico;
import com.exito.stockai.engine.MotorMatematico;
import com.exito.stockai.engine.ResultadoMotor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Familia de motores de TRANSPORTE Y DISTRIBUCION. */
@Configuration
public class MotoresTransporte {

    @Bean
    MotorMatematico transpVrp() {
        return new MotorGenerico("VRP", (p, c) -> {
            double Q = c.num("Q", 20000);
            List<Double> coordsX = c.lista("coords_x", List.of(100d, 220d, 150d, 260d, 340d, 90d));
            List<Double> coordsY = c.lista("coords_y", List.of(100d, 140d, 260d, 180d, 240d, 60d));
            List<Double> demandas = c.lista("demandas", List.of(0d, 5000d, 8000d, 4000d, 6000d, 3000d));
            double costoKm = c.num("costo_km", 2500);
            int n = Math.min(coordsX.size(), coordsY.size());
            double[] xs = new double[n];
            double[] ys = new double[n];
            for (int i = 0; i < n; i++) {
                xs[i] = coordsX.get(i);
                ys[i] = coordsY.get(i);
            }
            List<Double> rutas = new ArrayList<>();
            double costo = 0;
            for (int i = 1; i < n; i++) {
                costo += Math.hypot(xs[i] - xs[i - 1], ys[i] - ys[i - 1]);
            }
            costo += Math.hypot(xs[n - 1] - xs[0], ys[n - 1] - ys[0]);
            List<List<String>> rutasVec = new ArrayList<>();
            List<String> ruta = new ArrayList<>();
            double acc = 0;
            for (int i = 1; i < n; i++) {
                ruta.add("C" + i);
                acc += i < demandas.size() ? demandas.get(i) : 0;
                if (acc + (i + 1 < demandas.size() ? demandas.get(i + 1) : 0) > Q) {
                    rutasVec.add(new ArrayList<>(ruta));
                    rutas.clear();
                    ruta = new ArrayList<>();
                    acc = 0;
                }
            }
            if (!ruta.isEmpty()) {
                rutasVec.add(ruta);
            }
            int k = Math.max(1, rutasVec.size());
            double cTotal = costo * costoKm + k * 100000;
            List<Map<String, Object>> serie = new ArrayList<>();
            for (int i = 0; i < Math.min(rutasVec.size(), 5); i++) {
                serie.add(Calc.resultado("ruta", i + 1, "paradas", rutasVec.get(i).size(), "carga", c.fmt(acc)));
            }
            var res = Calc.resultado("K", (double) k, "C", c.fmt(cTotal), "R_k", rutas);
            return ResultadoMotor.of("VRP flota homogénea", res, serie,
                    "Con capacidad Q=" + c.t(Q) + ", se requieren K=" + k + " vehículos y el costo "
                            + "de transporte es " + c.t(cTotal) + " COP.");
        });
    }

    @Bean
    MotorMatematico transpCvrp() {
        return new MotorGenerico("CVRP", (p, c) -> {
            double Q = c.num("Q", 20000);
            List<Double> demandas = c.lista("demandas", List.of(0d, 5200d, 8100d, 4800d, 6200d, 3900d, 7400d));
            List<String> rutas = new ArrayList<>();
            List<Double> cargas = new ArrayList<>();
            double acc = 0;
            for (int i = 1; i < demandas.size(); i++) {
                double d = demandas.get(i);
                if (acc + d > Q) {
                    cargas.add(c.fmt(acc));
                    acc = 0;
                }
                rutas.add("C" + i + "->");
                acc += d;
            }
            cargas.add(c.fmt(acc));
            double ol = 0;
            for (double d : demandas) {
                if (d > Q) {
                    ol += d - Q;
                }
            }
            List<Map<String, Object>> serie = new ArrayList<>();
            for (int i = 0; i < cargas.size(); i++) {
                serie.add(Calc.resultado("ruta", i + 1, "carga", cargas.get(i)));
            }
            var res = Calc.resultado("q_k", cargas, "OL", c.fmt(ol), "K", (double) cargas.size());
            return ResultadoMotor.of("CVRP capacitado", res, serie,
                    "Se dividen " + (demandas.size() - 1) + " clientes en " + cargas.size()
                            + " rutas sin sobrecarga (OL=" + c.t(ol) + ") con capacidad Q=" + c.t(Q) + ".");
        });
    }

    @Bean
    MotorMatematico transpVrptw() {
        return new MotorGenerico("VRPTW", (p, c) -> {
            double s = c.num("s", 30);
            List<Double> tiempos = c.lista("tiempos", List.of(0d, 25d, 18d, 40d, 15d, 30d, 22d));
            List<Double> ventanas = c.lista("ventanas", List.of(0d, 60d, 55d, 70d, 50d, 80d, 65d));
            List<Double> llegada = new ArrayList<>();
            List<Double> espera = new ArrayList<>();
            double t = 0;
            for (int i = 0; i < tiempos.size(); i++) {
                double arribo = t + tiempos.get(i);
                double w = Math.max(0, ventanas.get(i) - arribo);
                llegada.add(c.fmt(arribo + s));
                espera.add(c.fmt(w));
                t = arribo + s + w;
            }
            List<Map<String, Object>> serie = new ArrayList<>();
            for (int i = 0; i < tiempos.size(); i++) {
                serie.add(Calc.resultado("cliente", i, "t_i", llegada.get(i), "w_i", espera.get(i)));
            }
            var res = Calc.resultado("t_i", llegada, "w_i", espera, "makespan", c.fmt(t));
            return ResultadoMotor.of("VRPTW ventanas de tiempo", res, serie,
                    "Con servicio s=" + c.t(s) + " min, la secuencia respeta las ventanas acumulando "
                            + "espera total de " + c.t(t) + " minutos (makespan).");
        });
    }

    @Bean
    MotorMatematico transpTsp() {
        return new MotorGenerico("TSP", (p, c) -> {
            List<Double> xs = c.lista("x", List.of(0d, 120d, 80d, 260d, 210d, 45d, 300d));
            List<Double> ys = c.lista("y", List.of(0d, 45d, 190d, 60d, 260d, 95d, 220d));
            int n = Math.min(xs.size(), ys.size());
            double[][] dMat = new double[n][n];
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    dMat[i][j] = Math.hypot(xs.get(i) - xs.get(j), ys.get(i) - ys.get(j));
                }
            }
            int[] tour = new int[n];
            boolean[] visited = new boolean[n];
            for (int i = 0; i < n; i++) {
                visited[i] = false;
            }
            int cur = 0;
            tour[0] = 0;
            visited[0] = true;
            for (int step = 1; step < n; step++) {
                int best = -1;
                double bd = Double.MAX_VALUE;
                for (int j = 1; j < n; j++) {
                    if (!visited[j] && dMat[cur][j] < bd) {
                        bd = dMat[cur][j];
                        best = j;
                    }
                }
                tour[step] = best;
                visited[best] = true;
                cur = best;
            }
            double d = 0;
            for (int i = 0; i < n; i++) {
                d += dMat[tour[i]][tour[(i + 1) % n]];
            }
            List<Double> seq = new ArrayList<>();
            List<Map<String, Object>> serie = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                seq.add((double) tour[i]);
                serie.add(Calc.resultado("pos", i + 1, "nodo", tour[i]));
            }
            var res = Calc.resultado("D", c.fmt(d), "pi", seq);
            return ResultadoMotor.of("TSP del viajante", res, serie,
                    "La heurística del vecino más cercano arma el circuito en " + c.t(d)
                            + " unidades de distancia sobre " + n + " paradas.");
        });
    }

    @Bean
    MotorMatematico transpTspPicking() {
        return new MotorGenerico("TSP_PICKING", (p, c) -> {
            List<Double> pos = c.lista("posiciones", List.of(2d, 5d, 1d, 8d, 4d, 6d, 3d));
            double aisle = c.num("modulo", 10);
            List<Double> sorted = new ArrayList<>(pos);
            java.util.Collections.sort(sorted);
            double d = 0;
            double cur = 0;
            List<Double> sec = new ArrayList<>();
            for (double x : sorted) {
                d += x - cur;
                cur = x;
                sec.add(x);
            }
            List<Map<String, Object>> serie = new ArrayList<>();
            for (int i = 0; i < sec.size(); i++) {
                serie.add(Calc.resultado("paso", i + 1, "ubicacion", sec.get(i)));
            }
            var res = Calc.resultado("D", c.fmt(d), "sec", sec);
            return ResultadoMotor.of("TSP picking", res, serie,
                    "Secuencia de recogida en S de " + c.t(d) + " metros (pasillo de " + c.t(aisle) + " m).");
        });
    }

    @Bean
    MotorMatematico transpPMedianaAlm() {
        return new MotorGenerico("P_MEDIANA_ALM", (p, c) -> {
            int pj = c.entero("p", 3);
            List<Double> demandas = c.lista("h_i", List.of(800d, 1200d, 600d, 1500d, 900d));
            List<Double> cotas = c.lista("dist", List.of(1.2d, 2.4d, 1.8d, 3.0d, 2.2d));
            List<String> ubic = new ArrayList<>();
            List<Integer> idx = new ArrayList<>();
            for (int i = 0; i < demandas.size(); i++) {
                idx.add(i);
            }
            idx.sort((a, b) -> Double.compare(cotas.get(a) + demandas.get(a), cotas.get(b) + demandas.get(b)));
            for (int j = 0; j < Math.min(pj, idx.size()); j++) {
                ubic.add("Y" + idx.get(j));
            }
            List<Map<String, Object>> serie = new ArrayList<>();
            for (int i = 0; i < idx.size(); i++) {
                serie.add(Calc.resultado("candidato", idx.get(i), "seleccion", i < pj ? "1" : "0"));
            }
            var res = Calc.resultado("y_j", ubic);
            return ResultadoMotor.of("P-mediana almacén", res, serie,
                    "Se abren p=" + pj + " mediaratas " + ubic + " en el almacén minimizando la "
                            + "distancia ponderada por demanda.");
        });
    }

    @Bean
    MotorMatematico transpPMedianaRed() {
        return new MotorGenerico("P_MEDIANA_RED", (p, c) -> {
            int pj = c.entero("p", 5);
            List<Double> demandas = c.lista("demanda", List.of(500d, 700d, 400d, 900d, 800d, 300d, 650d));
            List<String> sel = new ArrayList<>();
            List<Integer> idx = new ArrayList<>();
            for (int i = 0; i < demandas.size(); i++) {
                idx.add(i);
            }
            idx.sort((a, b) -> Double.compare(demandas.get(b), demandas.get(a)));
            for (int j = 0; j < Math.min(pj, idx.size()); j++) {
                sel.add("Y" + idx.get(j));
            }
            List<String> asig = new ArrayList<>();
            for (int i = 0; i < demandas.size(); i++) {
                String hub = "Y" + idx.get(Math.min(i, sel.size() - 1));
                asig.add("C" + i + ">" + hub);
            }
            List<Map<String, Object>> serie = new ArrayList<>();
            for (int i = 0; i < idx.size(); i++) {
                serie.add(Calc.resultado("nodo", "C" + i, "y_j", sel.contains("Y" + i) ? "1" : "0",
                        "x_ij", asig.get(i)));
            }
            var res = Calc.resultado("y_j", sel, "x_ij", asig);
            return ResultadoMotor.of("P-mediana de red", res, serie,
                    "Se eligen p=" + pj + " hubs " + sel + " y cada nodo se asigna al hub más cercano "
                            + "(asignaciones x_ij).");
        });
    }

    @Bean
    MotorMatematico transpClasico() {
        return new MotorGenerico("TRANSPORTE_CLASICO", (p, c) -> {
            double oferta = c.num("oferta", 1000);
            List<Double> demanda = c.lista("demanda", List.of(400d, 350d, 450d));
            List<Double> costoFil = c.lista("costo", List.of(120d, 150d, 100d));
            double deficit = oferta - demanda.stream().mapToDouble(Double::doubleValue).sum();
            List<Double> flujo = new ArrayList<>();
            double costo = 0;
            for (int j = 0; j < demanda.size(); j++) {
                double asignar = Math.min(oferta, demanda.get(j));
                flujo.add(c.fmt(asignar));
                costo += asignar * costoFil.get(j);
                oferta -= asignar;
            }
            List<Map<String, Object>> serie = new ArrayList<>();
            for (int j = 0; j < demanda.size(); j++) {
                serie.add(Calc.resultado("destino", "D" + (j + 1), "x_1j", flujo.get(j), "costo_u", costoFil.get(j)));
            }
            var res = Calc.resultado("C", c.fmt(costo), "x_ij", flujo, "deficit", c.fmt(deficit));
            return ResultadoMotor.of("Transporte lineal", res, serie,
                    "Se despacha a cada destino hasta cubrir su demanda con costo total de "
                            + c.t(costo) + " COP (excedente=" + c.t(deficit) + ").");
        });
    }

    @Bean
    MotorMatematico transpSetCovering() {
        return new MotorGenerico("SET_COVERING", (p, c) -> {
            double R = c.num("R", 50);
            List<Double> coordsX = c.lista("x", List.of(20d, 80d, 120d, 200d, 260d, 310d, 90d, 250d));
            List<Double> coordsY = c.lista("y", List.of(15d, 90d, 40d, 70d, 30d, 120d, 150d, 180d));
            int n = Math.min(coordsX.size(), coordsY.size());
            List<String> centros = new ArrayList<>();
            List<Double> cov = new ArrayList<>();
            boolean[] covered = new boolean[n];
            for (int i = 0; i < n; i++) {
                cov.add(c.fmt(0));
            }
            for (int i = 0; i < n; i++) {
                if (covered[i]) {
                    continue;
                }
                int mejor = i;
                int mejorCubiertos = 0;
                for (int k = 0; k < n; k++) {
                    if (covered[k]) {
                        continue;
                    }
                    int cu = 0;
                    for (int j = 0; j < n; j++) {
                        if (!covered[j] && Math.hypot(coordsX.get(j) - coordsX.get(k), coordsY.get(j) - coordsY.get(k)) <= R) {
                            cu++;
                        }
                    }
                    if (cu > mejorCubiertos) {
                        mejorCubiertos = cu;
                        mejor = k;
                    }
                }
                centros.add("Y" + mejor);
                for (int j = 0; j < n; j++) {
                    if (!covered[j] && Math.hypot(coordsX.get(j) - coordsX.get(mejor), coordsY.get(j) - coordsY.get(mejor)) <= R) {
                        covered[j] = true;
                    }
                }
            }
            int cub = 0;
            for (boolean b : covered) {
                if (b) {
                    cub++;
                }
            }
            List<Map<String, Object>> serie = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                serie.add(Calc.resultado("nodo", i, "y_j", centros.contains("Y" + i) ? "1" : "0"));
            }
            var res = Calc.resultado("Y", centros, "cov", c.fmt((double) cub / n));
            return ResultadoMotor.of("Location set covering", res, serie,
                    "Con radio R=" + c.t(R) + " km, se requieren " + centros.size() + " centros "
                            + centros + " cubriendo el " + c.t(100.0 * cub / n) + "% de los nodos.");
        });
    }

    @Bean
    MotorMatematico transpColdChain() {
        return new MotorGenerico("COLD_CHAIN", (p, c) -> {
            List<Double> tipo = c.lista("tipo", List.of(2d, 3d, 2d, 1d, 3d, 2d, 1d));
            double tAuto = c.num("t_auto", 2);
            List<String> match = new ArrayList<>();
            List<Map<String, Object>> serie = new ArrayList<>();
            for (int i = 0; i < tipo.size(); i++) {
                String m = Math.round(tipo.get(i)) == Math.round(tAuto) ? "1" : "0";
                match.add(m);
                serie.add(Calc.resultado("envio", i + 1, "temperatura", tipo.get(i), "match", m));
            }
            var res = Calc.resultado("match", match, "t_objetivo", c.fmt(tAuto));
            return ResultadoMotor.of("Cadena de frío selectiva", res, serie,
                    "Compatibilidad binaria con el vehículo refrigerado a " + c.t(tAuto)
                            + "°; sólo se embarcan los envíos tipo " + c.t(tAuto) + " (match=1).");
        });
    }

    @Bean
    MotorMatematico transpVmi() {
        return new MotorGenerico("VMI", (p, c) -> {
            double d = c.num("d_diaria", 120);
            double nivel = c.num("nivel", 0.75);
            double sRev = c.num("S_rev", 5);
            List<Double> visita = new ArrayList<>();
            double inv = nivel * sRev * d;
            double qv = 0;
            double acumPozo = 0;
            for (int t = 1; t <= 21; t++) {
                if (t % sRev == 0) {
                    visita.add(c.fmt(1));
                    double objetivo = sRev * d * 1.2;
                    qv = Math.max(0, objetivo - inv + acumPozo);
                    inv = objetivo;
                    acumPozo = 0;
                } else {
                    visita.add(c.fmt(0));
                }
                inv -= d;
                if (inv < 0) {
                    acumPozo += -inv;
                    inv = 0;
                }
            }
            List<Map<String, Object>> serie = new ArrayList<>();
            for (int t = 0; t < visita.size(); t++) {
                serie.add(Calc.resultado("dia", t + 1, "v_t", visita.get(t), "nivel", c.fmt(Math.max(0, inv))));
            }
            var res = Calc.resultado("q_v", c.fmt(qv), "v_t", visita);
            return ResultadoMotor.of("VMI proveedor-CEDI", res, serie,
                    "El proveedor visita cada " + c.t(sRev) + " días (v_t) y despacha q_v=" + c.t(qv)
                            + " unidades reponiendo al nivel objetivo sin quiebres.");
        });
    }

    @Bean
    MotorMatematico transpFleet() {
        return new MotorGenerico("FLEET", (p, c) -> {
            double viajes = c.num("viajes_dia", 14);
            double duracion = c.num("duracion_dia", 12);
            double tiempoViaje = c.num("tiempo_viaje", 6);
            double perdidas = Math.max(0, 1 - viajes * tiempoViaje / (duracion));
            double n = Math.ceil(viajes * tiempoViaje / duracion);
            double idle = perdidas;
            var res = Calc.resultado("N*", n, "E[idle]", c.fmt(idle), "viajes", c.fmt(viajes));
            return ResultadoMotor.of("Fleet sizing", res, List.of(),
                    "Con " + c.t(viajes) + " viajes de " + c.t(tiempoViaje) + " h en una ventana de "
                            + c.t(duracion) + " h, se requieren N*=" + c.t(n) + " vehículos con ocio "
                            + "esperado del " + c.t(idle * 100) + "%.");
        });
    }
}