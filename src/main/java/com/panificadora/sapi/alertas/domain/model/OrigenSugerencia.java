package com.panificadora.sapi.alertas.domain.model;

/**
 * Procedencia de la cantidad sugerida de compra.
 *
 * <p>RN-04: si no existen retiros en los últimos 7 días no hay base histórica
 * para proyectar, por lo que se devuelve la reposición manual estándar.</p>
 */
public enum OrigenSugerencia {

    HISTORICO("Histórico 7 días", "text-bg-info"),
    REPOSICION_MANUAL("Reposición manual estándar", "text-bg-secondary");

    private final String etiqueta;
    private final String cssBadge;

    OrigenSugerencia(String etiqueta, String cssBadge) {
        this.etiqueta = etiqueta;
        this.cssBadge = cssBadge;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public String getCssBadge() {
        return cssBadge;
    }
}
