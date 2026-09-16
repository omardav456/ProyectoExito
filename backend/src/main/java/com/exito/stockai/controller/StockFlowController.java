package com.exito.stockai.controller;

import com.exito.stockai.dto.StockFlowResponse;
import com.exito.stockai.service.inventario.StockFlowService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stockflow")
public class StockFlowController {

    private final StockFlowService service;

    public StockFlowController(StockFlowService service) {
        this.service = service;
    }

    @GetMapping("/flujo")
    public ResponseEntity<StockFlowResponse> calcularFlujo(
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(service.calcularFlujo(categoriaId, desde, hasta));
    }
}