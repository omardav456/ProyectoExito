package com.exito.stockai.dto;

public record AlertaRequest(Long productoId, String tipo, String prioridad, String mensaje) {}
