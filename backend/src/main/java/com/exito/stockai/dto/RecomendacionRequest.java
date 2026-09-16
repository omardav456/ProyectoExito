package com.exito.stockai.dto;

public record RecomendacionRequest(Long productoId, String tipo, String titulo,
                                    String descripcion, String accionSugerida,
                                    String prioridad) {}
