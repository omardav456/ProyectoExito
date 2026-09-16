package com.exito.stockai.model.inventario;

import com.exito.stockai.model.inventario.enums.EstadoAlerta;
import com.exito.stockai.model.inventario.enums.TipoAlerta;
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

@Entity
@Table(name = "alertas")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Alerta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id")
    private Producto producto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoAlerta tipo;

    @Column(nullable = false, length = 20)
    private String prioridad;

    @Column(nullable = false)
    private String mensaje;

    @Column(nullable = false)
    private OffsetDateTime hora;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoAlerta estado;

    @PrePersist
    void prePersist() {
        this.hora = OffsetDateTime.now(java.time.ZoneOffset.UTC);
        if (this.estado == null) {
            this.estado = EstadoAlerta.ACTIVA;
        }
        if (this.prioridad == null) {
            this.prioridad = "MEDIA";
        }
    }
}