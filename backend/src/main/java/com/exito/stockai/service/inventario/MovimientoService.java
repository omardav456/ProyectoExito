package com.exito.stockai.service.inventario;

import com.exito.stockai.dto.MovimientoRequest;
import com.exito.stockai.dto.MovimientoResponse;
import com.exito.stockai.exception.BadRequestException;
import com.exito.stockai.model.auditoria.OperacionAuditoria;
import com.exito.stockai.model.inventario.MovimientoInventario;
import com.exito.stockai.model.inventario.Producto;
import com.exito.stockai.model.inventario.enums.TipoMovimiento;
import com.exito.stockai.model.security.Usuario;
import com.exito.stockai.repository.MovimientoInventarioRepository;
import com.exito.stockai.repository.ProductoRepository;
import com.exito.stockai.repository.UsuarioRepository;
import com.exito.stockai.security.UsuarioAutenticado;
import com.exito.stockai.service.AuditoriaService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MovimientoService {

    private final MovimientoInventarioRepository movimientoRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaService auditoriaService;

    public MovimientoService(MovimientoInventarioRepository movimientoRepository,
                             ProductoRepository productoRepository,
                             UsuarioRepository usuarioRepository,
                             AuditoriaService auditoriaService) {
        this.movimientoRepository = movimientoRepository;
        this.productoRepository = productoRepository;
        this.usuarioRepository = usuarioRepository;
        this.auditoriaService = auditoriaService;
    }

    public List<MovimientoResponse> buscar(Long productoId, LocalDate desde, LocalDate hasta) {
        List<MovimientoInventario> movimientos;
        if (productoId != null) {
            movimientos = movimientoRepository.findByProductoIdAndFechaBetweenOrderByFechaAsc(productoId, desde, hasta);
        } else {
            movimientos = movimientoRepository.findByFechaBetweenOrderByFechaAsc(desde, hasta);
        }
        return movimientos.stream().map(this::toResponse).toList();
    }

    @Transactional
    public MovimientoResponse crear(MovimientoRequest req) {
        if (req.productoId() == null || req.tipo() == null || req.cantidad() == null || req.cantidad() <= 0) {
            throw new BadRequestException("productoId, tipo y cantidad (>0) son obligatorios");
        }
        var producto = productoRepository.findById(req.productoId())
                .orElseThrow(() -> new BadRequestException("Producto no encontrado: " + req.productoId()));
        TipoMovimiento tipo;
        try {
            tipo = TipoMovimiento.valueOf(req.tipo().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Tipo de movimiento inválido: " + req.tipo());
        }

        int stockAnterior = producto.getStockActual();
        switch (tipo) {
            case ENTRADA -> producto.setStockActual(stockAnterior + req.cantidad());
            case SALIDA -> {
                if (stockAnterior < req.cantidad()) {
                    throw new BadRequestException("Stock insuficiente: disponible="
                            + stockAnterior + ", solicitado=" + req.cantidad());
                }
                producto.setStockActual(stockAnterior - req.cantidad());
            }
            case AJUSTE -> producto.setStockActual(stockAnterior + req.cantidad());
        }

        var movimiento = MovimientoInventario.builder()
                .producto(producto)
                .tipo(tipo)
                .cantidad(req.cantidad())
                .fecha(req.fecha() != null ? req.fecha() : LocalDate.now())
                .descripcion(req.descripcion())
                .usuario(usuarioActual())
                .build();

        productoRepository.save(producto);
        MovimientoResponse resp = toResponse(movimientoRepository.save(movimiento));

        OperacionAuditoria auditoriaOperacion = switch (tipo) {
            case ENTRADA -> OperacionAuditoria.ENTRADA;
            case SALIDA -> OperacionAuditoria.SALIDA;
            case AJUSTE -> OperacionAuditoria.AJUSTE;
        };
        auditoriaService.registrar(producto, auditoriaOperacion, stockAnterior,
                req.cantidad(), producto.getStockActual(), req.descripcion());
        return resp;
    }

    private Usuario usuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UsuarioAutenticado ua) {
            return usuarioRepository.findById(ua.getId()).orElse(null);
        }
        return null;
    }

    private MovimientoResponse toResponse(MovimientoInventario m) {
        return new MovimientoResponse(
                m.getId(),
                m.getProducto().getId(),
                m.getProducto().getNombre(),
                m.getTipo().name(),
                m.getCantidad(),
                m.getFecha(),
                m.getDescripcion()
        );
    }
}