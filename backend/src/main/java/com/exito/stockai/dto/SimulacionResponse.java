package com.exito.stockai.dto;

public record SimulacionResponse(
    Long id,
    String modeloCodigo,
    String parametrosJson,
    String resultadoJson,
    String descripcion,
    java.time.OffsetDateTime creadaEn
) {}
