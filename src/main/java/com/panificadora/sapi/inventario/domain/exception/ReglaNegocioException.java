package com.panificadora.sapi.inventario.domain.exception;

/**
 * Excepción de regla de negocio del módulo Inventario.
 * No conoce HTTP ni Spring: el adaptador web decide cómo mostrarla.
 */
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
