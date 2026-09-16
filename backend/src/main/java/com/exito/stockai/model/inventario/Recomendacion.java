package com.exito.stockai.model.inventario;

import java.time.LocalDate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "recomendaciones")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Recomendacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id")
    private Producto producto;

    @Column(nullable = false, length = 30)
    private String tipo;

    @Column(nullable = false, length = 160)
    private String titulo;

    @Column(nullable = false)
    private String descripcion;

    @Column(name = "accion_sugerida")
    private String accionSugerida;

    @Column(nullable = false, length = 20)
    private String prioridad;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false)
    private Boolean activa;
}