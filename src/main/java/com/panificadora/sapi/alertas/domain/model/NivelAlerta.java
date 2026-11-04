package com.panificadora.sapi.alertas.domain.model;

/**
 * Semáforo de stock del módulo de Alertas.
 *
 * <p>El orden de las constantes es intencional: se usa para ordenar el panel
 * por severidad (primero los quiebres).</p>
 *
 * <p>Cada nivel trae sus clases de Bootstrap para que la vista no contenga
 * reglas de negocio ni condicionales de color.</p>
 */
public enum NivelAlerta {

    ROJO("Quiebre", "text-bg-danger", "table-danger", "progress-bar bg-danger"),
    AMARILLO("Por reponer", "text-bg-warning", "table-warning", "progress-bar bg-warning"),
    VERDE("Stock saludable", "text-bg-success", "", "progress-bar bg-success"),
    SIN_UMBRAL("Sin umbral", "text-bg-secondary", "", "progress-bar bg-secondary");

    private final String etiqueta;
    private final String cssBadge;
    private final String cssTabla;
    private final String cssBarra;

    NivelAlerta(String etiqueta, String cssBadge, String cssTabla, String cssBarra) {
        this.etiqueta = etiqueta;
        this.cssBadge = cssBadge;
        this.cssTabla = cssTabla;
        this.cssBarra = cssBarra;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public String getCssBadge() {
        return cssBadge;
    }

    public String getCssTabla() {
        return cssTabla;
    }

    public String getCssBarra() {
        return cssBarra;
    }

    public boolean esRojo() {
        return this == ROJO;
    }
}
