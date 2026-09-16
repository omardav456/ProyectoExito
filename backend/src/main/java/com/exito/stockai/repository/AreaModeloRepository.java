package com.exito.stockai.repository;

import com.exito.stockai.model.modelos.AreaModelo;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AreaModeloRepository extends JpaRepository<AreaModelo, Long> {

    List<AreaModelo> findAllByOrderByNombreAsc();
}