package com.exito.stockai.exception;

/**
 * Cuenta creada pero aún no aprobada por un administrador (usuario inactivo).
 * Respuesta HTTP 403 con mensaje claro para la demo.
 */
public class CuentaPendienteException extends RuntimeException {

    public CuentaPendienteException(String message) {
        super(message);
    }
}