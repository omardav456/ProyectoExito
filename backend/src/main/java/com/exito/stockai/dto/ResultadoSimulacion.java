package com.exito.stockai.dto;

import java.util.List;
import java.util.Map;

public record ResultadoSimulacion(
    Integer inventarioInicial,
    Integer reposicion,
    List<Integer> ventasSemana,
    Integer dias,
    List<Fila> filas,
    Integer inventarioFinal,
    Integer ventasTotales,
    Integer reposicionTotal,
    Integer balance,
    Boolean balanceOk,
    Integer inventarioMin,
    Integer diaMin,
    Integer diaAgotamiento,
    Double ventaPromedio,
    Double neteDiarioPromedio
) {
    public record Fila(
        Integer dia,
        String diaSemana,
        Integer inventarioInicial,
        Integer reposicion,
        Integer venta,
        Integer inventarioFinal
    ) {}
}
