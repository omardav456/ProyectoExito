package com.exito.stockai.dto;

public record EjecucionResponse(
    Long id,
    String modeloCodigo,
    String parametros,
    String resultado,
    String estado,
    String mensaje,
    java.time.OffsetDateTime creadaEn
) {}
