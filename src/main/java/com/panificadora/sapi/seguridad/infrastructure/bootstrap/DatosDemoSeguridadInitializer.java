package com.panificadora.sapi.seguridad.infrastructure.bootstrap;

import com.panificadora.sapi.seguridad.domain.Personal;
import com.panificadora.sapi.seguridad.domain.Rol;
import com.panificadora.sapi.seguridad.domain.UsuarioCredencial;
import com.panificadora.sapi.seguridad.infrastructure.PersonalRepository;
import com.panificadora.sapi.seguridad.infrastructure.RolRepository;
import com.panificadora.sapi.seguridad.infrastructure.UsuarioCredencialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Usuarios de demostración para el perfil H2.
 *
 * <p>Sin esto el perfil {@code demo} no tendría credenciales y el
 * administrador no podría entrar a ver el panel de alertas. En Oracle los
 * usuarios se crean con {@code db/03_schema_seguridad_oracle.sql}.</p>
 *
 * <p>Credenciales: {@code admin / admin123} y
 * {@code operario / operario123}.</p>
 */
@Component
@Profile("demo")
@RequiredArgsConstructor
public class DatosDemoSeguridadInitializer implements CommandLineRunner {

    private final RolRepository rolRepository;
    private final PersonalRepository personalRepository;
    private final UsuarioCredencialRepository usuarioCredencialRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (usuarioCredencialRepository.count() > 0) {
            return;
        }

        Rol administrador = rolRepository.findByNombre("ADMINISTRADOR")
                .orElseGet(() -> guardarRol(1L, "ADMINISTRADOR"));
        Rol operario = rolRepository.findByNombre("OPERARIO_ALMACEN")
                .orElseGet(() -> guardarRol(2L, "OPERARIO_ALMACEN"));

        Personal jefe = guardarPersonal(1L, "70123456", "Carlos", "Administrador", "admin@sapi.com");
        Personal almacenero = guardarPersonal(2L, "70654321", "Juan", "Almacenero", "operario@sapi.com");

        guardarUsuario(1L, jefe, administrador, "admin", "admin123");
        guardarUsuario(2L, almacenero, operario, "operario", "operario123");
    }

    private Rol guardarRol(Long id, String nombre) {
        Rol rol = new Rol();
        rol.setIdRol(id);
        rol.setNombre(nombre);
        return rolRepository.save(rol);
    }

    private Personal guardarPersonal(Long id, String dni, String nombres,
                                     String apellidos, String correo) {
        Personal personal = personalRepository.findById(id).orElseGet(Personal::new);
        personal.setIdPersonal(id);
        personal.setDni(dni);
        personal.setNombres(nombres);
        personal.setApellidos(apellidos);
        personal.setCorreo(correo);
        personal.setActivo(1);
        return personalRepository.save(personal);
    }

    private void guardarUsuario(Long id, Personal personal, Rol rol,
                                String usuario, String clavePlana) {
        UsuarioCredencial credencial = new UsuarioCredencial();
        credencial.setIdUsuario(id);
        credencial.setPersonal(personal);
        credencial.setRol(rol);
        credencial.setNombreUsuario(usuario);
        credencial.setClaveEncriptada(passwordEncoder.encode(clavePlana));
        credencial.setActivo(1);
        usuarioCredencialRepository.save(credencial);
    }
}
