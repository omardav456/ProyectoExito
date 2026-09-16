package com.exito.stockai.service.inventario;

import com.exito.stockai.dto.DashboardSummary.DiaDemanda;
import com.exito.stockai.engine.MotorStockFlow;
import com.exito.stockai.model.inventario.enums.TipoDemanda;
import com.exito.stockai.repository.DemandaDiariaRepository;
import com.exito.stockai.repository.ProductoRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PrediccionService {

    private static final String[] DIAS = {"Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"};

    private final DemandaDiariaRepository demandaDiariaRepository;
    private final ProductoRepository productoRepository;

    public PrediccionService(DemandaDiariaRepository demandaDiariaRepository,
                             ProductoRepository productoRepository) {
        this.demandaDiariaRepository = demandaDiariaRepository;
        this.productoRepository = productoRepository;
    }

    public DiaDemanda[] prediccionSemanal(Long productoId) {
        List<Double> historico;
        if (productoId != null) {
            historico = cargarHistorico(productoId);
        } else {
            historico = cargarHistoricoGlobal();
        }
        var predicciones = MotorStockFlow.predecir7Dias(historico);
        var resultado = new DiaDemanda[7];
        for (int i = 0; i < 7; i++) {
            resultado[i] = new DiaDemanda(DIAS[i], 0L, Math.round(predicciones[i]));
        }
        return resultado;
    }

    private List<Double> cargarHistorico(Long productoId) {
        var registros = demandaDiariaRepository.findByProductoIdAndTipoOrderByFechaDesc(
                productoId, TipoDemanda.REAL, PageRequest.of(0, 28));
        var historico = new ArrayList<Double>();
        for (int i = registros.size() - 1; i >= 0; i--) {
            historico.add((double) registros.get(i).getUnidades());
        }
        return historico;
    }

    private List<Double> cargarHistoricoGlobal() {
        var hace28 = LocalDate.now().minusDays(28);
        var ahora = LocalDate.now();
        var registros = demandaDiariaRepository.findByTipoAndFechaBetweenOrderByFechaAsc(
                TipoDemanda.REAL, hace28, ahora);
        var porFecha = new java.util.TreeMap<LocalDate, Integer>();
        for (var d : registros) {
            porFecha.merge(d.getFecha(), d.getUnidades(), Integer::sum);
        }
        var historico = new ArrayList<Double>();
        for (var unidades : porFecha.values()) {
            historico.add((double) unidades);
        }
        return historico;
    }
}