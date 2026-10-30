package com.panificadora.sapi.catalogo.infrastructure.persistence;

import com.panificadora.sapi.catalogo.domain.model.Producto;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Adaptador de salida (Spring Data JPA) del módulo Catálogo.
 *
 * <p>{@code @EntityGraph} trae la categoría en la misma consulta: evita el
 * LazyInitializationException al renderizar Thymeleaf con {@code open-in-view: false}.</p>
 */
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    @EntityGraph(attributePaths = "categoria")
    List<Producto> findByActivoTrueOrderByNombreAsc();

    @EntityGraph(attributePaths = "categoria")
    List<Producto> findByActivoTrueAndNombreContainingIgnoreCaseOrderByNombreAsc(String nombre);

    @EntityGraph(attributePaths = "categoria")
    List<Producto> findAllByOrderByNombreAsc();

    @EntityGraph(attributePaths = "categoria")
    Optional<Producto> findByIdAndActivoTrue(Long id);

    boolean existsByCodigoIgnoreCase(String codigo);

    boolean existsByCodigoIgnoreCaseAndIdNot(String codigo, Long id);

    @EntityGraph(attributePaths = "categoria")
    List<Producto> findByCategoriaIdAndActivoTrue(Long categoriaId);
}
