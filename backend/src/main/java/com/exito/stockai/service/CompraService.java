package com.exito.stockai.service;

import com.exito.stockai.dto.CompraDetalleResponse;
import com.exito.stockai.dto.CompraItemRequest;
import com.exito.stockai.dto.CompraRequest;
import com.exito.stockai.dto.CompraResponse;
import com.exito.stockai.exception.BadRequestException;
import com.exito.stockai.exception.CredencialesInvalidasException;
import com.exito.stockai.model.auditoria.OperacionAuditoria;
import com.exito.stockai.model.compra.Compra;
import com.exito.stockai.model.compra.DetalleCompra;
import com.exito.stockai.model.inventario.MovimientoInventario;
import com.exito.stockai.model.inventario.Producto;
import com.exito.stockai.model.inventario.enums.TipoMovimiento;
import com.exito.stockai.model.security.Usuario;
import com.exito.stockai.repository.CompraRepository;
import com.exito.stockai.repository.DetalleCompraRepository;
import com.exito.stockai.repository.MovimientoInventarioRepository;
import com.exito.stockai.repository.ProductoRepository;
import com.exito.stockai.repository.UsuarioRepository;
import com.exito.stockai.security.UsuarioAutenticado;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Compra simulada: valida stock, crea compra + detalle en una sola transacción,
 * decrementa inventario, registra movimiento SALIDA y auditoría COMPRA_SIMULADA.
 */
@Service
public class CompraService {

    private final CompraRepository compraRepository;
    private final DetalleCompraRepository detalleRepository;
    private final ProductoRepository productoRepository;
    private final MovimientoInventarioRepository movimientoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaService auditoriaService;

    public CompraService(CompraRepository compraRepository,
            DetalleCompraRepository detalleRepository,
            ProductoRepository productoRepository,
            MovimientoInventarioRepository movimientoRepository,
            UsuarioRepository usuarioRepository,
            AuditoriaService auditoriaService) {
        this.compraRepository = compraRepository;
        this.detalleRepository = detalleRepository;
        this.productoRepository = productoRepository;
        this.movimientoRepository = movimientoRepository;
        this.usuarioRepository = usuarioRepository;
        this.auditoriaService = auditoriaService;
    }

    @Transactional
    public CompraResponse crear(Authentication auth, CompraRequest request) {
        if (auth == null || !(auth.getPrincipal() instanceof UsuarioAutenticado principal)) {
            throw new CredencialesInvalidasException("Debe iniciar sesión para realizar una compra");
        }
        Usuario usuario = usuarioRepository.findById(principal.getId())
                .orElseThrow(() -> new CredencialesInvalidasException("Usuario no encontrado"));
        if (request.items() == null || request.items().isEmpty()) {
            throw new BadRequestException("El carrito está vacío");
        }

        List<ItemPreparado> preparados = new ArrayList<>();
        for (CompraItemRequest item : request.items()) {
            Producto producto = productoRepository.findById(item.productoId())
                    .orElseThrow(() -> new BadRequestException("Producto no encontrado: " + item.productoId()));
            int cantidad = item.cantidad() == null ? 0 : item.cantidad();
            if (cantidad <= 0) {
                throw new BadRequestException("La cantidad del producto " + producto.getNombre() + " debe ser mayor a 0");
            }
            if (producto.getStockActual() < cantidad) {
                throw new BadRequestException("No hay suficiente inventario para \"" + producto.getNombre()
                        + "\". Stock disponible: " + producto.getStockActual()
                        + ", solicitado: " + cantidad);
            }
            preparados.add(new ItemPreparado(producto, cantidad));
        }

        BigDecimal total = preparados.stream()
                .map(p -> p.producto().getPrecio().multiply(BigDecimal.valueOf(p.cantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Compra compra = Compra.builder()
                .usuario(usuario)
                .estado("REALIZADA")
                .total(total)
                .build();
        compra = compraRepository.save(compra);

        List<CompraDetalleResponse> detallesResp = new ArrayList<>();
        for (ItemPreparado p : preparados) {
            Producto producto = p.producto();
            var subtotal = producto.getPrecio().multiply(BigDecimal.valueOf(p.cantidad()));
            DetalleCompra detalle = DetalleCompra.builder()
                    .compra(compra)
                    .producto(producto)
                    .cantidad(p.cantidad())
                    .precioUnitario(producto.getPrecio())
                    .subtotal(subtotal)
                    .build();
            detalleRepository.save(detalle);

            int stockAnterior = producto.getStockActual();
            producto.setStockActual(stockAnterior - p.cantidad());
            productoRepository.save(producto);

            movimientoRepository.save(MovimientoInventario.builder()
                    .producto(producto)
                    .tipo(TipoMovimiento.SALIDA)
                    .cantidad(p.cantidad())
                    .fecha(LocalDate.now())
                    .descripcion("Venta simulada (compra #" + compra.getId() + ")")
                    .usuario(usuario)
                    .build());

            auditoriaService.registrar(producto, OperacionAuditoria.COMPRA_SIMULADA,
                    stockAnterior, p.cantidad(), producto.getStockActual(),
                    "Compra simulada #" + compra.getId());

            detallesResp.add(new CompraDetalleResponse(producto.getId(), producto.getNombre(),
                    producto.getUnidad(), p.cantidad(), producto.getPrecio(), subtotal));
        }

        return toResponse(compra, detallesResp, usuario);
    }

    @Transactional(readOnly = true)
    public List<CompraResponse> listar() {
        return compraRepository.findAllByOrderByFechaDesc().stream()
                .map(this::toResponseCompleta)
                .toList();
    }

    private CompraResponse toResponse(Compra compra, List<CompraDetalleResponse> items, Usuario usuario) {
        return new CompraResponse(compra.getId(), usuario.getId(), usuario.getNombre(), usuario.getEmail(),
                compra.getEstado(), compra.getTotal(), compra.getFecha(), items);
    }

    private CompraResponse toResponseCompleta(Compra compra) {
        List<CompraDetalleResponse> items = detalleRepository.findByCompraIdOrderByIdAsc(compra.getId()).stream()
                .map(d -> new CompraDetalleResponse(d.getProducto().getId(), d.getProducto().getNombre(),
                        d.getProducto().getUnidad(), d.getCantidad(), d.getPrecioUnitario(), d.getSubtotal()))
                .toList();
        Usuario usuario = compra.getUsuario();
        return new CompraResponse(compra.getId(), usuario.getId(), usuario.getNombre(), usuario.getEmail(),
                compra.getEstado(), compra.getTotal(), compra.getFecha(), items);
    }

    private record ItemPreparado(Producto producto, int cantidad) {
    }
}