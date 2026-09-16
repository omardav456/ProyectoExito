package com.exito.stockai.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UsuarioRequest(
        @NotBlank(message = "El email es obligatorio") @Email(message = "Email inválido") String email,
        String password,
        @NotBlank(message = "El nombre es obligatorio") String nombre,
        @NotBlank(message = "El rol es obligatorio") String rol,
        @NotNull(message = "El estado activo es obligatorio") Boolean activo
) {
}