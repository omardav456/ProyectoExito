package com.exito.stockai.dto;

import java.util.List;

public record StockFlowResponse(
    String categoria,
    List<FilaFlujo> filas,
    String formula
) {
    public record FilaFlujo(String t, Integer inventario, Integer entradas, Integer salidas) {}
}
