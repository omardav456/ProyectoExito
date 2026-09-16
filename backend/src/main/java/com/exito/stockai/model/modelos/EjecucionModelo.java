package com.exito.stockai.model.modelos;

import java.time.OffsetDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Entity
@Table(name = "ejecuciones_modelo")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EjecucionModelo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modelo_id")
    private ModeloMatematico modelo;

    @Column(name = "modelo_codigo", length = 20)
    private String modeloCodigo;

    @Column(nullable = false, columnDefinition = "text")
    private String parametros;

    @Column(columnDefinition = "text")
    private String resultado;

    @Column(nullable = false, length = 20)
    private String estado;

    private String mensaje;

    @Column(name = "creada_en", nullable = false, updatable = false)
    private OffsetDateTime creadaEn;

    @PrePersist
    void prePersist() {
        this.creadaEn = OffsetDateTime.now(java.time.ZoneOffset.UTC);
        if (this.estado == null) {
            this.estado = "OK";
        }
    }
}