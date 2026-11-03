package com.panificadora.sapi.inventario.infrastructure.persistence;

import com.panificadora.sapi.inventario.domain.model.Lote;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Adaptador de salida del módulo Inventario.
 *
 * <p>Las consultas ordenan por {@code FECHA_VENCIMIENTO} ascendente para
 * materializar la regla FIFO (RN-01) usando el índice {@code IDX_LOTE_FIFO}.</p>
 */
public interface LoteRepository extends JpaRepository<Lote, Long> {

    @EntityGraph(attributePaths = {"insumo", "movimiento"})
    @Query("""
            select l from Lote l
            where l.insumo.id = :idInsumo
              and l.cantidadDisponible > 0
            order by l.fechaVencimiento asc, l.id asc
            """)
    List<Lote> findDisponiblesPorInsumo(@Param("idInsumo") Long idInsumo);

    @EntityGraph(attributePaths = {"insumo", "movimiento"})
    @Query("""
            select l from Lote l
            where l.cantidadDisponible > 0
            order by l.fechaVencimiento asc, l.id asc
            """)
    List<Lote> findDisponibles();

    @EntityGraph(attributePaths = {"insumo", "movimiento"})
    @Query("""
            select l from Lote l
            where l.cantidadDisponible > 0
              and l.fechaVencimiento <= :limite
            order by l.fechaVencimiento asc, l.id asc
            """)
    List<Lote> findDisponiblesHasta(@Param("limite") LocalDate limite);

    @EntityGraph(attributePaths = {"insumo", "movimiento"})
    List<Lote> findByMovimientoId(Long idMovimiento);
}
