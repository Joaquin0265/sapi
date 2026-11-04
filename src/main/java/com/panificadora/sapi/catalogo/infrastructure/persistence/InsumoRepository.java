package com.panificadora.sapi.catalogo.infrastructure.persistence;

import com.panificadora.sapi.catalogo.domain.model.Insumo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Adaptador de salida (Spring Data JPA) de insumos.
 *
 * <p>El {@code @EntityGraph} trae categoría y umbral en la misma consulta: la
 * vista necesita ambos datos y el proyecto trabaja con
 * {@code spring.jpa.open-in-view: false}.</p>
 */
public interface InsumoRepository extends JpaRepository<Insumo, Long> {

    @EntityGraph(attributePaths = {"categoria", "umbral"})
    List<Insumo> findByActivoTrueOrderByNombreAsc();

    @EntityGraph(attributePaths = {"categoria", "umbral"})
    List<Insumo> findByActivoTrueAndNombreContainingIgnoreCaseOrderByNombreAsc(String nombre);

    @EntityGraph(attributePaths = {"categoria", "umbral"})
    Optional<Insumo> findByIdAndActivoTrue(Long id);

    Optional<Insumo> findByCodigoIgnoreCase(String codigo);

    boolean existsByCodigoIgnoreCase(String codigo);
}
