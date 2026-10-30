package com.panificadora.sapi.catalogo.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidad JPA plana: sin lógica de negocio, sin dependencias de Spring.
 * Agrupa los productos del catálogo (Panadería, Pastelería, Bebidas, ...).
 */
@Entity
@Table(
        name = "CAT_CATEGORIA",
        uniqueConstraints = @UniqueConstraint(name = "UK_CAT_CATEGORIA_NOMBRE", columnNames = "NOMBRE")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "categoria_seq")
    @SequenceGenerator(name = "categoria_seq", sequenceName = "CAT_CATEGORIA_SEQ", allocationSize = 1)
    @Column(name = "ID_CATEGORIA")
    private Long id;

    @Column(name = "NOMBRE", nullable = false, length = 80)
    private String nombre;

    @Column(name = "DESCRIPCION", length = 200)
    private String descripcion;

    /** En Oracle se persiste como NUMBER(1): 1 = activo, 0 = inactivo. */
    @Builder.Default
    @Column(name = "ACTIVO", nullable = false)
    private Boolean activo = Boolean.TRUE;

    public boolean estaActiva() {
        return Boolean.TRUE.equals(activo);
    }
}
