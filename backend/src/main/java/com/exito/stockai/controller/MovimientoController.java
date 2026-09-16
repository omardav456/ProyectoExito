package com.exito.stockai.controller;

import com.exito.stockai.dto.MovimientoRequest;
import com.exito.stockai.dto.MovimientoResponse;
import com.exito.stockai.exception.BadRequestException;
import com.exito.stockai.exception.ResourceNotFoundException;
import com.exito.stockai.service.inventario.MovimientoService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/movimientos")
public class MovimientoController {

    private final MovimientoService service;

    public MovimientoController(MovimientoService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<MovimientoResponse>> buscar(
            @RequestParam(required = false) Long productoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(service.buscar(productoId, desde, hasta));
    }

    @PostMapping
    public ResponseEntity<MovimientoResponse> crear(@Valid @RequestBody MovimientoRequest req) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(req));
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