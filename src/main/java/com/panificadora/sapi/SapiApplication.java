package com.panificadora.sapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada del monolito modular SAPI.
 *
 * <p>Los 5 módulos (Seguridad, Catálogo, Inventario, Despacho y Alertas) viven bajo
 * {@code com.panificadora.sapi.<modulo>} con la misma división interna:</p>
 * <pre>
 *   domain/          -> entidades JPA y reglas de negocio del dominio
 *   application/     -> servicios de caso de uso (sin dependencias de Spring MVC)
 *   infrastructure/  -> adaptadores: controladores MVC y repositorios Spring Data
 * </pre>
 */
@SpringBootApplication
public class SapiApplication {

    public static void main(String[] args) {
        SpringApplication.run(SapiApplication.class, args);
    }
}
