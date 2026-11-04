package com.panificadora.sapi.alertas.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Evaluación de un insumo contra su política de reposición.
 *
 * <p>RN-03: existe quiebre cuando {@code stock_actual - umbral_minimo <= 0}.
 * La prueba de verificación usa el caso {@code (10, 15)} y espera que el
 * semáforo se dispare: {@code 10 - 15 = -5 <= 0}.</p>
 *
 * <p>El nivel ámbar es una zona de aviso temprana: {@code stock <= minimo * 1.20}.
 * El stock crítico no redefine el rojo (RN-03 manda), pero sí marca la bandera
 * {@link #isQuiebreInminente()} para que la vista lo destaque.</p>
 */
public class EvaluacionStock {

    /** Margen de aviso temprana sobre el punto de reorden. */
    private static final BigDecimal FACTOR_AMARILLO = new BigDecimal("1.20");

    private final BigDecimal stockActual;
    private final BigDecimal stockMinimo;
    private final BigDecimal stockCritico;

    public EvaluacionStock(BigDecimal stockActual, BigDecimal stockMinimo, BigDecimal stockCritico) {
        this.stockActual = stockActual == null ? BigDecimal.ZERO : stockActual;
        this.stockMinimo = stockMinimo;
        this.stockCritico = stockCritico;
    }

    /**
     * Regla RN-03, pura y sin dependencias de Spring.
     *
     * @return {@code true} cuando el stock alcanzó o cayó por debajo del umbral.
     */
    public static boolean evaluarQuiebreStock(BigDecimal stockActual, BigDecimal umbralMinimo) {
        Objects.requireNonNull(stockActual, "El stock actual es obligatorio.");
        Objects.requireNonNull(umbralMinimo, "El umbral mínimo es obligatorio.");
        return stockActual.subtract(umbralMinimo).compareTo(BigDecimal.ZERO) <= 0;
    }

    public NivelAlerta getNivel() {
        if (stockMinimo == null) {
            return NivelAlerta.SIN_UMBRAL;
        }
        if (evaluarQuiebreStock(stockActual, stockMinimo)) {
            return NivelAlerta.ROJO;
        }
        if (stockActual.compareTo(stockMinimo.multiply(FACTOR_AMARILLO)) <= 0) {
            return NivelAlerta.AMARILLO;
        }
        return NivelAlerta.VERDE;
    }

    /** Quiebre inminente: el saldo ya está en el nivel crítico o no hay unidades. */
    public boolean isQuiebreInminente() {
        if (stockActual.compareTo(BigDecimal.ZERO) <= 0) {
            return true;
        }
        return stockCritico != null && stockActual.compareTo(stockCritico) <= 0;
    }

    /** Unidades que faltan para volver al punto de reorden. */
    public BigDecimal getFaltante() {
        if (stockMinimo == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return stockMinimo.subtract(stockActual)
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.CEILING);
    }

    public BigDecimal getStockActual() {
        return stockActual;
    }

    public BigDecimal getStockMinimo() {
        return stockMinimo;
    }

    public BigDecimal getStockCritico() {
        return stockCritico;
    }
}
