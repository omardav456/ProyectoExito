package com.exito.stockai.dto;

public record DemandaRequest(Long productoId, java.time.LocalDate fecha, Integer unidades, String tipo) {}
