package com.panificadora.sapi.catalogo.infrastructure.persistence;

import com.panificadora.sapi.catalogo.domain.model.UmbralSeguridad;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Adaptador de salida del umbral de seguridad.
 *
 * <p>La relación con {@link com.panificadora.sapi.catalogo.domain.model.Insumo}
 * es 1:1 y la FK está en esta tabla, por eso se busca por {@code insumo.id}.</p>
 */
public interface UmbralSeguridadRepository extends JpaRepository<UmbralSeguridad, Long> {

    @EntityGraph(attributePaths = "insumo")
    Optional<UmbralSeguridad> findByInsumoId(Long idInsumo);

    boolean existsByInsumoId(Long idInsumo);
}
