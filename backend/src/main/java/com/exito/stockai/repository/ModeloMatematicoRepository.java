package com.exito.stockai.repository;

import com.exito.stockai.model.modelos.ModeloMatematico;
import com.exito.stockai.model.modelos.enums.ComplejidadModelo;
import com.exito.stockai.model.modelos.enums.EstadoModelo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ModeloMatematicoRepository extends JpaRepository<ModeloMatematico, Long> {

    Optional<ModeloMatematico> findByCodigo(String codigo);

    @Query("""
        SELECT DISTINCT m FROM ModeloMatematico m
        JOIN FETCH m.subcategoria s JOIN FETCH s.categoria c JOIN FETCH c.area a
        LEFT JOIN FETCH m.metodos
        WHERE (:areaId       IS NULL OR a.id  = :areaId)
          AND (:categoriaId  IS NULL OR c.id  = :categoriaId)
          AND (:subcategoriaId IS NULL OR s.id = :subcategoriaId)
          AND (:tipo IS NULL OR m.tipoModelo = :tipo)
          AND (:complejidad IS NULL OR m.complejidad = :complejidad)
          AND (:estado IS NULL OR m.estado = :estado)
          AND (:q IS NULL
               OR LOWER(m.nombre) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))
               OR LOWER(m.descripcion) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%')))
        ORDER BY m.codigo
        """)
    Page<ModeloMatematico> buscar(@Param("areaId") Long areaId,
                                  @Param("categoriaId") Long categoriaId,
                                  @Param("subcategoriaId") Long subcategoriaId,
                                  @Param("tipo") String tipo,
                                  @Param("complejidad") ComplejidadModelo complejidad,
                                  @Param("estado") EstadoModelo estado,
                                  @Param("q") String q,
                                  Pageable pageable);

    @Query("""
        SELECT COUNT(DISTINCT m) FROM ModeloMatematico m
        JOIN m.subcategoria s JOIN s.categoria c JOIN c.area a
        WHERE (:areaId       IS NULL OR a.id  = :areaId)
          AND (:categoriaId  IS NULL OR c.id  = :categoriaId)
          AND (:subcategoriaId IS NULL OR s.id = :subcategoriaId)
          AND (:tipo IS NULL OR m.tipoModelo = :tipo)
          AND (:complejidad IS NULL OR m.complejidad = :complejidad)
          AND (:estado IS NULL OR m.estado = :estado)
          AND (:q IS NULL
               OR LOWER(m.nombre) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))
               OR LOWER(m.descripcion) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%')))
        """)
    long contar(@Param("areaId") Long areaId,
                @Param("categoriaId") Long categoriaId,
                @Param("subcategoriaId") Long subcategoriaId,
                @Param("tipo") String tipo,
                @Param("complejidad") ComplejidadModelo complejidad,
                @Param("estado") EstadoModelo estado,
                @Param("q") String q);

    @Query("""
        SELECT m.tipoModelo, COUNT(m) FROM ModeloMatematico m
        WHERE m.tipoModelo IS NOT NULL
        GROUP BY m.tipoModelo
        ORDER BY COUNT(m) DESC
        """)
    List<Object[]> contarPorTipo();

    @Query("""
        SELECT m.estado, COUNT(m) FROM ModeloMatematico m
        GROUP BY m.estado ORDER BY m.estado
        """)
    List<Object[]> contarPorEstado();
}