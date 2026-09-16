package com.exito.stockai.controller;

import com.exito.stockai.dto.DashboardSummary.DiaDemanda;
import com.exito.stockai.service.inventario.PrediccionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/prediccion")
public class PrediccionController {

    private final PrediccionService service;

    public PrediccionController(PrediccionService service) {
        this.service = service;
    }

    @GetMapping("/semanal")
    public ResponseEntity<DiaDemanda[]> prediccionSemanal(@RequestParam(required = false) Long productoId) {
        return ResponseEntity.ok(service.prediccionSemanal(productoId));
    }
}