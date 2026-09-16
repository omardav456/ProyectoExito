package com.exito.stockai.model.auditoria;

/**
 * Tipo de operación registrado en la tabla de auditoría.
 * INMUTABLE: la auditoría no se edita ni se elimina.
 */
public enum OperacionAuditoria {
    ENTRADA,
    SALIDA,
    AJUSTE,
    ACTUALIZACION,
    COMPRA_SIMULADA
}