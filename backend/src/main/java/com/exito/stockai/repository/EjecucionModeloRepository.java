package com.exito.stockai.repository;

import com.exito.stockai.model.modelos.EjecucionModelo;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EjecucionModeloRepository extends JpaRepository<EjecucionModelo, Long> {

    List<EjecucionModelo> findByModeloIdOrderByCreadaEnDesc(Long modeloId);

    List<EjecucionModelo> findAllByOrderByCreadaEnDesc();
}