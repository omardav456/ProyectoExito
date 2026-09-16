package com.exito.stockai.controller;

import com.exito.stockai.dto.ResultadoSimulacion;
import com.exito.stockai.dto.SimulacionParamsRequest;
import com.exito.stockai.dto.SimulacionResponse;
import com.exito.stockai.exception.BadRequestException;
import com.exito.stockai.exception.ResourceNotFoundException;
import com.exito.stockai.service.inventario.SimulacionService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/simulaciones")
public class SimulacionController {

    private final SimulacionService service;

    public SimulacionController(SimulacionService service) {
        this.service = service;
    }

    @PostMapping("/ejecutar")
    public ResponseEntity<ResultadoSimulacion> ejecutar(@Valid @RequestBody SimulacionParamsRequest req) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(service.ejecutar(req));
        } catch (IllegalArgumentException e) {
            throw mapear(e);
        }
    }

    @GetMapping
    public ResponseEntity<List<SimulacionResponse>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SimulacionResponse> buscarPorId(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(service.buscarPorId(id));
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