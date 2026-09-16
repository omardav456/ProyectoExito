package com.exito.stockai.repository;

import com.exito.stockai.model.inventario.DemandaDiaria;
import com.exito.stockai.model.inventario.enums.TipoDemanda;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DemandaDiariaRepository extends JpaRepository<DemandaDiaria, Long> {

    List<DemandaDiaria> findByTipoAndFechaBetweenOrderByFechaAsc(TipoDemanda tipo, LocalDate desde, LocalDate hasta);

    List<DemandaDiaria> findByProductoIdAndTipoAndFechaBetweenOrderByFechaAsc(Long productoId,
                                                                               TipoDemanda tipo,
                                                                               LocalDate desde,
                                                                               LocalDate hasta);

    List<DemandaDiaria> findByProductoIdAndTipoOrderByFechaDesc(Long productoId, TipoDemanda tipo,
                                                                org.springframework.data.domain.Pageable pageable);

    /**
     * Demanda total (o por producto) agregada por día.
     */
    @Query("""
        SELECT d.fecha AS fecha, SUM(d.unidades) AS unidades
        FROM DemandaDiaria d
        WHERE d.tipo = :tipo AND d.fecha BETWEEN :desde AND :hasta
          AND (:productoId IS NULL OR d.producto.id = :productoId)
        GROUP BY d.fecha
        ORDER BY d.fecha
        """)
    List<DiaDemanda> sumPorDia(@Param("productoId") Long productoId,
                               @Param("tipo") TipoDemanda tipo,
                               @Param("desde") LocalDate desde,
                               @Param("hasta") LocalDate hasta);

    interface DiaDemanda {
        LocalDate getFecha();
        Long getUnidades();
    }
}