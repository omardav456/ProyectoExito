package com.exito.stockai.dto;

import java.math.BigDecimal;

public record ProductoResponse(
    Long id,
    String nombre,
    Long categoriaId,
    String categoriaNombre,
    String categoriaColor,
    Integer stockActual,
    Integer stockMinimo,
    BigDecimal precio,
    Integer tasaReposicion,
    String unidad,
    Boolean activo,
    Double ventasPromedio,
    Integer demandaPrevista,
    Integer diasInventario,
    String estado,
    String accionRecomendada,
    String descripcion,
    String imagen
) {}
