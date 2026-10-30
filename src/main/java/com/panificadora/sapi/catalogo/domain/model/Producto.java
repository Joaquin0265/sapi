package com.panificadora.sapi.catalogo.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidad JPA plana del catálogo.
 *
 * <p>{@code stockMinimo} es la <b>política de reposición</b> definida por el área de
 * almacén: el módulo de Inventario la usará para disparar las alertas preventivas
 * (módulo de Alertas). Catálogo solo la almacena; no consulta stock real.</p>
 */
@Entity
@Table(
        name = "CAT_PRODUCTO",
        uniqueConstraints = @UniqueConstraint(name = "UK_CAT_PRODUCTO_CODIGO", columnNames = "CODIGO")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "producto_seq")
    @SequenceGenerator(name = "producto_seq", sequenceName = "CAT_PRODUCTO_SEQ", allocationSize = 1)
    @Column(name = "ID_PRODUCTO")
    private Long id;

    /** Código interno / SKU. Se normaliza siempre en mayúsculas. */
    @Column(name = "CODIGO", nullable = false, length = 20)
    private String codigo;

    @Column(name = "NOMBRE", nullable = false, length = 120)
    private String nombre;

    @Column(name = "DESCRIPCION", length = 250)
    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "ID_CATEGORIA",
            nullable = false,
            foreignKey = @ForeignKey(name = "FK_CAT_PRODUCTO_CATEGORIA")
    )
    private Categoria categoria;

    @Enumerated(EnumType.STRING)
    @Column(name = "UNIDAD_MEDIDA", nullable = false, length = 12)
    private UnidadMedida unidadMedida;

    @Column(name = "PRECIO", nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    @Column(name = "STOCK_MINIMO", nullable = false, precision = 12, scale = 2)
    private BigDecimal stockMinimo;

    @Builder.Default
    @Column(name = "ACTIVO", nullable = false)
    private Boolean activo = Boolean.TRUE;

    @Column(name = "FECHA_REGISTRO", nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    @Column(name = "FECHA_ACTUALIZACION")
    private LocalDateTime fechaActualizacion;

    // ------------------------------------------------------------------
    // Comportamiento de dominio (sin Spring, sin HTTP)
    // ------------------------------------------------------------------

    /** Baja lógica: el producto conserva su historial para auditoría y trazabilidad. */
    public void desactivar() {
        this.activo = Boolean.FALSE;
        this.fechaActualizacion = LocalDateTime.now();
    }

    public void reactivar() {
        this.activo = Boolean.TRUE;
        this.fechaActualizacion = LocalDateTime.now();
    }

    public boolean estaActivo() {
        return Boolean.TRUE.equals(activo);
    }

    @PrePersist
    void alPersistir() {
        LocalDateTime ahora = LocalDateTime.now();
        this.fechaRegistro = ahora;
        this.fechaActualizacion = ahora;
        if (this.activo == null) {
            this.activo = Boolean.TRUE;
        }
    }

    @PreUpdate
    void alActualizar() {
        this.fechaActualizacion = LocalDateTime.now();
    }
}
