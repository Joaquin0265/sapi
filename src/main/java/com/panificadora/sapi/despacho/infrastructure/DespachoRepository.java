package com.panificadora.sapi.despacho.infrastructure;

import com.panificadora.sapi.despacho.domain.Despacho;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DespachoRepository extends JpaRepository<Despacho, Long> {

    @EntityGraph(attributePaths = {"insumo", "movimiento"})
    List<Despacho> findByInsumoIdOrderByFechaHoraDesc(Long idInsumo);
}
