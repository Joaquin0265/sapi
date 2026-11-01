package com.panificadora.sapi.seguridad.application.service;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AutenticacionService {

    private final PasswordEncoder passwordEncoder;

    public AutenticacionService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public String encriptarClave(String clavePlana) {
        return passwordEncoder.encode(clavePlana);
    }

    public boolean validarCredenciales(String username, String clavePlana) {
        // Simulación temporal del hash guardado en la tabla usuario_credencial
        // (En producción, aquí harías una consulta a la BD usando el repositorio)
        String hashEnBaseDeDatos = passwordEncoder.encode("admin123");
        
        if (!passwordEncoder.matches(clavePlana, hashEnBaseDeDatos)) {
            throw new BadCredentialsException("Credenciales incorrectas");
        }
        
        return true;
    }
}