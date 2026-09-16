package com.exito.stockai.controller;

import com.exito.stockai.dto.EjecutarModeloRequest;
import com.exito.stockai.dto.EjecutarModeloResponse;
import com.exito.stockai.dto.ModeloDetailResponse;
import com.exito.stockai.dto.ModeloListResponse;
import com.exito.stockai.dto.TaxonomiaResponse;
import com.exito.stockai.exception.BadRequestException;
import com.exito.stockai.exception.ResourceNotFoundException;
import com.exito.stockai.service.modelos.EjecucionModeloService;
import com.exito.stockai.service.modelos.ModeloMatematicoService;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/modelos")
public class ModeloMatematicoController {

    private final ModeloMatematicoService service;
    private final EjecucionModeloService ejecucionService;

    public ModeloMatematicoController(ModeloMatematicoService service,
            EjecucionModeloService ejecucionService) {
        this.service = service;
        this.ejecucionService = ejecucionService;
    }

    @GetMapping
    public ResponseEntity<ModeloListResponse> buscar(
            @RequestParam(required = false) Long areaId,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) Long subcategoriaId,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String complejidad,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String q,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        try {
            return ResponseEntity.ok(service.buscar(areaId, categoriaId, subcategoriaId,
                    tipo, complejidad, estado, q, page, size));
        } catch (IllegalArgumentException e) {
            throw mapear(e);
        }
    }

    @GetMapping("/taxonomia")
    public ResponseEntity<TaxonomiaResponse> taxonomia() {
        return ResponseEntity.ok(service.taxonomia());
    }

    @GetMapping("/estadisticas")
    public ResponseEntity<Map<String, Object>> estadisticas() {
        return ResponseEntity.ok(service.estadisticas());
    }

    @GetMapping("/codigo/{codigo}")
    public ResponseEntity<ModeloDetailResponse> buscarPorCodigo(@PathVariable String codigo) {
        try {
            return ResponseEntity.ok(service.buscarPorCodigo(codigo));
        } catch (IllegalArgumentException e) {
            throw mapear(e);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ModeloDetailResponse> buscarPorId(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(service.buscarPorId(id));
        } catch (IllegalArgumentException e) {
            throw mapear(e);
        }
    }

    @GetMapping("/ejecuciones")
    public ResponseEntity<List<EjecutarModeloResponse>> ejecuciones() {
        return ResponseEntity.ok(ejecucionService.listar());
    }

    @PostMapping("/{id}/ejecutar")
    public ResponseEntity<EjecutarModeloResponse> ejecutar(@PathVariable Long id,
            @RequestBody EjecutarModeloRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ejecucionService.ejecutar(id, request));
    }

    private RuntimeException mapear(IllegalArgumentException e) {
        String msg = e.getMessage();
        if (msg != null && msg.toLowerCase(Locale.ROOT).contains("no encontrad")) {
            return new ResourceNotFoundException(msg);
        }
        return new BadRequestException(msg);
    }
}