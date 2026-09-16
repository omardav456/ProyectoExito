package com.exito.stockai.dto;

import java.util.List;

public record TaxonomiaResponse(
    List<AreaDto> areas
) {
    public record AreaDto(Long id, String nombre, String descripcion, List<CategoriaDto> categorias) {}
    public record CategoriaDto(Long id, String nombre, String descripcion, List<SubcatDto> subcategorias) {}
    public record SubcatDto(Long id, String nombre, String descripcion) {}
}
