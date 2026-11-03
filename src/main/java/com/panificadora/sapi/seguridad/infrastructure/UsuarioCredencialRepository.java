package com.panificadora.sapi.seguridad.infrastructure;

import com.panificadora.sapi.seguridad.domain.UsuarioCredencial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UsuarioCredencialRepository extends JpaRepository<UsuarioCredencial, Long> {
    Optional<UsuarioCredencial> findByNombreUsuario(String nombreUsuario);
}