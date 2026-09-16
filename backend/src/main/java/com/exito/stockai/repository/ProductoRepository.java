package com.exito.stockai.repository;

import com.exito.stockai.model.inventario.Producto;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    @Query("""
        SELECT p FROM Producto p JOIN p.categoria c
        WHERE (:q IS NULL OR LOWER(p.nombre) LIKE LOWER(CONCAT('%', :q, '%')))
          AND (:categoriaId IS NULL OR c.id = :categoriaId)
          AND (:activo IS NULL OR p.activo = :activo)
        ORDER BY p.nombre
        """)
    List<Producto> buscar(@Param("q") String q,
                          @Param("categoriaId") Long categoriaId,
                          @Param("activo") Boolean activo);

    Optional<Producto> findByNombreIgnoreCase(String nombre);
}