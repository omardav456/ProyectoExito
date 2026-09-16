package com.exito.stockai.repository;

import com.exito.stockai.model.auditoria.Auditoria;
import com.exito.stockai.model.auditoria.OperacionAuditoria;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {

    List<Auditoria> findAllByOrderByFechaHoraDesc();

    List<Auditoria> findByUsuarioIdOrderByFechaHoraDesc(Long usuarioId);

    List<Auditoria> findByProductoIdOrderByFechaHoraDesc(Long productoId);

    List<Auditoria> findByOperacionOrderByFechaHoraDesc(OperacionAuditoria operacion);

    List<Auditoria> findByFechaBetweenOrderByFechaHoraDesc(LocalDate desde, LocalDate hasta);
}