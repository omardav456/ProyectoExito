package com.exito.stockai.dto;

import java.util.List;

public record SimulacionParamsRequest(
    Integer inventarioInicial,
    Integer reposicion,
    List<Integer> ventasSemana,
    Integer dias
) {}
