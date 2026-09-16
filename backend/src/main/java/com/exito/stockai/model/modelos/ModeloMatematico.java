package com.exito.stockai.model.modelos;

import com.exito.stockai.model.modelos.enums.ComplejidadModelo;
import com.exito.stockai.model.modelos.enums.EstadoModelo;
import java.util.LinkedHashSet;
import java.util.Set;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "modelos_matematicos")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModeloMatematico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subcategoria_id", nullable = false)
    private SubcategoriaModelo subcategoria;

    @Column(nullable = false, unique = true, length = 20)
    private String codigo;

    @Column(nullable = false, length = 180)
    private String nombre;

    @Column(columnDefinition = "text")
    private String descripcion;

    @Column(columnDefinition = "text")
    private String problema;

    @Column(name = "tipo_modelo", length = 40)
    private String tipoModelo;

    @Column(name = "metodo_sugerido", length = 120)
    private String metodoSugerido;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ComplejidadModelo complejidad;

    @Column(name = "entrada_esperada", columnDefinition = "text")
    private String entradaEsperada;

    @Column(name = "salida_esperada", columnDefinition = "text")
    private String salidaEsperada;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoModelo estado;

    @Column(name = "motor_impl", length = 120)
    private String motorImpl;

    @Column(columnDefinition = "text")
    private String documentacion;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "modelo_metodo",
        joinColumns = @JoinColumn(name = "modelo_id"),
        inverseJoinColumns = @JoinColumn(name = "metodo_id"))
    @Builder.Default
    private Set<MetodoNumerico> metodos = new LinkedHashSet<>();
}