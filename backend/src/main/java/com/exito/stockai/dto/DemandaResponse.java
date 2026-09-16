package com.exito.stockai.dto;

public record DemandaResponse(Long id, Long productoId, String productoNombre,
                               java.time.LocalDate fecha, Integer unidades, String tipo) {}
