package com.panificadora.sapi.despacho.domain;

import com.panificadora.sapi.catalogo.domain.model.Insumo;
import com.panificadora.sapi.inventario.domain.model.Movimiento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Detalle de una salida de insumo a producción (RF-03).
 *
 * <p>Cada fila cuelga de una cabecera {@link Movimiento} de tipo {@code EGRESO}
 * y referencia el insumo afectado. Es la fuente de los retiros históricos que
 * consume el motor de alertas para proyectar el consumo.</p>
 */
@Entity
@Table(
        name = "DESPACHO",
        indexes = @Index(name = "IX_DESPACHO_INSUMO_FECHA", columnList = "ID_INSUMO, FECHA_HORA")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Despacho {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "despacho_seq")
    @SequenceGenerator(name = "despacho_seq", sequenceName = "DSP_DESPACHO_SEQ", allocationSize = 1)
    @Column(name = "ID_DESPACHO")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "ID_MOVIMIENTO",
            nullable = false,
            foreignKey = @ForeignKey(name = "FK_DESPACHO_MOVIMIENTO")
    )
    private Movimiento movimiento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "ID_INSUMO",
            nullable = false,
            foreignKey = @ForeignKey(name = "FK_DESPACHO_INSUMO")
    )
    private Insumo insumo;

    @Column(name = "CANTIDAD", nullable = false, precision = 12, scale = 2)
    private BigDecimal cantidad;

    @Column(name = "FECHA_HORA", nullable = false, updatable = false)
    private LocalDateTime fechaHora;

    @PrePersist
    void alPersistir() {
        if (this.fechaHora == null) {
            this.fechaHora = LocalDateTime.now();
        }
    }
}
