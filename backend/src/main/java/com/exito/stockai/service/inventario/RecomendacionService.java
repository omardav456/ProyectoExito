package com.exito.stockai.service.inventario;

import com.exito.stockai.dto.RecomendacionRequest;
import com.exito.stockai.dto.RecomendacionResponse;
import com.exito.stockai.model.inventario.Producto;
import com.exito.stockai.model.inventario.Recomendacion;
import com.exito.stockai.repository.ProductoRepository;
import com.exito.stockai.repository.RecomendacionRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RecomendacionService {

    private final RecomendacionRepository recomendacionRepository;
    private final ProductoRepository productoRepository;

    public RecomendacionService(RecomendacionRepository recomendacionRepository,
                                ProductoRepository productoRepository) {
        this.recomendacionRepository = recomendacionRepository;
        this.productoRepository = productoRepository;
    }

    public List<RecomendacionResponse> listar(Boolean soloActivas) {
        var list = Boolean.TRUE.equals(soloActivas)
                ? recomendacionRepository.findByActivaTrueOrderByPrioridadDescFechaDesc()
                : recomendacionRepository.findAllByOrderByPrioridadDescFechaDesc();
        return list.stream().map(this::toResponse).toList();
    }

    @Transactional
    public RecomendacionResponse crear(RecomendacionRequest req) {
        Producto producto = null;
        if (req.productoId() != null) {
            producto = productoRepository.findById(req.productoId())
                    .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + req.productoId()));
        }
        var recomendacion = Recomendacion.builder()
                .producto(producto)
                .tipo(req.tipo())
                .titulo(req.titulo())
                .descripcion(req.descripcion())
                .accionSugerida(req.accionSugerida())
                .prioridad(req.prioridad() != null ? req.prioridad() : "MEDIA")
                .fecha(LocalDate.now())
                .activa(true)
                .build();
        return toResponse(recomendacionRepository.save(recomendacion));
    }

    private RecomendacionResponse toResponse(Recomendacion r) {
        return new RecomendacionResponse(
                r.getId(),
                r.getProducto() != null ? r.getProducto().getId() : null,
                r.getProducto() != null ? r.getProducto().getNombre() : null,
                r.getTipo(),
                r.getTitulo(),
                r.getDescripcion(),
                r.getAccionSugerida(),
                r.getPrioridad(),
                r.getFecha(),
                r.getActiva()
        );
    }
}