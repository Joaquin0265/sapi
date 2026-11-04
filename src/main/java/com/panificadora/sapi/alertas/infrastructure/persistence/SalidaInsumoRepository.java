package com.panificadora.sapi.alertas.infrastructure.persistence;

import com.panificadora.sapi.despacho.domain.Despacho;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Retiros históricos por insumo.
 *
 * <p>La ventana la decide el servicio (7 días para RN-04); el repositorio solo
 * agrega las salidas registradas por el módulo de Despacho.</p>
 */
public interface SalidaInsumoRepository extends Repository<Despacho, Long> {

    @Query("""
            select d.insumo.id as idInsumo,
                   sum(d.cantidad) as salidas
            from Despacho d
            where d.fechaHora >= :desde
            group by d.insumo.id
            """)
    List<SalidaPorInsumo> salidasDesde(@Param("desde") LocalDateTime desde);

    interface SalidaPorInsumo {
        Long getIdInsumo();

        BigDecimal getSalidas();
    }
}
