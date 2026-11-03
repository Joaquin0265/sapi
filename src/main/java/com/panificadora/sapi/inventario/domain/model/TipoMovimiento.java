package com.panificadora.sapi.inventario.domain.model;

/**
 * Naturaleza de un movimiento de inventario.
 * Se persiste como texto ({@code @Enumerated(EnumType.STRING)}) para que la
 * columna TIPO_MOVIMIENTO sea legible desde PL/SQL.
 */
public enum TipoMovimiento {

    INGRESO("Ingreso"),
    EGRESO("Egreso"),
    AJUSTE("Ajuste");

    private final String etiqueta;

    TipoMovimiento(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
