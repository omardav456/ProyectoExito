package com.exito.stockai.repository;

import com.exito.stockai.model.compra.Compra;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompraRepository extends JpaRepository<Compra, Long> {

    List<Compra> findAllByOrderByFechaDesc();

    List<Compra> findByUsuarioIdOrderByFechaDesc(Long usuarioId);
}