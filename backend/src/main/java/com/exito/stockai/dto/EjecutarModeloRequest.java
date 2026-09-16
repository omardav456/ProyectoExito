package com.exito.stockai.dto;

import java.util.Map;

public record EjecutarModeloRequest(
        Map<String, Object> parametros,
        String fuente
) {
}