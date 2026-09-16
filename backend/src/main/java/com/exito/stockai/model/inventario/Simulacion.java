package com.exito.stockai.model.inventario;

import java.time.OffsetDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "simulaciones")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Simulacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "modelo_codigo", length = 40)
    private String modeloCodigo;

    private String descripcion;

    @Column(nullable = false, columnDefinition = "text")
    private String parametros;

    @Column(nullable = false, columnDefinition = "text")
    private String resultado;

    @Column(name = "creada_en", nullable = false, updatable = false)
    private OffsetDateTime creadaEn;

    @PrePersist
    void prePersist() {
        this.creadaEn = OffsetDateTime.now(java.time.ZoneOffset.UTC);
    }
}