package com.exito.stockai.dto;

import java.util.List;

public record DashboardSummary(
    Long totalProductos,
    Integer stockTotal,
    Long productosCriticos,
    Long productosBajos,
    Long productosSobrestock,
    Long totalAlertasActivas,
    Long totalRecomendacionesActivas,
    List<CategoriaStock> stockPorCategoria,
    List<DiaDemanda> demandaSemana
) {
    public record CategoriaStock(String nombre, Long stockTotal) {}
    public record DiaDemanda(String dia, Long unidades, Long forecast) {}
}
