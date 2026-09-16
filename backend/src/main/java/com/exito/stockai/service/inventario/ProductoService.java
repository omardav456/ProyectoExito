package com.exito.stockai.service.inventario;

import com.exito.stockai.dto.ProductoRequest;
import com.exito.stockai.dto.ProductoResponse;
import com.exito.stockai.model.inventario.Producto;
import com.exito.stockai.model.inventario.enums.TipoDemanda;
import com.exito.stockai.repository.CategoriaRepository;
import com.exito.stockai.repository.DemandaDiariaRepository;
import com.exito.stockai.repository.ProductoRepository;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final DemandaDiariaRepository demandaDiariaRepository;

    public ProductoService(ProductoRepository productoRepository,
                           CategoriaRepository categoriaRepository,
                           DemandaDiariaRepository demandaDiariaRepository) {
        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
        this.demandaDiariaRepository = demandaDiariaRepository;
    }

    public List<ProductoResponse> buscar(String q, Long categoriaId, Boolean activo) {
        return productoRepository.buscar(q, categoriaId, activo).stream()
                .map(this::toResponse)
                .toList();
    }

    public ProductoResponse buscarPorId(Long id) {
        return toResponse(productoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + id)));
    }

    @Transactional
    public ProductoResponse crear(ProductoRequest req) {
        if (req.nombre() == null || req.nombre().isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
        if (req.categoriaId() == null) {
            throw new IllegalArgumentException("La categoría es obligatoria");
        }
        var categoria = categoriaRepository.findById(req.categoriaId())
                .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada: " + req.categoriaId()));
        var producto = Producto.builder()
                .nombre(req.nombre())
                .categoria(categoria)
                .stockActual(req.stockActual() != null ? req.stockActual() : 0)
                .stockMinimo(req.stockMinimo() != null ? req.stockMinimo() : 0)
                .precio(req.precio())
                .tasaReposicion(req.tasaReposicion() != null ? req.tasaReposicion() : 0)
                .unidad(req.unidad())
                .activo(req.activo() != null ? req.activo() : true)
                .build();
        return toResponse(productoRepository.save(producto));
    }

    @Transactional
    public ProductoResponse actualizar(Long id, ProductoRequest req) {
        var producto = productoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + id));
        if (req.nombre() != null) producto.setNombre(req.nombre());
        if (req.categoriaId() != null) {
            var categoria = categoriaRepository.findById(req.categoriaId())
                    .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada: " + req.categoriaId()));
            producto.setCategoria(categoria);
        }
        if (req.stockActual() != null) producto.setStockActual(req.stockActual());
        if (req.stockMinimo() != null) producto.setStockMinimo(req.stockMinimo());
        if (req.precio() != null) producto.setPrecio(req.precio());
        if (req.tasaReposicion() != null) producto.setTasaReposicion(req.tasaReposicion());
        if (req.unidad() != null) producto.setUnidad(req.unidad());
        if (req.activo() != null) producto.setActivo(req.activo());
        return toResponse(productoRepository.save(producto));
    }

    @Transactional
    public void eliminar(Long id) {
        var producto = productoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + id));
        producto.setActivo(false);
        productoRepository.save(producto);
    }

    ProductoResponse toResponse(Producto p) {
        var ventasPromedio = calcularVentasPromedio(p);
        var demandaPrevista = calcularDemandaPrevista(p);
        var estado = calcularEstado(p, ventasPromedio);
        var diasInv = diasInventario(p, ventasPromedio);
        var accion = accionRecomendada(estado, null);
        return new ProductoResponse(
                p.getId(),
                p.getNombre(),
                p.getCategoria().getId(),
                p.getCategoria().getNombre(),
                p.getCategoria().getColor(),
                p.getStockActual(),
                p.getStockMinimo(),
                p.getPrecio(),
                p.getTasaReposicion(),
                p.getUnidad(),
                p.getActivo(),
                ventasPromedio,
                demandaPrevista,
                diasInv,
                estado,
                accion,
                p.getDescripcion(),
                p.getImagen()
        );
    }

    private double calcularVentasPromedio(Producto p) {
        var registros = demandaDiariaRepository.findByProductoIdAndTipoOrderByFechaDesc(
                p.getId(), TipoDemanda.REAL, PageRequest.of(0, 14));
        if (registros.isEmpty()) {
            return p.getTasaReposicion();
        }
        return registros.stream().mapToInt(d -> d.getUnidades()).average().orElse(p.getTasaReposicion());
    }

    private int calcularDemandaPrevista(Producto p) {
        var registros = demandaDiariaRepository.findByProductoIdAndTipoOrderByFechaDesc(
                p.getId(), TipoDemanda.PREVISTA, PageRequest.of(0, 7));
        if (registros.isEmpty()) {
            return p.getTasaReposicion();
        }
        return (int) Math.round(registros.stream().mapToInt(d -> d.getUnidades()).average().orElse(p.getTasaReposicion()));
    }

    String calcularEstado(Producto p, double ventasPromedio) {
        if (p.getStockActual() == 0) return "AGOTADO";
        if (p.getStockActual() < p.getStockMinimo() * 0.25) return "CRITICO";
        if (p.getStockActual() < p.getStockMinimo()) return "BAJO";
        if (diasInventario(p, ventasPromedio) > 14) return "SOBRESTOCK";
        return "OK";
    }

    int diasInventario(Producto p, double ventasProm) {
        return ventasProm > 0 ? (int) (p.getStockActual() / ventasProm) : 999;
    }

    String accionRecomendada(String estado, Double diasHastaAgotamiento) {
        return switch (estado) {
            case "CRITICO" -> "Pedir hoy";
            case "BAJO" -> "Pedir pronto";
            case "SOBRESTOCK" -> "Pausar pedido";
            case "AGOTADO" -> "Pedido urgente inmediato";
            default -> "Monitorear";
        };
    }
}
