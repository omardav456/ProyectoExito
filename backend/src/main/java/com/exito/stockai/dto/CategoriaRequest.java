package com.exito.stockai.dto;

import jakarta.validation.constraints.NotBlank;

public record CategoriaRequest(
    @NotBlank String nombre,
    String descripcion,
    String color,
    Boolean activa
) {}
