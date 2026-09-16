package com.exito.stockai.controller;

import com.exito.stockai.dto.DashboardSummary;
import com.exito.stockai.service.inventario.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummary> resumen() {
        return ResponseEntity.ok(service.resumen());
    }
}