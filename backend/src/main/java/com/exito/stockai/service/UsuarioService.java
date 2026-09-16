package com.exito.stockai.service;

import com.exito.stockai.dto.UsuarioRequest;
import com.exito.stockai.dto.UsuarioResponse;
import com.exito.stockai.exception.BadRequestException;
import com.exito.stockai.exception.ResourceNotFoundException;
import com.exito.stockai.model.security.Rol;
import com.exito.stockai.model.security.Usuario;
import com.exito.stockai.repository.RolRepository;
import com.exito.stockai.repository.UsuarioRepository;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        return usuarioRepository.findAll().stream()
                .map(UsuarioResponse::from)
                .toList();
    }

    @Transactional
    public UsuarioResponse crear(UsuarioRequest request) {
        if (usuarioRepository.existsByEmailIgnoreCase(request.email().trim())) {
            throw new BadRequestException("Ya existe un usuario con ese email");
        }
        if (request.password() == null || request.password().isBlank()) {
            throw new BadRequestException("La contraseña es obligatoria");
        }
        Rol rol = obtenerRol(request.rol());
        Usuario usuario = Usuario.builder()
                .email(request.email().trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.password()))
                .nombre(request.nombre().trim())
                .rol(rol)
                .activo(request.activo())
                .build();
        return UsuarioResponse.from(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioResponse actualizar(Long id, UsuarioRequest request) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        var existe = usuarioRepository.findByEmailIgnoreCase(request.email().trim());
        if (existe.isPresent() && !existe.get().getId().equals(id)) {
            throw new BadRequestException("Ya existe un usuario con ese email");
        }
        Rol rol = obtenerRol(request.rol());
        usuario.setEmail(request.email().trim().toLowerCase());
        usuario.setNombre(request.nombre().trim());
        usuario.setRol(rol);
        usuario.setActivo(request.activo());
        if (request.password() != null && !request.password().isBlank()) {
            usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        return UsuarioResponse.from(usuarioRepository.save(usuario));
    }

    private Rol obtenerRol(String nombre) {
        return rolRepository.findByNombre(nombre.trim().toUpperCase())
                .orElseThrow(() -> new BadRequestException(
                        "Rol inválido. Roles permitidos: ADMINISTRADOR, EMPLEADO, CLIENTE, PROVEEDOR"));
    }
}