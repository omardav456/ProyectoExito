package com.exito.stockai.dto;

import java.math.BigDecimal;

public record ProductoRequest(
    String nombre,
    Long categoriaId,
    Integer stockActual,
    Integer stockMinimo,
    BigDecimal precio,
    Integer tasaReposicion,
    String unidad,
    Boolean activo
) {}
