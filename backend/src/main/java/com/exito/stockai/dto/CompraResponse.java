package com.exito.stockai.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record CompraResponse(
        Long id,
        Long usuarioId,
        String usuarioNombre,
        String email,
        String estado,
        BigDecimal total,
        OffsetDateTime fecha,
        List<CompraDetalleResponse> items
) {
}