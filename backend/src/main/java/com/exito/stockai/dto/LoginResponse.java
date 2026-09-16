package com.exito.stockai.dto;

public record LoginResponse(
        String token,
        String tipo,
        UsuarioResponse usuario
) {
}