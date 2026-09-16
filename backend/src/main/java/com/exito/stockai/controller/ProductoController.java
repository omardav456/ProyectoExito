package com.exito.stockai.controller;

import com.exito.stockai.dto.ProductoRequest;
import com.exito.stockai.dto.ProductoResponse;
import com.exito.stockai.exception.BadRequestException;
import com.exito.stockai.exception.ResourceNotFoundException;
import com.exito.stockai.service.inventario.ProductoService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    private final ProductoService service;

    public ProductoController(ProductoService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<ProductoResponse>> buscar(@RequestParam(required = false) String q,
                                                         @RequestParam(required = false) Long categoriaId,
                                                         @RequestParam(required = false) String activo) {
        Boolean activoBool = activo != null ? Boolean.parseBoolean(activo) : null;
        return ResponseEntity.ok(service.buscar(q, categoriaId, activoBool));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductoResponse> buscarPorId(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(service.buscarPorId(id));
        } catch (IllegalArgumentException e) {
            throw mapear(e);
        }
    }

    @PostMapping
    public ResponseEntity<ProductoResponse> crear(@Valid @RequestBody ProductoRequest req) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(req));
        } catch (IllegalArgumentException e) {
            throw mapear(e);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductoResponse> actualizar(@PathVariable Long id,
                                                       @Valid @RequestBody ProductoRequest req) {
        try {
            return ResponseEntity.ok(service.actualizar(id, req));
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