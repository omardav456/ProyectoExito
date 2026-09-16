package com.exito.stockai.repository;

import com.exito.stockai.model.inventario.Recomendacion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecomendacionRepository extends JpaRepository<Recomendacion, Long> {

    List<Recomendacion> findByActivaTrueOrderByPrioridadDescFechaDesc();

    List<Recomendacion> findAllByOrderByPrioridadDescFechaDesc();
}