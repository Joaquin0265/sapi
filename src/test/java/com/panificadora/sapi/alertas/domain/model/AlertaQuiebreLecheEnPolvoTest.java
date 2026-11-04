package com.panificadora.sapi.alertas.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Prueba unitaria de RN-03 con el insumo <b>LEC-001 / Leche en polvo</b>.
 *
 * <p>Escenario: stock actual = 10 kg y umbral mínimo = 15 kg. La alerta debe
 * dispararse automáticamente al cumplirse la condición lógica
 * {@code Stock Actual <= Umbral}.</p>
 */
class AlertaQuiebreLecheEnPolvoTest {

    private static final BigDecimal STOCK_ACTUAL = new BigDecimal("10");
    private static final BigDecimal UMBRAL_MINIMO = new BigDecimal("15");
    private static final BigDecimal STOCK_CRITICO = new BigDecimal("5");

    @Test
    @DisplayName("LEC-001 con 10 kg y umbral 15 kg dispara la alerta (10 <= 15)")
    void stockMenorAlUmbralDisparaLaAlerta() {
        assertTrue(EvaluacionStock.evaluarQuiebreStock(STOCK_ACTUAL, UMBRAL_MINIMO),
                "10 - 15 = -5 <= 0: la alerta debe dispararse.");
    }

    @Test
    @DisplayName("LEC-001 en el límite exacto también dispara (15 <= 15)")
    void stockIgualAlUmbralTambienDisparaLaAlerta() {
        assertTrue(EvaluacionStock.evaluarQuiebreStock(
                        UMBRAL_MINIMO, UMBRAL_MINIMO),
                "15 - 15 = 0 <= 0: el límite forma parte de la condición.");
    }

    @Test
    @DisplayName("LEC-001 con stock sobre el umbral no dispara la alerta")
    void stockSobreElUmbralNoDisparaLaAlerta() {
        assertFalse(EvaluacionStock.evaluarQuiebreStock(
                        new BigDecimal("15.01"), UMBRAL_MINIMO),
                "15.01 - 15 > 0: todavía no corresponde alertar.");
    }

    @Test
    @DisplayName("El semáforo de LEC-001 queda en ROJO y reporta 5 kg faltantes")
    void elSemaforoDelProductoQuedaEnRojo() {
        EvaluacionStock evaluacion = new EvaluacionStock(
                STOCK_ACTUAL, UMBRAL_MINIMO, STOCK_CRITICO);

        assertEquals(NivelAlerta.ROJO, evaluacion.getNivel(),
                "10 <= 15: el semáforo debe mostrarse en rojo.");
        assertEquals(new BigDecimal("5.00"), evaluacion.getFaltante(),
                "Faltante para volver al mínimo = 15 - 10 = 5 kg.");
    }
}
