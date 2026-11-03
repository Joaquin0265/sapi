package com.panificadora.sapi.seguridad.application.service;

import com.panificadora.sapi.seguridad.infrastructure.UsuarioCredencialRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UsuarioCredencialRepository repository;

    public UserDetailsServiceImpl(UsuarioCredencialRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var credencial = repository.findByNombreUsuario(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

        String nombreRol = credencial.getRol() == null
                ? ""
                : credencial.getRol().getNombre();

        String rolNormalizado = nombreRol == null
                ? ""
                : nombreRol.trim().toUpperCase();

        if (rolNormalizado.isBlank()) {
            throw new UsernameNotFoundException("El usuario no tiene un rol asociado: " + username);
        }

        var authority = new SimpleGrantedAuthority(rolNormalizado);

        return new User(
                credencial.getNombreUsuario(),
                credencial.getClaveEncriptada(),
                credencial.getActivo() == 1,
                true,
                true,
                true,
                Collections.singletonList(authority)
        );
    }
}