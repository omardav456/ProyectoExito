package com.exito.stockai.dto;

import com.exito.stockai.model.security.Usuario;
import java.time.OffsetDateTime;

public record UsuarioResponse(
        Long id,
        String email,
        String nombre,
        String rol,
        Boolean activo,
        OffsetDateTime createdAt
) {
    public static UsuarioResponse from(Usuario u) {
        return new UsuarioResponse(
                u.getId(),
                u.getEmail(),
                u.getNombre(),
                u.getRol().getNombre(),
                u.getActivo(),
                u.getCreatedAt());
    }
}