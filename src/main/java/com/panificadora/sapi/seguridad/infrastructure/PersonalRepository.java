package com.panificadora.sapi.seguridad.infrastructure;

import com.panificadora.sapi.seguridad.domain.Personal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PersonalRepository extends JpaRepository<Personal, Long> {
    Optional<Personal> findByDni(String dni);

    List<Personal> findByActivo(Integer activo);

    Optional<Personal> findByDniAndActivo(String dni, Integer activo);

    Optional<Personal> findByIdPersonalAndActivo(Long idPersonal, Integer activo);
}