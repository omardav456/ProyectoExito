package com.exito.stockai.repository;

import com.exito.stockai.model.modelos.AplicacionModelo;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AplicacionModeloRepository extends JpaRepository<AplicacionModelo, Long> {

    List<AplicacionModelo> findByModeloIdOrderByDescripcionAsc(Long modeloId);
}