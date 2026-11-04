package com.panificadora.sapi.alertas.application.dto;

import com.panificadora.sapi.alertas.domain.model.NivelAlerta;
import com.panificadora.sapi.alertas.domain.model.OrigenSugerencia;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

/**
 * Fila del panel de alertas ya consolidada: stock real, umbrales, semáforo y
 * proyección. Es un DTO de solo lectura; no se persiste.
 */
@Getter
@Builder
public class AlertaStockView {

    private final Long idInsumo;
    private final String codigo;
    private final String nombre;
    private final String unidad;
    private final BigDecimal stockActual;
    private final BigDecimal stockMinimo;
    private final BigDecimal stockCritico;
    private final NivelAlerta nivel;
    private final boolean quiebreInminente;
    private final BigDecimal faltante;
    private final BigDecimal consumoDiario;
    private final BigDecimal diasCobertura;
    private final LocalDate fechaEstimadaQuiebre;
    private final BigDecimal cantidadSugerida;
    private final OrigenSugerencia origenSugerencia;

    public boolean isSinUmbral() {
        return nivel == NivelAlerta.SIN_UMBRAL;
    }

    /** Altura de la barra de cobertura respecto al punto de reorden (0–100). */
    public int getPorcentajeCobertura() {
        if (stockMinimo == null || stockMinimo.signum() <= 0) {
            return stockActual != null && stockActual.signum() > 0 ? 100 : 0;
        }
        BigDecimal porcentaje = stockActual
                .multiply(BigDecimal.valueOf(100))
                .divide(stockMinimo, 0, RoundingMode.HALF_UP);
        return Math.max(0, Math.min(100, porcentaje.intValue()));
    }

    public String getEtiquetaSugerencia() {
        return origenSugerencia.getEtiqueta();
    }

    public String getBadgeSugerencia() {
        return origenSugerencia.getCssBadge();
    }

    public boolean isSugiereCompra() {
        return cantidadSugerida != null && cantidadSugerida.signum() > 0;
    }
}
