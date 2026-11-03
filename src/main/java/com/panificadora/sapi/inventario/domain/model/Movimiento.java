package com.panificadora.sapi.inventario.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Cabecera de un movimiento de inventario.
 *
 * <p>El ingreso de un lote se registra siempre dentro de una sola transacción:
 * primero esta cabecera y luego su detalle en {@link Lote}.</p>
 */
@Entity
@Table(name = "MOVIMIENTO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Movimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "movimiento_seq")
    @SequenceGenerator(name = "movimiento_seq", sequenceName = "INV_MOVIMIENTO_SEQ", allocationSize = 1)
    @Column(name = "ID_MOVIMIENTO")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "TIPO_MOVIMIENTO", nullable = false, length = 20)
    private TipoMovimiento tipoMovimiento;

    @Column(name = "FECHA_MOVIMIENTO", nullable = false, updatable = false)
    private LocalDateTime fechaMovimiento;

    /** Identificador del operario que registró el movimiento. */
    @Column(name = "ID_PERSONAL")
    private Long idPersonal;

    @Column(name = "OBSERVACION", length = 250)
    private String observacion;

    @PrePersist
    void alPersistir() {
        if (this.fechaMovimiento == null) {
            this.fechaMovimiento = LocalDateTime.now();
        }
        if (this.tipoMovimiento == null) {
            this.tipoMovimiento = TipoMovimiento.INGRESO;
        }
    }
}
