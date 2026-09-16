package com.exito.stockai.service;

import com.exito.stockai.dto.LoginRequest;
import com.exito.stockai.dto.LoginResponse;
import com.exito.stockai.dto.UsuarioResponse;
import com.exito.stockai.exception.CredencialesInvalidasException;
import com.exito.stockai.exception.CuentaPendienteException;
import com.exito.stockai.exception.ResourceNotFoundException;
import com.exito.stockai.model.security.Usuario;
import com.exito.stockai.repository.UsuarioRepository;
import com.exito.stockai.security.JwtService;
import com.exito.stockai.security.UsuarioAutenticado;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(() -> new CredencialesInvalidasException("Credenciales incorrectas"));
        if (!passwordEncoder.matches(request.password(), usuario.getPasswordHash())) {
            throw new CredencialesInvalidasException("Credenciales incorrectas");
        }
        if (!usuario.getActivo()) {
            throw new CuentaPendienteException(
                    "Tu cuenta está pendiente de aprobación por el administrador");
        }
        return new LoginResponse(jwtService.generarToken(usuario), "Bearer", UsuarioResponse.from(usuario));
    }

    public UsuarioResponse me(Authentication authentication) {
        UsuarioAutenticado principal = (UsuarioAutenticado) authentication.getPrincipal();
        return usuarioRepository.findById(principal.getId())
                .map(UsuarioResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }
}