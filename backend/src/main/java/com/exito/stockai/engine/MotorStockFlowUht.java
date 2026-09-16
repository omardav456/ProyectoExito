package com.exito.stockai.engine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Exposición del motor Stock & Flow (fuente fiel de Solucion/codigo.py)
 * como implementación de MotorMatematico para el catálogo.
 */
@Component
public class MotorStockFlowUht implements MotorMatematico {

    @Override
    public String nombreMotor() {
        return "STOCKFLOW_UHT";
    }

    @Override
    public ResultadoMotor ejecutar(Map<String, Object> p) {
        int inventarioInicial = (int) Math.round(num(p, "inventarioInicial"));
        int reposicion = (int) Math.round(num(p, "reposicion"));
        int dias = (int) Math.round(num(p, "dias", 14));
        List<Integer> ventasSemana = new ArrayList<>();
        if (p.get("ventasSemana") instanceof List<?> lista) {
            for (Object o : lista) {
                ventasSemana.add((int) Math.round(((Number) o).doubleValue()));
            }
        }
        if (ventasSemana.isEmpty()) {
            ventasSemana = List.of(8, 12, 15, 10, 14, 18, 9);
        }

        MotorStockFlow.Resultado r = MotorStockFlow.simular(inventarioInicial, reposicion, ventasSemana, dias);

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("inventarioInicial", r.inventarioInicial());
        resultado.put("inventarioFinal", r.inventarioFinal());
        resultado.put("ventasTotales", r.ventasTotales());
        resultado.put("reposicionTotal", r.reposicionTotal());
        resultado.put("balance", r.balance());
        resultado.put("balanceOk", r.balanceOk());
        resultado.put("inventarioMin", r.inventarioMin());
        resultado.put("diaMin", r.diaMin());
        resultado.put("diaAgotamiento", r.diaAgotamiento());
        resultado.put("ventaPromedio", fmt(r.ventaPromedio()));
        resultado.put("neteDiarioPromedio", fmt(r.neteDiarioPromedio()));
        resultado.put("cantidadSemanal", r.cantidadSemanal());
        resultado.put("diasHastaAgotamiento", r.diasHastaAgotamiento());

        List<Map<String, Object>> serie = new ArrayList<>();
        for (MotorStockFlow.Fila f : r.filas()) {
            Map<String, Object> punto = new LinkedHashMap<>();
            punto.put("dia", f.dia());
            punto.put("diaSemana", f.diaSemana());
            punto.put("inventario", f.inventarioFinal());
            serie.add(punto);
        }

        String interpretacion = "El inventario evoluciona según I(t+1) = I(t) + R − V(t). "
                + (r.balanceOk() ? "El balance cuadra (inventarioInicial + reposiciones − ventas = inventarioFinal). "
                        : "¡El balance NO cuadra! Revisar entradas o ventas.")
                + (r.diaAgotamiento() != null
                        ? " Se detecta agotamiento (stock<0) en el día " + r.diaAgotamiento() + "."
                        : " No hay agotamiento en el horizonte evaluado.")
                + (r.cantidadSemanal() != null
                        ? " Cantidad semanal sugerida para cubrir demanda: " + r.cantidadSemanal() + " unidades."
                        : "");
        return ResultadoMotor.of("Simulación Stock & Flow completada", resultado, serie, interpretacion);
    }

    private double num(Map<String, Object> p, String k) {
        return num(p, k, 0);
    }

    private double num(Map<String, Object> p, String k, double defecto) {
        Object v = p.get(k);
        return v instanceof Number n ? n.doubleValue() : defecto;
    }

    private double fmt(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}