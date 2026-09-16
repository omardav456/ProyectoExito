package com.exito.stockai.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public record EjecutarModeloResponse(
        Long id,
        Long modeloId,
        String modeloCodigo,
        String modeloNombre,
        String motor,
        String fuente,
        Map<String, Object> parametros,
        Map<String, Object> resultado,
        List<Map<String, Object>> serie,
        String interpretacion,
        String estado,
        OffsetDateTime creadaEn
) {
}