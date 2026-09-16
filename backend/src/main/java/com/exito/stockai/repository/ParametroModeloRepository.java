package com.exito.stockai.repository;

import com.exito.stockai.model.modelos.ParametroModelo;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParametroModeloRepository extends JpaRepository<ParametroModelo, Long> {

    List<ParametroModelo> findByModeloIdOrderByNombreAsc(Long modeloId);
}