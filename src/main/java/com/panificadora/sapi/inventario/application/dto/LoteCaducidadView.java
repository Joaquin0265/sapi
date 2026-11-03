package com.panificadora.sapi.inventario.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Fila del Reporte de Caducidad. Es un DTO de salida: la vista no necesita
 * navegar relaciones JPA ni calcular estados.
 */
@Getter
@AllArgsConstructor
public class LoteCaducidadView {

    private final Long idLote;
    private final String codigoLote;
    private final String codigoInsumo;
    private final String nombreInsumo;
    private final BigDecimal cantidadDisponible;
    private final LocalDate fechaVencimiento;
    private final long diasRestantes;
    private final String estado;
}
