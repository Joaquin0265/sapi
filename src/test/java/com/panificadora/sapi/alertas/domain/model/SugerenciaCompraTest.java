package com.panificadora.sapi.alertas.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/**
 * Prueba unitaria de verificación de RN-04: si no hay salidas en los últimos
 * 7 días, el algoritmo debe omitir el cálculo histórico y devolver la
 * reposición manual estándar.
 */
class SugerenciaCompraTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 4);

    @Test
    void sinSalidasEnSieteDiasDevuelveReposicionManualEstandar() {
        SugerenciaCompra sugerencia = SugerenciaCompra.calcular(
                new BigDecimal("10"), new BigDecimal("15"),
                BigDecimal.ZERO, 7, 14, HOY);

        assertEquals(OrigenSugerencia.REPOSICION_MANUAL, sugerencia.getOrigen(),
                "Sin retiros no hay base histórica: reposición manual.");
        assertEquals(0, sugerencia.getConsumoDiario().signum(),
                "El consumo diario debe quedar en cero.");
        assertNull(sugerencia.getDiasCobertura(),
                "No se proyectan días de cobertura sin historial.");
        assertNull(sugerencia.getFechaEstimadaQuiebre(),
                "No se estima fecha de quiebre sin historial.");
        assertEquals(new BigDecimal("5.00"), sugerencia.getCantidadSugerida(),
                "Reposición manual estándar = mínimo - stock = 15 - 10.");
    }

    @Test
    void conSalidasCalculaConsumoProyeccionYCompraA14Dias() {
        SugerenciaCompra sugerencia = SugerenciaCompra.calcular(
                new BigDecimal("30"), new BigDecimal("50"),
                new BigDecimal("35"), 7, 14, HOY);

        assertEquals(OrigenSugerencia.HISTORICO, sugerencia.getOrigen());
        assertEquals(new BigDecimal("5.0000"), sugerencia.getConsumoDiario(),
                "35 unidades en 7 días = 5 por día.");
        assertEquals(new BigDecimal("6.0"), sugerencia.getDiasCobertura(),
                "30 / 5 = 6 días de cobertura.");
        assertEquals(HOY.plusDays(6), sugerencia.getFechaEstimadaQuiebre());
        assertEquals(new BigDecimal("40.00"), sugerencia.getCantidadSugerida(),
                "5 * 14 - 30 = 40 unidades para cubrir el horizonte.");
    }

    @Test
    void sinUmbralYsinSalidasNoInventaCantidad() {
        SugerenciaCompra sugerencia = SugerenciaCompra.calcular(
                new BigDecimal("0"), null,
                BigDecimal.ZERO, 7, 14, HOY);

        assertEquals(OrigenSugerencia.REPOSICION_MANUAL, sugerencia.getOrigen());
        assertEquals(new BigDecimal("0.00"), sugerencia.getCantidadSugerida());
    }

    @Test
    void ventanaInvalidaLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> SugerenciaCompra.calcular(
                BigDecimal.TEN, BigDecimal.ONE, BigDecimal.TEN, 0, 14, HOY));
    }

    @Test
    void horizonteNegativoLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> SugerenciaCompra.calcular(
                BigDecimal.TEN, BigDecimal.ONE, BigDecimal.TEN, 7, -1, HOY));
    }
}
