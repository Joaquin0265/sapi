package com.panificadora.sapi.seguridad.application.service;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.BadCredentialsException;

@SpringBootTest
public class AutenticacionServiceTest {

    @Autowired
    private AutenticacionService autenticacionService;

    @Test
    public void verificarEncriptarClave_RetornaHashSeguro() {
        String clavePlana = "secreta123";
        String hash = autenticacionService.encriptarClave(clavePlana);
        
        // Verifica que no guarde el texto plano
        assertNotEquals(clavePlana, hash);
        // Verifica que el hash generado use el algoritmo BCrypt
        assertTrue(hash.startsWith("$2a$")); 
    }

    @Test
    public void verificarValidarCredenciales_LanzaExcepcion() {
        // Verifica que lance la excepción al enviar credenciales incorrectas
        assertThrows(BadCredentialsException.class, () -> {
            autenticacionService.validarCredenciales("admin", "claveEquivocada");
        });
    }
}