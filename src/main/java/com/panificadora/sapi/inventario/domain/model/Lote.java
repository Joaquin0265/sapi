package com.panificadora.sapi.inventario.domain.model;

import com.panificadora.sapi.catalogo.domain.model.Insumo;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Detalle de un movimiento de ingreso: un lote físico de un insumo.
 *
 * <p>La columna {@code FECHA_VENCIMIENTO} junto con {@code ID_INSUMO} está
 * cubierta por el índice {@code IDX_LOTE_FIFO}, que soporta el consumo FIFO
 * (RN-01: primero vence, primero sale).</p>
 */
@Entity
@Table(
        name = "LOTE",
        indexes = @Index(name = "IDX_LOTE_FIFO", columnList = "ID_INSUMO, FECHA_VENCIMIENTO")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lote {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "lote_seq")
    @SequenceGenerator(name = "lote_seq", sequenceName = "INV_LOTE_SEQ", allocationSize = 1)
    @Column(name = "ID_LOTE")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "ID_MOVIMIENTO",
            nullable = false,
            foreignKey = @ForeignKey(name = "FK_LOTE_MOVIMIENTO")
    )
    private Movimiento movimiento;

    /** Insumo del catálogo que se vigila con umbrales de seguridad (módulo Alertas). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "ID_INSUMO",
            nullable = false,
            foreignKey = @ForeignKey(name = "FK_LOTE_INSUMO")
    )
    private Insumo insumo;

    @Column(name = "CODIGO_LOTE", length = 40)
    private String codigoLote;

    @Column(name = "CANTIDAD", nullable = false, precision = 12, scale = 2)
    private BigDecimal cantidad;

    @Column(name = "CANTIDAD_DISPONIBLE", nullable = false, precision = 12, scale = 2)
    private BigDecimal cantidadDisponible;

    @Column(name = "COSTO_UNITARIO", precision = 12, scale = 2)
    private BigDecimal costoUnitario;

    @Column(name = "FECHA_INGRESO", nullable = false, updatable = false)
    private LocalDateTime fechaIngreso;

    @Column(name = "FECHA_VENCIMIENTO", nullable = false)
    private LocalDate fechaVencimiento;

    /**
     * Un lote es vigente cuando aún tiene saldo disponible y su fecha de
     * vencimiento no es anterior a la fecha de referencia.
     */
    public boolean estaVigente(LocalDate fechaReferencia) {
        return cantidadDisponible != null
                && cantidadDisponible.compareTo(BigDecimal.ZERO) > 0
                && fechaVencimiento != null
                && !fechaVencimiento.isBefore(fechaReferencia);
    }

    public long diasHastaVencimiento(LocalDate fechaReferencia) {
        return ChronoUnit.DAYS.between(fechaReferencia, fechaVencimiento);
    }

    @PrePersist
    void alPersistir() {
        if (this.fechaIngreso == null) {
            this.fechaIngreso = LocalDateTime.now();
        }
        if (this.cantidadDisponible == null) {
            this.cantidadDisponible = this.cantidad;
        }
    }
}
