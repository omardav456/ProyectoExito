package com.exito.stockai.repository;

import com.exito.stockai.model.modelos.CategoriaModelo;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaModeloRepository extends JpaRepository<CategoriaModelo, Long> {

    List<CategoriaModelo> findByAreaIdOrderByNombreAsc(Long areaId);
}