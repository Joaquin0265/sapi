package com.panificadora.sapi.alertas.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Prueba unitaria de RN-04 con el insumo <b>LEC-001 / Leche en polvo</b>.
 *
 * <p>Escenario: stock actual = 10 kg, umbral mínimo = 15 kg y <b>cero salidas
 * en los últimos 7 días</b>. El algoritmo de sugerencia de compra debe omitir
 * el cálculo histórico y devolver la reposición manual estándar.</p>
 */
class SugerenciaCompraLecheEnPolvoTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 4);
    private static final BigDecimal STOCK_ACTUAL = new BigDecimal("10");
    private static final BigDecimal UMBRAL_MINIMO = new BigDecimal("15");
    private static final BigDecimal SIN_SALIDAS = BigDecimal.ZERO;

    @Test
    @DisplayName("LEC-001 sin salidas en 7 días omite el histórico y sugiere 5 kg manuales")
    void sinSalidasEnSieteDiasOmiteElCalculoHistorico() {
        SugerenciaCompra sugerencia = SugerenciaCompra.calcular(
                STOCK_ACTUAL,
                UMBRAL_MINIMO,
                SIN_SALIDAS,
                SugerenciaCompra.DIAS_VENTANA,
                SugerenciaCompra.HORIZONTE_DIAS,
                HOY);

        assertEquals(OrigenSugerencia.REPOSICION_MANUAL, sugerencia.getOrigen(),
                "Sin retiros no hay base histórica: debe usar reposición manual.");
        assertEquals(0, sugerencia.getConsumoDiario().signum(),
                "El consumo diario debe quedar en cero, sin promedio histórico.");
        assertNull(sugerencia.getDiasCobertura(),
                "No se deben proyectar días de cobertura sin historial.");
        assertNull(sugerencia.getFechaEstimadaQuiebre(),
                "No se debe estimar fecha de quiebre sin historial.");
        assertEquals(new BigDecimal("5.00"), sugerencia.getCantidadSugerida(),
                "Reposición manual = mínimo - stock = 15 - 10 = 5 kg.");
    }

    @Test
    @DisplayName("Control: con salidas en 7 días LEC-001 sí usa el cálculo histórico")
    void conSalidasSiCalculaElHistorico() {
        SugerenciaCompra sugerencia = SugerenciaCompra.calcular(
                STOCK_ACTUAL,
                UMBRAL_MINIMO,
                new BigDecimal("7"),
                SugerenciaCompra.DIAS_VENTANA,
                SugerenciaCompra.HORIZONTE_DIAS,
                HOY);

        assertEquals(OrigenSugerencia.HISTORICO, sugerencia.getOrigen(),
                "Con retiros sí existe base histórica para proyectar.");
        assertEquals(new BigDecimal("1.0000"), sugerencia.getConsumoDiario(),
                "7 kg en 7 días = 1 kg por día.");
        assertNotNull(sugerencia.getFechaEstimadaQuiebre(),
                "Con historial sí se estima la fecha de quiebre.");
    }
}
