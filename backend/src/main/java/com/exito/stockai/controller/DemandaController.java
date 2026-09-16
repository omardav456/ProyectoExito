package com.exito.stockai.controller;

import com.exito.stockai.dto.DemandaRequest;
import com.exito.stockai.dto.DemandaResponse;
import com.exito.stockai.exception.BadRequestException;
import com.exito.stockai.exception.ResourceNotFoundException;
import com.exito.stockai.service.inventario.DemandaService;
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
@RequestMapping("/api/demanda")
public class DemandaController {

    private final DemandaService service;

    public DemandaController(DemandaService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<DemandaResponse>> buscar(
            @RequestParam(required = false) Long productoId,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        try {
            return ResponseEntity.ok(service.buscar(productoId, tipo, desde, hasta));
        } catch (IllegalArgumentException e) {
            throw mapear(e);
        }
    }

    @PostMapping
    public ResponseEntity<List<DemandaResponse>> crearLote(@RequestBody List<DemandaRequest> reqs) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(service.crearLote(reqs));
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