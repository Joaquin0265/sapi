package com.panificadora.sapi.despacho.infrastructure;

import com.panificadora.sapi.despacho.domain.Despacho;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DespachoRepository extends JpaRepository<Despacho, Long> {
}