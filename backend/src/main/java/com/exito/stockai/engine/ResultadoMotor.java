package com.exito.stockai.engine;

import java.util.List;
import java.util.Map;

public record ResultadoMotor(
        String mensaje,
        Map<String, Object> resultado,
        List<Map<String, Object>> serie,
        String interpretacion
) {

    public static ResultadoMotor of(String mensaje, Map<String, Object> resultado,
            List<Map<String, Object>> serie, String interpretacion) {
        return new ResultadoMotor(mensaje, resultado, serie, interpretacion);
    }
}