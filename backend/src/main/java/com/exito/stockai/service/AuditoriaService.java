package com.exito.stockai.service;

import com.exito.stockai.dto.AuditoriaResponse;
import com.exito.stockai.model.auditoria.Auditoria;
import com.exito.stockai.model.auditoria.OperacionAuditoria;
import com.exito.stockai.model.inventario.Producto;
import com.exito.stockai.model.security.Usuario;
import com.exito.stockai.repository.AuditoriaRepository;
import com.exito.stockai.repository.UsuarioRepository;
import com.exito.stockai.security.UsuarioAutenticado;
import java.time.LocalDate;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registro obligatorio e inmutable de cada modificación de inventario.
 * El usuario se obtiene del contexto de seguridad, nunca del request.
 */
@Service
public class AuditoriaService {

    private final AuditoriaRepository auditoriaRepository;
    private final UsuarioRepository usuarioRepository;

    public AuditoriaService(AuditoriaRepository auditoriaRepository,
            UsuarioRepository usuarioRepository) {
        this.auditoriaRepository = auditoriaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public Auditoria registrar(Producto producto, OperacionAuditoria operacion,
            Integer stockAnterior, Integer cantidad, Integer stockPosterior, String observacion) {
        Usuario usuario = null;
        String rol = "SISTEMA";
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UsuarioAutenticado ua) {
            rol = ua.getRol().replace("ROLE_", "");
            usuario = usuarioRepository.findById(ua.getId()).orElse(null);
        }
        Auditoria auditoria = Auditoria.builder()
                .usuario(usuario)
                .rol(rol)
                .producto(producto)
                .operacion(operacion)
                .stockAnterior(stockAnterior)
                .cantidad(cantidad)
                .stockPosterior(stockPosterior)
                .observacion(observacion)
                .build();
        return auditoriaRepository.save(auditoria);
    }

    @Transactional(readOnly = true)
    public List<AuditoriaResponse> listar(Long usuarioId, Long productoId, String operacion,
            LocalDate desde, LocalDate hasta) {
        List<Auditoria> registros;
        if (usuarioId != null) {
            registros = auditoriaRepository.findByUsuarioIdOrderByFechaHoraDesc(usuarioId);
        } else if (productoId != null) {
            registros = auditoriaRepository.findByProductoIdOrderByFechaHoraDesc(productoId);
        } else if (operacion != null && !operacion.isBlank()) {
            registros = auditoriaRepository.findByOperacionOrderByFechaHoraDesc(
                    OperacionAuditoria.valueOf(operacion.trim().toUpperCase()));
        } else if (desde != null && hasta != null) {
            registros = auditoriaRepository.findByFechaBetweenOrderByFechaHoraDesc(desde, hasta);
        } else {
            registros = auditoriaRepository.findAllByOrderByFechaHoraDesc();
        }
        return registros.stream().map(this::toResponse).toList();
    }

    private AuditoriaResponse toResponse(Auditoria a) {
        return new AuditoriaResponse(
                a.getId(),
                a.getUsuario() != null ? a.getUsuario().getId() : null,
                a.getUsuario() != null ? a.getUsuario().getEmail() : null,
                a.getUsuario() != null ? a.getUsuario().getNombre() : null,
                a.getRol(),
                a.getProducto() != null ? a.getProducto().getId() : null,
                a.getProducto() != null ? a.getProducto().getNombre() : null,
                a.getOperacion().name(),
                a.getStockAnterior(),
                a.getCantidad(),
                a.getStockPosterior(),
                a.getFecha(),
                a.getHora(),
                a.getFechaHora(),
                a.getObservacion());
    }
}