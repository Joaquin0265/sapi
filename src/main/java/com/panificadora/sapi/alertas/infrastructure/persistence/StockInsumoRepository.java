package com.panificadora.sapi.alertas.infrastructure.persistence;

import com.panificadora.sapi.inventario.domain.model.Lote;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Consulta de stock real del módulo Alertas.
 *
 * <p>El stock de un insumo es la suma de {@code CANTIDAD_DISPONIBLE} de sus
 * lotes con saldo y no vencidos: un lote vencido no es stock utilizable.</p>
 */
public interface StockInsumoRepository extends Repository<Lote, Long> {

    @Query("""
            select l.insumo.id as idInsumo,
                   sum(l.cantidadDisponible) as stock
            from Lote l
            where l.cantidadDisponible > 0
              and l.fechaVencimiento >= :hoy
            group by l.insumo.id
            """)
    List<StockPorInsumo> stockVigentePorInsumo(@Param("hoy") LocalDate hoy);

    interface StockPorInsumo {
        Long getIdInsumo();

        BigDecimal getStock();
    }
}
