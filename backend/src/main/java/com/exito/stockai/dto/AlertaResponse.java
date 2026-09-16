package com.exito.stockai.dto;

public record AlertaResponse(Long id, Long productoId, String productoNombre,
                              String tipo, String prioridad, String mensaje,
                              java.time.OffsetDateTime hora, String estado) {}
