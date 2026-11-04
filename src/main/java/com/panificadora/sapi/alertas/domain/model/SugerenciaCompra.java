package com.panificadora.sapi.alertas.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Proyección de consumo y sugerencia de compra (RF-05 / RN-04).
 *
 * <p>Con salidas en la ventana: {@code consumoDiario = salidas / 7} y la
 * cantidad sugerida cubre {@value #HORIZONTE_DIAS} días de operación.</p>
 *
 * <p>Sin salidas en los últimos 7 días (RN-04): no se calcula promedio ni
 * fecha de quiebre; se devuelve la <b>reposición manual estándar</b>, es decir,
 * lo que falta para volver al stock mínimo.</p>
 */
public class SugerenciaCompra {

    /** Ventana histórica exigida por RN-04. */
    public static final int DIAS_VENTANA = 7;

    /** Horizonte de cobertura de la compra sugerida. */
    public static final int HORIZONTE_DIAS = 14;

    private final OrigenSugerencia origen;
    private final BigDecimal consumoDiario;
    private final BigDecimal diasCobertura;
    private final LocalDate fechaEstimadaQuiebre;
    private final BigDecimal cantidadSugerida;

    public SugerenciaCompra(OrigenSugerencia origen,
                            BigDecimal consumoDiario,
                            BigDecimal diasCobertura,
                            LocalDate fechaEstimadaQuiebre,
                            BigDecimal cantidadSugerida) {
        this.origen = origen;
        this.consumoDiario = consumoDiario;
        this.diasCobertura = diasCobertura;
        this.fechaEstimadaQuiebre = fechaEstimadaQuiebre;
        this.cantidadSugerida = cantidadSugerida;
    }

    public static SugerenciaCompra calcular(BigDecimal stockActual,
                                            BigDecimal stockMinimo,
                                            BigDecimal salidasVentana,
                                            int diasVentana,
                                            int horizonteDias,
                                            LocalDate hoy) {
        if (diasVentana <= 0) {
            throw new IllegalArgumentException("La ventana histórica debe ser mayor a cero.");
        }
        if (horizonteDias < 0) {
            throw new IllegalArgumentException("El horizonte de cobertura no puede ser negativo.");
        }
        Objects.requireNonNull(hoy, "La fecha de referencia es obligatoria.");

        BigDecimal stock = stockActual == null ? BigDecimal.ZERO : stockActual;
        BigDecimal salidas = salidasVentana == null ? BigDecimal.ZERO : salidasVentana;

        BigDecimal consumoDiario = salidas.divide(
                BigDecimal.valueOf(diasVentana), 4, RoundingMode.HALF_UP);

        // RN-04: sin retiros en la ventana no se proyecta; reposición manual.
        if (consumoDiario.signum() == 0) {
            BigDecimal cantidad = stockMinimo == null
                    ? BigDecimal.ZERO
                    : stockMinimo.subtract(stock).max(BigDecimal.ZERO);
            return new SugerenciaCompra(
                    OrigenSugerencia.REPOSICION_MANUAL,
                    BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
                    null,
                    null,
                    cantidad.setScale(2, RoundingMode.CEILING));
        }

        BigDecimal diasCobertura = stock.divide(consumoDiario, 1, RoundingMode.HALF_UP);
        LocalDate fechaQuiebre = hoy.plusDays(
                Math.max(0, diasCobertura.setScale(0, RoundingMode.FLOOR).longValue()));

        BigDecimal cantidad = consumoDiario
                .multiply(BigDecimal.valueOf(horizonteDias))
                .subtract(stock)
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.CEILING);

        return new SugerenciaCompra(
                OrigenSugerencia.HISTORICO, consumoDiario, diasCobertura, fechaQuiebre, cantidad);
    }

    public OrigenSugerencia getOrigen() {
        return origen;
    }

    public BigDecimal getConsumoDiario() {
        return consumoDiario;
    }

    public BigDecimal getDiasCobertura() {
        return diasCobertura;
    }

    public LocalDate getFechaEstimadaQuiebre() {
        return fechaEstimadaQuiebre;
    }

    public BigDecimal getCantidadSugerida() {
        return cantidadSugerida;
    }
}
