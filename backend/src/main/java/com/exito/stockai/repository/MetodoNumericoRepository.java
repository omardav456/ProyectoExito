package com.exito.stockai.repository;

import com.exito.stockai.model.modelos.MetodoNumerico;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MetodoNumericoRepository extends JpaRepository<MetodoNumerico, Long> {

    List<MetodoNumerico> findAllByOrderByNombreAsc();
}