package com.exito.stockai.service.inventario;

import com.exito.stockai.dto.StockFlowResponse;
import com.exito.stockai.dto.StockFlowResponse.FilaFlujo;
import com.exito.stockai.model.inventario.enums.TipoDemanda;
import com.exito.stockai.repository.CategoriaRepository;
import com.exito.stockai.repository.DemandaDiariaRepository;
import com.exito.stockai.repository.MovimientoInventarioRepository;
import com.exito.stockai.repository.ProductoRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class StockFlowService {

    private final MovimientoInventarioRepository movimientoRepository;
    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;
    private final DemandaDiariaRepository demandaDiariaRepository;

    public StockFlowService(MovimientoInventarioRepository movimientoRepository,
                            CategoriaRepository categoriaRepository,
                            ProductoRepository productoRepository,
                            DemandaDiariaRepository demandaDiariaRepository) {
        this.movimientoRepository = movimientoRepository;
        this.categoriaRepository = categoriaRepository;
        this.productoRepository = productoRepository;
        this.demandaDiariaRepository = demandaDiariaRepository;
    }

    public StockFlowResponse calcularFlujo(Long categoriaId, LocalDate desde, LocalDate hasta) {
        if (desde == null) desde = LocalDate.now().minusDays(6);
        if (hasta == null) hasta = LocalDate.now();

        var nombreCategoria = categoriaId != null
                ? categoriaRepository.findById(categoriaId).map(c -> c.getNombre()).orElse("Total")
                : "Total";

        var inventarioBase = calcularInventarioBase(categoriaId);

        var flujoMap = new LinkedHashMap<LocalDate, int[]>();
        var registros = movimientoRepository.flujoPorDia(null, desde, hasta);
        for (var r : registros) {
            flujoMap.put(r.getFecha(), new int[]{r.getEntradas().intValue(), r.getSalidas().intValue()});
        }

        if (flujoMap.isEmpty()) {
            flujoMap = sintetizarFlujo(categoriaId, desde, hasta);
        }

        var filas = new ArrayList<FilaFlujo>();
        int inventario = inventarioBase;
        int idx = 0;
        for (var entry : flujoMap.entrySet()) {
            int entradas = entry.getValue()[0];
            int salidas = entry.getValue()[1];
            filas.add(new FilaFlujo("t" + idx, inventario, entradas, salidas));
            inventario += entradas - salidas;
            idx++;
        }

        return new StockFlowResponse(nombreCategoria, filas, "I(t+1) = I(t) + Entradas(t) - Salidas(t)");
    }

    private int calcularInventarioBase(Long categoriaId) {
        return productoRepository.findAll().stream()
                .filter(p -> Boolean.TRUE.equals(p.getActivo()))
                .filter(p -> categoriaId == null || p.getCategoria().getId().equals(categoriaId))
                .mapToInt(p -> p.getStockActual())
                .sum();
    }

    private LinkedHashMap<LocalDate, int[]> sintetizarFlujo(Long categoriaId, LocalDate desde, LocalDate hasta) {
        var map = new LinkedHashMap<LocalDate, int[]>();
        var activos = productoRepository.findAll().stream()
                .filter(p -> Boolean.TRUE.equals(p.getActivo()))
                .filter(p -> categoriaId == null || p.getCategoria().getId().equals(categoriaId))
                .toList();
        int totalReposicion = activos.stream().mapToInt(p -> p.getTasaReposicion()).sum();

        var hace28 = desde.minusDays(28);
        var demandas = demandaDiariaRepository.findByTipoAndFechaBetweenOrderByFechaAsc(
                TipoDemanda.REAL, hace28, hasta);
        var demandaPorFecha = new LinkedHashMap<LocalDate, Integer>();
        for (var d : demandas) {
            demandaPorFecha.merge(d.getFecha(), d.getUnidades(), Integer::sum);
        }

        var dia = desde;
        while (!dia.isAfter(hasta)) {
            int entradas = totalReposicion;
            int salidas = demandaPorFecha.getOrDefault(dia, 0);
            map.put(dia, new int[]{entradas, salidas});
            dia = dia.plusDays(1);
        }
        return map;
    }
}