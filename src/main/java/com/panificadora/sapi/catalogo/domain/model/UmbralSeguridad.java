package com.panificadora.sapi.catalogo.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
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
import org.hibernate.annotations.Check;

/**
 * Política de reposición de un insumo (tabla {@code UMBRAL_SEGURIDAD}).
 *
 * <p>Es la entidad que consume el módulo de Alertas para decidir cuándo avisar:</p>
 * <ul>
 *   <li>{@code stockMinimo}: punto de reorden. Si el stock real baja de aquí,
 *       se sugiere reponer.</li>
 *   <li>{@code stockCritico}: nivel de quiebre inminente. Debe ser
 *       <b>estrictamente menor</b> que {@code stockMinimo}.</li>
 * </ul>
 *
 * <p>La regla se protege en dos capas: en código
 * ({@code CatalogoService#registrarUmbral}) y en la base de datos con la
 * restricción {@code CHK_UMBRALES}. La anotación {@link Check} es la que
 * replica esa restricción cuando Hibernate genera el esquema (perfil demo/H2);
 * en Oracle la crea el script {@code db/03_schema_insumos_oracle.sql}.</p>
 */
@Entity
@Table(
        name = "UMBRAL_SEGURIDAD",
        uniqueConstraints = @UniqueConstraint(name = "UK_UMBRAL_INSUMO", columnNames = "ID_INSUMO")
)
@Check(name = "CHK_UMBRALES", constraints = "STOCK_CRITICO < STOCK_MINIMO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UmbralSeguridad {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "umbral_seq")
    @SequenceGenerator(name = "umbral_seq", sequenceName = "UMBRAL_SEGURIDAD_SEQ", allocationSize = 1)
    @Column(name = "ID_UMBRAL")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "ID_INSUMO",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(name = "FK_UMBRAL_INSUMO")
    )
    private Insumo insumo;

    @Column(name = "STOCK_MINIMO", nullable = false, precision = 12, scale = 2)
    private BigDecimal stockMinimo;

    @Column(name = "STOCK_CRITICO", nullable = false, precision = 12, scale = 2)
    private BigDecimal stockCritico;

    @Column(name = "FECHA_ACTUALIZACION")
    private LocalDateTime fechaActualizacion;

    /** Regla de dominio: el nivel crítico siempre está por debajo del mínimo. */
    public boolean esRangoValido() {
        return stockMinimo != null
                && stockCritico != null
                && stockCritico.compareTo(stockMinimo) < 0;
    }

    @PrePersist
    @PreUpdate
    void actualizarFecha() {
        this.fechaActualizacion = LocalDateTime.now();
    }
}
