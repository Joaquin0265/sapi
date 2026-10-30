package com.panificadora.sapi.catalogo.domain.model;

/**
 * Unidad de venta/almacenamiento del producto.
 * Se persiste como texto ({@code @Enumerated(EnumType.STRING)}) para que la
 * columna UNIDAD_MEDIDA sea legible desde PL/SQL.
 */
public enum UnidadMedida {

    UNIDAD("Unidad"),
    KILOGRAMO("Kilogramo"),
    GRAMO("Gramo"),
    LITRO("Litro"),
    CAJA("Caja");

    private final String etiqueta;

    UnidadMedida(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
