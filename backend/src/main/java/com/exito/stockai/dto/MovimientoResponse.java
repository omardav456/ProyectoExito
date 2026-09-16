package com.exito.stockai.dto;

public record MovimientoResponse(
    Long id,
    Long productoId,
    String productoNombre,
    String tipo,
    Integer cantidad,
    java.time.LocalDate fecha,
    String descripcion
) {}
