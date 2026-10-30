package com.panificadora.sapi.catalogo.domain.exception;

/**
 * Excepción de regla de negocio del dominio.
 * No conoce HTTP ni Spring: el adaptador web decide cómo mostrarla.
 */
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
