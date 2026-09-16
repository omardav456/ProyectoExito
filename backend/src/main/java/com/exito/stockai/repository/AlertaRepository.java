package com.exito.stockai.repository;

import com.exito.stockai.model.inventario.Alerta;
import com.exito.stockai.model.inventario.enums.EstadoAlerta;
import com.exito.stockai.model.inventario.enums.TipoAlerta;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AlertaRepository extends JpaRepository<Alerta, Long> {

    List<Alerta> findByEstadoAndTipoOrderByHoraDesc(EstadoAlerta estado, TipoAlerta tipo);

    List<Alerta> findByEstadoOrderByHoraDesc(EstadoAlerta estado);

    List<Alerta> findAllByOrderByHoraDesc();

    @Query("""
        SELECT a.tipo, COUNT(a)
        FROM Alerta a WHERE a.estado = com.exito.stockai.model.inventario.enums.EstadoAlerta.ACTIVA
        GROUP BY a.tipo
        """)
    List<Object[]> contarActivasPorTipo();
}