package com.exito.stockai.controller;

import com.exito.stockai.dto.AlertaRequest;
import com.exito.stockai.dto.AlertaResponse;
import com.exito.stockai.exception.BadRequestException;
import com.exito.stockai.exception.ResourceNotFoundException;
import com.exito.stockai.service.inventario.AlertaService;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alertas")
public class AlertaController {

    private final AlertaService service;

    public AlertaController(AlertaService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<AlertaResponse>> buscar(@RequestParam(required = false) String estado,
                                                       @RequestParam(required = false) String tipo) {
        try {
            return ResponseEntity.ok(service.buscar(estado, tipo));
        } catch (IllegalArgumentException e) {
            throw mapear(e);
        }
    }

    @GetMapping("/contar-activas")
    public ResponseEntity<Map<String, Long>> contarActivas() {
        var counts = service.contarActivasPorTipo();
        return ResponseEntity.ok(Map.of(
                "critico", counts[0],
                "advertencia", counts[1],
                "info", counts[2]));
    }

    @PostMapping
    public ResponseEntity<AlertaResponse> crear(@RequestBody AlertaRequest req) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(req));
        } catch (IllegalArgumentException e) {
            throw mapear(e);
        }
    }

    @PatchMapping("/{id}/resolver")
    public ResponseEntity<AlertaResponse> resolver(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(service.resolver(id));
        } catch (IllegalArgumentException e) {
            throw mapear(e);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        try {
            service.eliminar(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            throw mapear(e);
        }
    }

    private RuntimeException mapear(IllegalArgumentException e) {
        String msg = e.getMessage();
        if (msg != null && msg.toLowerCase(Locale.ROOT).contains("no encontrad")) {
            return new ResourceNotFoundException(msg);
        }
        return new BadRequestException(msg);
    }
}