package com.exito.stockai.engine;

import java.util.Map;

/**
 * Implementación genérica de {@link MotorMatematico} para los motores del
 * catálogo que se definen por fórmula (bean de cálculo por lambda).
 * El nombre del motor debe coincidir con modelos_matematicos.motor_impl.
 */
public final class MotorGenerico implements MotorMatematico {

    private final String nombre;
    private final boolean usaDatosReales;
    private final Calculo calculo;

    @FunctionalInterface
    public interface Calculo {
        ResultadoMotor aplicar(Map<String, Object> parametros, Calc c);
    }

    public MotorGenerico(String nombre, Calculo calculo) {
        this(nombre, false, calculo);
    }

    public MotorGenerico(String nombre, boolean usaDatosReales, Calculo calculo) {
        this.nombre = nombre;
        this.usaDatosReales = usaDatosReales;
        this.calculo = calculo;
    }

    @Override
    public String nombreMotor() {
        return nombre;
    }

    @Override
    public boolean usaDatosReales() {
        return usaDatosReales;
    }

    @Override
    public ResultadoMotor ejecutar(Map<String, Object> parametros) {
        return calculo.aplicar(parametros, new Calc(parametros));
    }
}