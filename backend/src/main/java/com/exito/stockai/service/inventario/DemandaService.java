package com.exito.stockai.service.inventario;

import com.exito.stockai.dto.DemandaRequest;
import com.exito.stockai.dto.DemandaResponse;
import com.exito.stockai.model.inventario.DemandaDiaria;
import com.exito.stockai.model.inventario.enums.TipoDemanda;
import com.exito.stockai.repository.DemandaDiariaRepository;
import com.exito.stockai.repository.ProductoRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DemandaService {

    private final DemandaDiariaRepository demandaDiariaRepository;
    private final ProductoRepository productoRepository;

    public DemandaService(DemandaDiariaRepository demandaDiariaRepository,
                          ProductoRepository productoRepository) {
        this.demandaDiariaRepository = demandaDiariaRepository;
        this.productoRepository = productoRepository;
    }

    public List<DemandaResponse> buscar(Long productoId, String tipo, LocalDate desde, LocalDate hasta) {
        TipoDemanda tipoEnum = tipo != null ? TipoDemanda.valueOf(tipo) : null;
        List<DemandaDiaria> demandas;
        if (productoId != null && tipoEnum != null) {
            demandas = demandaDiariaRepository.findByProductoIdAndTipoAndFechaBetweenOrderByFechaAsc(productoId, tipoEnum, desde, hasta);
        } else if (tipoEnum != null) {
            demandas = demandaDiariaRepository.findByTipoAndFechaBetweenOrderByFechaAsc(tipoEnum, desde, hasta);
        } else {
            demandas = demandaDiariaRepository.findAll();
        }
        return demandas.stream().map(this::toResponse).toList();
    }

    @Transactional
    public List<DemandaResponse> crearLote(List<DemandaRequest> reqs) {
        var entities = new ArrayList<DemandaDiaria>();
        for (var req : reqs) {
            var producto = productoRepository.findById(req.productoId())
                    .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + req.productoId()));
            var demanda = DemandaDiaria.builder()
                    .producto(producto)
                    .fecha(req.fecha() != null ? req.fecha() : LocalDate.now())
                    .unidades(req.unidades())
                    .tipo(TipoDemanda.valueOf(req.tipo()))
                    .build();
            entities.add(demanda);
        }
        var saved = demandaDiariaRepository.saveAll(entities);
        return saved.stream().map(this::toResponse).toList();
    }

    private DemandaResponse toResponse(DemandaDiaria d) {
        return new DemandaResponse(
                d.getId(),
                d.getProducto().getId(),
                d.getProducto().getNombre(),
                d.getFecha(),
                d.getUnidades(),
                d.getTipo().name()
        );
    }
}