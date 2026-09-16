package com.exito.stockai.service.modelos;

import com.exito.stockai.dto.EjecutarModeloRequest;
import com.exito.stockai.dto.EjecutarModeloResponse;
import com.exito.stockai.engine.MotorMatematico;
import com.exito.stockai.engine.MotorStockFlowUht;
import com.exito.stockai.engine.ResultadoMotor;
import com.exito.stockai.exception.BadRequestException;
import com.exito.stockai.exception.ConflictoException;
import com.exito.stockai.exception.ResourceNotFoundException;
import com.exito.stockai.model.modelos.EjecucionModelo;
import com.exito.stockai.model.modelos.ModeloMatematico;
import com.exito.stockai.model.modelos.enums.EstadoModelo;
import com.exito.stockai.repository.EjecucionModeloRepository;
import com.exito.stockai.repository.MovimientoInventarioRepository;
import com.exito.stockai.repository.ModeloMatematicoRepository;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
public class EjecucionModeloService {

    private final ModeloMatematicoRepository modeloRepository;
    private final EjecucionModeloRepository ejecucionRepository;
    private final MovimientoInventarioRepository movimientoRepository;
    private final MotorStockFlowUht motorStockFlow;
    private final ObjectMapper objectMapper;
    private final Map<String, MotorMatematico> motores;

    public EjecucionModeloService(ModeloMatematicoRepository modeloRepository,
            EjecucionModeloRepository ejecucionRepository,
            MovimientoInventarioRepository movimientoRepository,
            MotorStockFlowUht motorStockFlow,
            ObjectMapper objectMapper,
            List<MotorMatematico> motores) {
        this.modeloRepository = modeloRepository;
        this.ejecucionRepository = ejecucionRepository;
        this.movimientoRepository = movimientoRepository;
        this.motorStockFlow = motorStockFlow;
        this.objectMapper = objectMapper;
        this.motores = motores.stream()
                .collect(Collectors.toMap(MotorMatematico::nombreMotor, m -> m, (a, b) -> a));
    }

    @Transactional
    public EjecutarModeloResponse ejecutar(Long modeloId, EjecutarModeloRequest req) {
        ModeloMatematico modelo = modeloRepository.findById(modeloId)
                .orElseThrow(() -> new ResourceNotFoundException("Modelo no encontrado: " + modeloId));

        if (modelo.getEstado() == EstadoModelo.PROPUESTO
                || modelo.getMotorImpl() == null
                || modelo.getMotorImpl().isBlank()) {
            throw new ConflictoException("El modelo " + modelo.getCodigo() + " (" + modelo.getNombre()
                    + ") está en estado PROPUESTO y no tiene motor de cálculo implementado. "
                    + "Solo son ejecutables los modelos en estado PILOTO o IMPLEMENTADO.");
        }

        MotorMatematico motor = motores.get(modelo.getMotorImpl());
        if (motor == null) {
            throw new ConflictoException("No hay implementación registrada para el motor " + modelo.getMotorImpl());
        }

        String fuente = req.fuente() == null || req.fuente().isBlank()
                ? "SIMULADA" : req.fuente().trim().toUpperCase();
        if (!List.of("REAL", "SIMULADA").contains(fuente)) {
            throw new BadRequestException("La fuente debe ser REAL o SIMULADA");
        }

        Map<String, Object> params = new LinkedHashMap<>();
        if (req.parametros() != null) {
            params.putAll(req.parametros());
        }

        if (fuente.equals("REAL") && motor instanceof MotorStockFlowUht
                && !params.containsKey("ventasSemana")) {
            params.put("ventasSemana", ventasRealesUltimos14Dias());
        }
        if (fuente.equals("REAL") && motor.usaDatosReales()
                && !params.containsKey("serie")) {
            params.put("serie", ventasRealesUltimos14Dias());
        }

        ResultadoMotor res = motor.ejecutar(params);

        String parametrosJson = toJson(params);
        String resultadoJson = toJson(res.resultado());

        EjecucionModelo ejecucion = EjecucionModelo.builder()
                .modelo(modelo)
                .modeloCodigo(modelo.getCodigo())
                .parametros(parametrosJson)
                .resultado(resultadoJson)
                .estado("OK")
                .mensaje(res.mensaje())
                .build();
        ejecucion = ejecucionRepository.save(ejecucion);

        return new EjecutarModeloResponse(ejecucion.getId(), modelo.getId(), modelo.getCodigo(),
                modelo.getNombre(), modelo.getMotorImpl(), fuente, params, res.resultado(),
                res.serie(), res.interpretacion(), "OK", ejecucion.getCreadaEn());
    }

    @Transactional(readOnly = true)
    public List<EjecutarModeloResponse> listar() {
        return ejecucionRepository.findAllByOrderByCreadaEnDesc().stream()
                .map(e -> new EjecutarModeloResponse(
                        e.getId(),
                        e.getModelo() != null ? e.getModelo().getId() : null,
                        e.getModeloCodigo(),
                        e.getModelo() != null ? e.getModelo().getNombre() : null,
                        e.getModelo() != null ? e.getModelo().getMotorImpl() : null,
                        null,
                        fromJson(e.getParametros()),
                        fromJson(e.getResultado()),
                        List.of(),
                        null,
                        e.getEstado(),
                        e.getCreadaEn()))
                .toList();
    }

    private List<Double> ventasRealesUltimos14Dias() {
        LocalDate hasta = LocalDate.now();
        LocalDate desde = hasta.minusDays(13);
        Map<LocalDate, Long> porDia = movimientoRepository.flujoPorDia(null, desde, hasta).stream()
                .collect(Collectors.toMap(
                        MovimientoInventarioRepository.DiaFlujo::getFecha,
                        MovimientoInventarioRepository.DiaFlujo::getSalidas,
                        (a, b) -> a,
                        LinkedHashMap::new));
        if (porDia.values().stream().allMatch(v -> v == 0L)) {
            return List.of(8d, 12d, 15d, 10d, 14d, 18d, 9d);
        }
        List<Double> serie = new java.util.ArrayList<>();
        for (LocalDate d = desde; !d.isAfter(hasta); d = d.plusDays(1)) {
            serie.add(porDia.getOrDefault(d, 0L).doubleValue());
        }
        return serie;
    }

    private String toJson(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (Exception e) {
            throw new BadRequestException("No se pudo serializar la ejecución: " + e.getMessage());
        }
    }

    private Map<String, Object> fromJson(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            return Map.of();
        }
    }
}