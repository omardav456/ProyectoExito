package com.exito.stockai.service.inventario;

import com.exito.stockai.dto.ResultadoSimulacion;
import com.exito.stockai.dto.SimulacionParamsRequest;
import com.exito.stockai.dto.SimulacionResponse;
import com.exito.stockai.engine.MotorStockFlow;
import com.exito.stockai.model.inventario.Simulacion;
import com.exito.stockai.repository.SimulacionRepository;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.core.JacksonException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SimulacionService {

    private final SimulacionRepository simulacionRepository;
    private final ObjectMapper objectMapper;

    public SimulacionService(SimulacionRepository simulacionRepository,
                             ObjectMapper objectMapper) {
        this.simulacionRepository = simulacionRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ResultadoSimulacion ejecutar(SimulacionParamsRequest req) {
        if (req.inventarioInicial() == null || req.inventarioInicial() < 0) {
            throw new IllegalArgumentException("inventarioInicial debe ser >= 0");
        }
        if (req.reposicion() == null || req.reposicion() < 0) {
            throw new IllegalArgumentException("reposicion debe ser >= 0");
        }
        if (req.dias() == null || req.dias() <= 0) {
            throw new IllegalArgumentException("dias debe ser > 0");
        }
        if (req.ventasSemana() == null || req.ventasSemana().size() != 7) {
            throw new IllegalArgumentException("ventasSemana debe tener exactamente 7 elementos");
        }

        var resultado = MotorStockFlow.simular(
                req.inventarioInicial(),
                req.reposicion(),
                req.ventasSemana(),
                req.dias());

        var parametrosJson = writeValue(req);
        var resultadoJson = writeValue(resultado);

        var simulacion = Simulacion.builder()
                .modeloCodigo("STOCKFLOW_UHT")
                .parametros(parametrosJson)
                .resultado(resultadoJson)
                .descripcion("Simulación StockFlow: I₀=" + req.inventarioInicial()
                        + ", R=" + req.reposicion() + ", días=" + req.dias())
                .build();
        simulacionRepository.save(simulacion);

        return new ResultadoSimulacion(
                resultado.inventarioInicial(),
                resultado.reposicion(),
                resultado.ventasSemana(),
                resultado.dias(),
                resultado.filas().stream()
                        .map(f -> new ResultadoSimulacion.Fila(f.dia(), f.diaSemana(), f.inventarioInicial(), f.reposicion(), f.venta(), f.inventarioFinal()))
                        .toList(),
                resultado.inventarioFinal(),
                resultado.ventasTotales(),
                resultado.reposicionTotal(),
                resultado.balance(),
                resultado.balanceOk(),
                resultado.inventarioMin(),
                resultado.diaMin(),
                resultado.diaAgotamiento(),
                resultado.ventaPromedio(),
                resultado.neteDiarioPromedio()
        );
    }

    public List<SimulacionResponse> listar() {
        return simulacionRepository.findAllByOrderByCreadaEnDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    public SimulacionResponse buscarPorId(Long id) {
        return toResponse(simulacionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Simulación no encontrada: " + id)));
    }

    @Transactional
    public void eliminar(Long id) {
        simulacionRepository.deleteById(id);
    }

    private SimulacionResponse toResponse(Simulacion s) {
        return new SimulacionResponse(
                s.getId(),
                s.getModeloCodigo(),
                s.getParametros(),
                s.getResultado(),
                s.getDescripcion(),
                s.getCreadaEn()
        );
    }

    private String writeValue(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JacksonException e) {
            throw new RuntimeException("Error serializando a JSON", e);
        }
    }
}