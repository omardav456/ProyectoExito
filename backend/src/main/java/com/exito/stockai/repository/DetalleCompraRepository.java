package com.exito.stockai.repository;

import com.exito.stockai.model.compra.DetalleCompra;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DetalleCompraRepository extends JpaRepository<DetalleCompra, Long> {

    List<DetalleCompra> findByCompraIdOrderByIdAsc(Long compraId);
}