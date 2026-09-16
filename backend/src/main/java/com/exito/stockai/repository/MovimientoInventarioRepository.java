package com.exito.stockai.repository;

import com.exito.stockai.model.inventario.MovimientoInventario;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Long> {

    List<MovimientoInventario> findByProductoIdAndFechaBetweenOrderByFechaAsc(Long productoId,
                                                                               LocalDate desde,
                                                                               LocalDate hasta);

    List<MovimientoInventario> findByFechaBetweenOrderByFechaAsc(LocalDate desde, LocalDate hasta);

    /**
     * Entradas y salidas agregadas por día para el flujo stock-flow.
     */
    @Query("""
        SELECT m.fecha AS fecha,
               COALESCE(SUM(CASE WHEN m.tipo = com.exito.stockai.model.inventario.enums.TipoMovimiento.ENTRADA
                                 THEN m.cantidad END), 0) AS entradas,
               COALESCE(SUM(CASE WHEN m.tipo = com.exito.stockai.model.inventario.enums.TipoMovimiento.SALIDA
                                 THEN m.cantidad END), 0) AS salidas
        FROM MovimientoInventario m
        WHERE m.fecha BETWEEN :desde AND :hasta
          AND (:productoId IS NULL OR m.producto.id = :productoId)
        GROUP BY m.fecha
        ORDER BY m.fecha
        """)
    List<DiaFlujo> flujoPorDia(@Param("productoId") Long productoId,
                               @Param("desde") LocalDate desde,
                               @Param("hasta") LocalDate hasta);

    interface DiaFlujo {
        LocalDate getFecha();
        Long getEntradas();
        Long getSalidas();
    }
}