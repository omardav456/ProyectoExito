package com.exito.stockai.repository;

import com.exito.stockai.model.inventario.Simulacion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SimulacionRepository extends JpaRepository<Simulacion, Long> {

    List<Simulacion> findAllByOrderByCreadaEnDesc();

    List<Simulacion> findByModeloCodigoOrderByCreadaEnDesc(String modeloCodigo);
}