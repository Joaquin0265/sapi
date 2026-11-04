package com.panificadora.sapi.seguridad.infrastructure;

import com.panificadora.sapi.seguridad.domain.Rol;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Acceso a los roles de seguridad (ADMINISTRADOR, OPERARIO_ALMACEN). */
@Repository
public interface RolRepository extends JpaRepository<Rol, Long> {

    Optional<Rol> findByNombre(String nombre);
}
