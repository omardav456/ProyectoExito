package com.exito.stockai.engine;

import java.util.List;
import java.util.Map;

/**
 * Contrato de cualquier motor matemático conectable al catálogo.
 * Un modelo de catálogo se vuelve ejecutable cuando su columna motor_impl
 * apunta al nombre de una implementación registrada en el contexto Spring.
 */
public interface MotorMatematico {

    /** Identificador que debe coincidir con modelos_matematicos.motor_impl. */
    String nombreMotor();

    ResultadoMotor ejecutar(Map<String, Object> parametros);

    /**
     * Indica si el motor, al ejecutarse con fuente REAL, consume la serie de
     * movimientos reales del sistema (inyectada bajo la clave "serie").
     */
    default boolean usaDatosReales() {
        return false;
    }
}