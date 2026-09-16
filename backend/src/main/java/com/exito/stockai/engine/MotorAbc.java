package com.exito.stockai.engine;

import com.exito.stockai.exception.BadRequestException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Clasificación ABC de inventario por valor (costo × demanda anual).
 * A: ·80% | B: 80–95% | C: 95–100% del valor acumulado.
 */
@Component
public class MotorAbc implements MotorMatematico {

    @Override
    public String nombreMotor() {
        return "ABC";
    }

    @Override
    public ResultadoMotor ejecutar(Map<String, Object> p) {
        Object raw = p.get("items");
        if (!(raw instanceof List<?> items) || items.isEmpty()) {
            throw new BadRequestException("ABC: el parámetro 'items' (lista de {nombre, costo, demanda}) es obligatorio");
        }

        record Item(String nombre, double valor) {}
        List<Item> lista = new ArrayList<>();
        for (Object o : items) {
            if (!(o instanceof Map<?, ?> m)) {
                continue;
            }
            Object nombre = m.get("nombre");
            Object costo = m.get("costo");
            Object demanda = m.get("demanda");
            if (nombre == null || !(costo instanceof Number c) || !(demanda instanceof Number d)) {
                continue;
            }
            lista.add(new Item(String.valueOf(nombre), c.doubleValue() * d.doubleValue()));
        }
        if (lista.isEmpty()) {
            throw new BadRequestException("ABC: no hay ítems válidos con {nombre, costo, demanda}");
        }

        List<Item> ordenadas = lista.stream()
                .sorted((a, b) -> Double.compare(b.valor(), a.valor()))
                .toList();
        double total = ordenadas.stream().mapToDouble(Item::valor).sum();
        double acumulado = 0;

        List<Map<String, Object>> detalle = new ArrayList<>();
        List<Map<String, Object>> serie = new ArrayList<>();
        int i = 0;
        for (Item it : ordenadas) {
            acumulado += it.valor();
            double pct = total > 0 ? it.valor() / total * 100 : 0;
            double pctAcum = total > 0 ? acumulado / total * 100 : 0;
            String categoria = pctAcum <= 80 ? "A" : pctAcum <= 95 ? "B" : "C";

            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("nombre", it.nombre());
            fila.put("valor", fmt(it.valor()));
            fila.put("participacionPct", fmt(pct));
            fila.put("acumuladoPct", fmt(pctAcum));
            fila.put("categoria", categoria);
            detalle.add(fila);

            Map<String, Object> punto = new LinkedHashMap<>();
            punto.put("indice", ++i);
            punto.put("acumuladoPct", fmt(pctAcum));
            punto.put("categoria", categoria);
            serie.add(punto);
        }

        long catA = detalle.stream().filter(d -> "A".equals(d.get("categoria"))).count();
        long catB = detalle.stream().filter(d -> "B".equals(d.get("categoria"))).count();
        long catC = detalle.stream().filter(d -> "C".equals(d.get("categoria"))).count();

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("totalItems", detalle.size());
        resultado.put("valorTotal", fmt(total));
        resultado.put("conteoA", catA);
        resultado.put("conteoB", catB);
        resultado.put("conteoC", catC);
        resultado.put("items", detalle);

        String interpretacion = "Clasificación ABC con " + detalle.size() + " ítems. "
                + "Categoría A (mayor control): " + catA + " · Categoría B: " + catB
                + " · Categoría C: " + catC + ". Las categorías A concentran ~80% del valor "
                + "y requieren control y revisión frecuente.";
        return ResultadoMotor.of("Clasificación ABC completada", resultado, serie, interpretacion);
    }

    private double fmt(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}