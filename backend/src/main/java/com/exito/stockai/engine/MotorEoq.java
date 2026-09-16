package com.exito.stockai.engine;

import com.exito.stockai.exception.BadRequestException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Modelo EOQ (Cantidad Económica de Pedido).
 * Q* = sqrt(2·D·S / H)  con D=demanda anual, S=costo por pedido, H=costo de almacenar por unidad/año.
 */
@Component
public class MotorEoq implements MotorMatematico {

    @Override
    public String nombreMotor() {
        return "EOQ";
    }

    @Override
    public ResultadoMotor ejecutar(Map<String, Object> p) {
        double demanda = req(p, "demandaAnual", "demandaAnual (unidades/año) es obligatoria");
        double costoPedido = req(p, "costoPedido", "costoPedido (por pedido) es obligatorio");
        double costoAlmacen = req(p, "costoAlmacenamiento", "costoAlmacenamiento (por unidad/año) es obligatorio");
        if (demanda <= 0 || costoPedido <= 0 || costoAlmacen <= 0) {
            throw new BadRequestException("EOQ: demandaAnual, costoPedido y costoAlmacenamiento deben ser > 0");
        }

        double q = Math.sqrt((2 * demanda * costoPedido) / costoAlmacen);
        int qEntero = (int) Math.round(q);
        double costoPedidoAnual = (demanda / qEntero) * costoPedido;
        double costoAlmacenAnual = (qEntero / 2.0) * costoAlmacen;
        double costoTotal = costoPedidoAnual + costoAlmacenAnual;
        double pedidosPorAnno = demanda / qEntero;
        double diasEntrePedidos = 365.0 / pedidosPorAnno;

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("qOptima", qEntero);
        resultado.put("demandaAnual", demanda);
        resultado.put("costoPedidoAnual", fmt(costoPedidoAnual));
        resultado.put("costoAlmacenamientoAnual", fmt(costoAlmacenAnual));
        resultado.put("costoTotalAnual", fmt(costoTotal));
        resultado.put("pedidosPorAno", fmt(pedidosPorAnno));
        resultado.put("diasEntrePedidos", fmt(diasEntrePedidos));

        List<Map<String, Object>> serie = new ArrayList<>();
        double[] qu = {0.4, 0.6, 0.8, 1.0, 1.2, 1.5, 2.0};
        for (double f : qu) {
            double v = qEntero * f;
            double cp = (demanda / v) * costoPedido;
            double ca = (v / 2.0) * costoAlmacen;
            Map<String, Object> punto = new LinkedHashMap<>();
            punto.put("q", Math.round(v));
            punto.put("costoPedido", fmt(cp));
            punto.put("costoAlmacenamiento", fmt(ca));
            punto.put("costoTotal", fmt(cp + ca));
            serie.add(punto);
        }

        String interpretacion = "La cantidad óptima de pedido es " + qEntero + " unidades, "
                + "lo que representa " + fmt(pedidosPorAnno) + " pedidos al año (uno cada ~"
                + fmt(diasEntrePedidos) + " días). "
                + "El costo total anual mínimo es " + fmt(costoTotal)
                + " (pedidos: " + fmt(costoPedidoAnual) + ", almacenamiento: " + fmt(costoAlmacenAnual) + ").";
        return ResultadoMotor.of("Cálculo EOQ completado", resultado, serie, interpretacion);
    }

    private double req(Map<String, Object> p, String k, String mensaje) {
        Object v = p.get(k);
        if (v == null) {
            throw new BadRequestException(mensaje);
        }
        return ((Number) v).doubleValue();
    }

    private double fmt(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}