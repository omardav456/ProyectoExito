package com.exito.stockai.model.modelos;

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
@Table(name = "parametros_modelo")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParametroModelo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "modelo_id", nullable = false)
    private ModeloMatematico modelo;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(length = 30)
    private String simbolo;

    @Column(name = "valor_por_defecto", length = 60)
    private String valorPorDefecto;

    @Column(length = 40)
    private String unidad;

    @Column(columnDefinition = "text")
    private String descripcion;
}