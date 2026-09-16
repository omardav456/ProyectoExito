package com.exito.stockai.security;

import com.exito.stockai.security.JwtService.TokenPayload;

/**
 * Principal de Spring Security reconstruido desde el token JWT.
 * Los claims provienen de un token firmado, nunca del cliente.
 */
public record UsuarioAutenticado(TokenPayload payload) {

    public Long getId() {
        return payload.id();
    }

    public String getNombre() {
        return payload.nombre();
    }

    public String getRol() {
        return payload.rol();
    }

    @Override
    public String toString() {
        return payload.email();
    }
}