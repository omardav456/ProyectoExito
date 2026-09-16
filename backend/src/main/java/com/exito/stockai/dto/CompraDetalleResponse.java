package com.exito.stockai.dto;

import java.math.BigDecimal;

public record CompraDetalleResponse(
        Long productoId,
        String productoNombre,
        String unidad,
        Integer cantidad,
        BigDecimal precioUnitario,
        BigDecimal subtotal
) {
}