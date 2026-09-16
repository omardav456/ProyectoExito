package com.exito.stockai.dto;

public record MovimientoRequest(
    Long productoId,
    String tipo,
    Integer cantidad,
    java.time.LocalDate fecha,
    String descripcion
) {}
