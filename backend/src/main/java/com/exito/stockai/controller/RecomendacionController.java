package com.exito.stockai.controller;

import com.exito.stockai.dto.RecomendacionRequest;
import com.exito.stockai.dto.RecomendacionResponse;
import com.exito.stockai.exception.BadRequestException;
import com.exito.stockai.exception.ResourceNotFoundException;
import com.exito.stockai.model.inventario.Recomendacion;
import com.exito.stockai.repository.RecomendacionRepository;
import com.exito.stockai.service.inventario.RecomendacionService;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
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
@RequestMapping("/api/recomendaciones")
public class RecomendacionController {

    private final RecomendacionService service;
    private final RecomendacionRepository recomendacionRepository;

    public RecomendacionController(RecomendacionService service,
                                   RecomendacionRepository recomendacionRepository) {
        this.service = service;
        this.recomendacionRepository = recomendacionRepository;
    }

    @GetMapping
    public ResponseEntity<List<RecomendacionResponse>> listar(@RequestParam(required = false) Boolean activa) {
        if (Boolean.TRUE.equals(activa)) {
            return ResponseEntity.ok(service.listar(true));
        }
        return ResponseEntity.ok(service.listar(false));
    }

    @PostMapping
    public ResponseEntity<RecomendacionResponse> crear(@RequestBody RecomendacionRequest req) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(req));
        } catch (IllegalArgumentException e) {
            throw mapear(e);
        }
    }

    @Transactional
    @PatchMapping("/{id}")
    public ResponseEntity<RecomendacionResponse> toggle(@PathVariable Long id) {
        var rec = recomendacionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recomendación no encontrada: " + id));
        rec.setActiva(!Boolean.TRUE.equals(rec.getActiva()));
        recomendacionRepository.save(rec);
        return ResponseEntity.ok(toResponse(rec));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        recomendacionRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    private RecomendacionResponse toResponse(Recomendacion r) {
        return new RecomendacionResponse(
                r.getId(),
                r.getProducto() != null ? r.getProducto().getId() : null,
                r.getProducto() != null ? r.getProducto().getNombre() : null,
                r.getTipo(),
                r.getTitulo(),
                r.getDescripcion(),
                r.getAccionSugerida(),
                r.getPrioridad(),
                r.getFecha(),
                r.getActiva());
    }

    private RuntimeException mapear(IllegalArgumentException e) {
        String msg = e.getMessage();
        if (msg != null && msg.toLowerCase(Locale.ROOT).contains("no encontrad")) {
            return new ResourceNotFoundException(msg);
        }
        return new BadRequestException(msg);
    }
}