package com.exito.stockai.dto;

import java.util.List;

public record ModeloDetailResponse(
    Long id,
    String codigo,
    String nombre,
    String descripcion,
    String problema,
    String tipoModelo,
    String metodoSugerido,
    String complejidad,
    String entradaEsperada,
    String salidaEsperada,
    String estado,
    String motorImpl,
    String documentacion,
    String area,
    String categoria,
    String subcategoria,
    List<VariableDto> variables,
    List<ParametroDto> parametros,
    List<String> metodos,
    List<String> aplicaciones
) {
    public record VariableDto(Long id, String nombre, String simbolo, String rol, String tipoDato, String unidad, String descripcion) {}
    public record ParametroDto(Long id, String nombre, String simbolo, String valorPorDefecto, String unidad, String descripcion) {}
}
