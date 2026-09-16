package com.exito.stockai.model.inventario.enums;

/**
 * Estado calculado de un producto según su stock y su demanda.
 * No se persiste: se deriva de stock actual, stock mínimo y ventas promedio.
 */
public enum EstadoProducto {
    OK,
    BAJO,
    CRITICO,
    SOBRESTOCK,
    AGOTADO
}