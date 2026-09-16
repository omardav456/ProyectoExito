package com.exito.stockai.repository;

import com.exito.stockai.model.modelos.VariableModelo;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VariableModeloRepository extends JpaRepository<VariableModelo, Long> {

    List<VariableModelo> findByModeloIdOrderByNombreAsc(Long modeloId);
}