package com.panificadora.sapi.catalogo.domain.model;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Insumo (materia prima) del catálogo de la panificadora.
 *
 * <p>Se separa de {@link Producto} a propósito: los productos son lo que se
 * vende y los insumos son lo que se consume en producción (harina, azúcar,
 * levadura...). El módulo de Alertas solo vigila insumos, porque son los que
 * pueden generar un quiebre de stock.</p>
 *
 * <p>El stock real <b>no</b> vive aquí: lo administra el módulo de Inventario.
 * En esta entidad solo se guarda la política de reposición
 * ({@link UmbralSeguridad}).</p>
 */
@Entity
@Table(
        name = "CAT_INSUMO",
        uniqueConstraints = @UniqueConstraint(name = "UK_CAT_INSUMO_CODIGO", columnNames = "CODIGO")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Insumo {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "insumo_seq")
    @SequenceGenerator(name = "insumo_seq", sequenceName = "CAT_INSUMO_SEQ", allocationSize = 1)
    @Column(name = "ID_INSUMO")
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
            foreignKey = @ForeignKey(name = "FK_CAT_INSUMO_CATEGORIA")
    )
    private Categoria categoria;

    @Enumerated(EnumType.STRING)
    @Column(name = "UNIDAD_MEDIDA", nullable = false, length = 12)
    private UnidadMedida unidadMedida;

    @Builder.Default
    @Column(name = "ACTIVO", nullable = false)
    private Boolean activo = Boolean.TRUE;

    @Column(name = "FECHA_REGISTRO", nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    @Column(name = "FECHA_ACTUALIZACION")
    private LocalDateTime fechaActualizacion;

    /**
     * Umbral de seguridad 1:1. Es el lado inverso de la relación: la FK vive en
     * {@code UMBRAL_SEGURIDAD.ID_INSUMO}.
     */
    @OneToOne(mappedBy = "insumo", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private UmbralSeguridad umbral;

    // ------------------------------------------------------------------
    // Comportamiento de dominio (sin Spring, sin HTTP)
    // ------------------------------------------------------------------

    public boolean estaActivo() {
        return Boolean.TRUE.equals(activo);
    }

    public boolean tieneUmbral() {
        return umbral != null;
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
