package com.exito.stockai.service.inventario;

import com.exito.stockai.dto.AlertaRequest;
import com.exito.stockai.dto.AlertaResponse;
import com.exito.stockai.model.inventario.Alerta;
import com.exito.stockai.model.inventario.Producto;
import com.exito.stockai.model.inventario.enums.EstadoAlerta;
import com.exito.stockai.model.inventario.enums.TipoAlerta;
import com.exito.stockai.repository.AlertaRepository;
import com.exito.stockai.repository.ProductoRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AlertaService {

    private final AlertaRepository alertaRepository;
    private final ProductoRepository productoRepository;

    public AlertaService(AlertaRepository alertaRepository,
                         ProductoRepository productoRepository) {
        this.alertaRepository = alertaRepository;
        this.productoRepository = productoRepository;
    }

    public List<AlertaResponse> buscar(String estado, String tipo) {
        List<Alerta> alertas;
        if (estado != null && tipo != null) {
            alertas = alertaRepository.findByEstadoAndTipoOrderByHoraDesc(
                    EstadoAlerta.valueOf(estado), TipoAlerta.valueOf(tipo));
        } else if (estado != null) {
            alertas = alertaRepository.findByEstadoOrderByHoraDesc(EstadoAlerta.valueOf(estado));
        } else {
            alertas = alertaRepository.findAllByOrderByHoraDesc();
        }
        return alertas.stream().map(this::toResponse).toList();
    }

    public long[] contarActivasPorTipo() {
        long critico = 0, advertencia = 0, info = 0;
        for (var row : alertaRepository.contarActivasPorTipo()) {
            var tipo = (TipoAlerta) row[0];
            var count = (Long) row[1];
            switch (tipo) {
                case CRITICO -> critico = count;
                case ADVERTENCIA -> advertencia = count;
                case INFO -> info = count;
            }
        }
        return new long[]{critico, advertencia, info};
    }

    @Transactional
    public AlertaResponse crear(AlertaRequest req) {
        Producto producto = null;
        if (req.productoId() != null) {
            producto = productoRepository.findById(req.productoId())
                    .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + req.productoId()));
        }
        var alerta = Alerta.builder()
                .producto(producto)
                .tipo(TipoAlerta.valueOf(req.tipo()))
                .prioridad(req.prioridad() != null ? req.prioridad() : "MEDIA")
                .mensaje(req.mensaje())
                .estado(EstadoAlerta.ACTIVA)
                .build();
        return toResponse(alertaRepository.save(alerta));
    }

    @Transactional
    public AlertaResponse resolver(Long id) {
        var alerta = alertaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Alerta no encontrada: " + id));
        alerta.setEstado(EstadoAlerta.RESUELTA);
        return toResponse(alertaRepository.save(alerta));
    }

    @Transactional
    public void eliminar(Long id) {
        alertaRepository.deleteById(id);
    }

    @Transactional
    public void regenerar() {
        var activas = alertaRepository.findByEstadoOrderByHoraDesc(EstadoAlerta.ACTIVA);
        for (var a : activas) {
            a.setEstado(EstadoAlerta.RESUELTA);
        }
        alertaRepository.saveAll(activas);

        var nuevas = new ArrayList<Alerta>();
        for (var p : productoRepository.findAll()) {
            if (!Boolean.TRUE.equals(p.getActivo())) continue;
            var estado = computeEstado(p);
            switch (estado) {
                case "AGOTADO" -> nuevas.add(buildAlerta(p, TipoAlerta.CRITICO, "ALTA",
                        "Stock agotado para " + p.getNombre()));
                case "CRITICO" -> nuevas.add(buildAlerta(p, TipoAlerta.CRITICO, "ALTA",
                        "Stock crítico para " + p.getNombre() + " (" + p.getStockActual() + " unidades)"));
                case "BAJO" -> nuevas.add(buildAlerta(p, TipoAlerta.ADVERTENCIA, "MEDIA",
                        "Stock bajo para " + p.getNombre()));
                case "SOBRESTOCK" -> nuevas.add(buildAlerta(p, TipoAlerta.INFO, "BAJA",
                        "Sobrestock detectado en " + p.getNombre()));
                default -> { }
            }
        }
        if (!nuevas.isEmpty()) {
            alertaRepository.saveAll(nuevas);
        }
    }

    private Alerta buildAlerta(Producto p, TipoAlerta tipo, String prioridad, String mensaje) {
        return Alerta.builder()
                .producto(p)
                .tipo(tipo)
                .prioridad(prioridad)
                .mensaje(mensaje)
                .estado(EstadoAlerta.ACTIVA)
                .build();
    }

    private String computeEstado(Producto p) {
        var ventasPromedio = (double) p.getTasaReposicion();
        if (p.getStockActual() == 0) return "AGOTADO";
        if (p.getStockActual() < p.getStockMinimo() * 0.25) return "CRITICO";
        if (p.getStockActual() < p.getStockMinimo()) return "BAJO";
        if (ventasPromedio > 0 && (p.getStockActual() / ventasPromedio) > 14) return "SOBRESTOCK";
        return "OK";
    }

    private AlertaResponse toResponse(Alerta a) {
        var productoNombre = a.getProducto() != null ? a.getProducto().getNombre() : null;
        return new AlertaResponse(
                a.getId(),
                a.getProducto() != null ? a.getProducto().getId() : null,
                productoNombre,
                a.getTipo().name(),
                a.getPrioridad(),
                a.getMensaje(),
                a.getHora(),
                a.getEstado().name()
        );
    }
}