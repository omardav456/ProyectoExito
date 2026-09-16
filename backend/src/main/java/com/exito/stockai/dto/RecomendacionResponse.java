package com.exito.stockai.dto;

public record RecomendacionResponse(Long id, Long productoId, String productoNombre,
                                     String tipo, String titulo, String descripcion,
                                     String accionSugerida, String prioridad,
                                     java.time.LocalDate fecha, Boolean activa) {}
