package com.exito.stockai.dto;

import java.util.List;

public record ModeloListResponse(
    long total,
    int page,
    int size,
    List<ModeloResumen> modelos
) {
    public record ModeloResumen(
        Long id,
        String codigo,
        String nombre,
        String tipoModelo,
        String metodoSugerido,
        String complejidad,
        String estado,
        String area,
        String categoria,
        String subcategoria
    ) {}
}
