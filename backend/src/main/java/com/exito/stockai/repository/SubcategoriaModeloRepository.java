package com.exito.stockai.repository;

import com.exito.stockai.model.modelos.SubcategoriaModelo;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubcategoriaModeloRepository extends JpaRepository<SubcategoriaModelo, Long> {

    List<SubcategoriaModelo> findByCategoriaIdOrderByNombreAsc(Long categoriaId);
}