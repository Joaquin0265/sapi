package com.panificadora.sapi.alertas.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/**
 * Prueba unitaria de verificación de RN-03:
 * {@code evaluarQuiebreStock(stock_actual, umbral_minimo)} debe dispararse
 * cuando {@code stock_actual - umbral_minimo <= 0}.
 *
 * <p>Caso exigido por la asignación: stock = 10, umbral = 15.</p>
 */
class EvaluacionStockTest {

    @Test
    void evaluarQuiebreStockConStockMenorAlUmbralDispara() {
        assertTrue(EvaluacionStock.evaluarQuiebreStock(
                        new BigDecimal("10"), new BigDecimal("15")),
                "10 - 15 = -5 <= 0: debe disparar la alerta.");
    }

    @Test
    void evaluarQuiebreStockEnElLimiteTambienDispara() {
        assertTrue(EvaluacionStock.evaluarQuiebreStock(
                        new BigDecimal("15"), new BigDecimal("15")),
                "15 - 15 = 0 <= 0: el límite también cuenta como quiebre.");
    }

    @Test
    void evaluarQuiebreStockSobreElUmbralNoDispara() {
        assertFalse(EvaluacionStock.evaluarQuiebreStock(
                        new BigDecimal("15.01"), new BigDecimal("15")),
                "15.01 - 15 > 0: todavía hay stock sobre el punto de reorden.");
    }

    @Test
    void nivelRojoCuandoElStockNoSuperaElMinimo() {
        EvaluacionStock evaluacion = new EvaluacionStock(
                new BigDecimal("10"), new BigDecimal("15"), new BigDecimal("5"));
        assertEquals(NivelAlerta.ROJO, evaluacion.getNivel());
    }

    @Test
    void nivelAmarilloEnLaZonaDeAvisoDelVeintePorCiento() {
        EvaluacionStock evaluacion = new EvaluacionStock(
                new BigDecimal("17"), new BigDecimal("15"), new BigDecimal("5"));
        assertEquals(NivelAlerta.AMARILLO, evaluacion.getNivel());
    }

    @Test
    void nivelVerdeCuandoSuperaLaZonaDeAviso() {
        EvaluacionStock evaluacion = new EvaluacionStock(
                new BigDecimal("19"), new BigDecimal("15"), new BigDecimal("5"));
        assertEquals(NivelAlerta.VERDE, evaluacion.getNivel());
    }

    @Test
    void nivelSinUmbralCuandoNoHayPoliticaConfigurada() {
        EvaluacionStock evaluacion = new EvaluacionStock(
                new BigDecimal("19"), null, null);
        assertEquals(NivelAlerta.SIN_UMBRAL, evaluacion.getNivel());
    }

    @Test
    void marcaQuiebreInminenteCuandoBajaDelCriticoOEstaEnCero() {
        assertTrue(new EvaluacionStock(new BigDecimal("4"), new BigDecimal("15"), new BigDecimal("5"))
                .isQuiebreInminente(), "4 <= 5: quiebre inminente.");
        assertTrue(new EvaluacionStock(BigDecimal.ZERO, new BigDecimal("15"), new BigDecimal("5"))
                .isQuiebreInminente(), "Sin existencias: quiebre inminente.");
        assertFalse(new EvaluacionStock(new BigDecimal("10"), new BigDecimal("15"), new BigDecimal("5"))
                .isQuiebreInminente(), "10 > 5: rojo, pero no inminente.");
    }

    @Test
    void faltanteEsLoQueFaltaParaVolverAlMinimo() {
        EvaluacionStock evaluacion = new EvaluacionStock(
                new BigDecimal("30"), new BigDecimal("50"), new BigDecimal("20"));
        assertEquals(new BigDecimal("20.00"), evaluacion.getFaltante());
    }
}
