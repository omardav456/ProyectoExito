package com.exito.stockai.model.auditoria;

import com.exito.stockai.model.inventario.Producto;
import com.exito.stockai.model.security.Usuario;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Registro inmutable de cada modificación de inventario.
 * El usuario se toma del contexto de seguridad, nunca del cliente.
 */
@Entity
@Table(name = "auditoria")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(nullable = false, length = 40)
    private String rol;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id")
    private Producto producto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private OperacionAuditoria operacion;

    @Column(name = "stock_anterior")
    private Integer stockAnterior;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "stock_posterior")
    private Integer stockPosterior;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false)
    private LocalTime hora;

    @Column(name = "fecha_hora", nullable = false)
    private OffsetDateTime fechaHora;

    private String observacion;

    @PrePersist
    void prePersist() {
        this.fechaHora = OffsetDateTime.now(java.time.ZoneOffset.UTC);
        this.fecha = LocalDate.now();
        this.hora = LocalTime.now();
    }
}