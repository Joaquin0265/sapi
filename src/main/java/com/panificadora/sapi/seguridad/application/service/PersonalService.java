package com.panificadora.sapi.seguridad.application.service;

import com.panificadora.sapi.seguridad.domain.Personal;
import com.panificadora.sapi.seguridad.infrastructure.PersonalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PersonalService {

    private final PersonalRepository personalRepository;

    public List<Personal> listarActivos() {
        return personalRepository.findByActivo(1);
    }

    @Transactional
    public Personal guardar(Personal personal) {
        if (personal == null) {
            throw new IllegalArgumentException("El personal es obligatorio.");
        }

        String dni = personal.getDni() == null ? "" : personal.getDni().trim();
        if (!dni.matches("^\\d{8}$")) {
            throw new IllegalArgumentException("El DNI debe tener exactamente 8 dígitos numéricos.");
        }

        personal.setDni(dni);

        if (personal.getActivo() == null) {
            personal.setActivo(1);
        }

        personalRepository.findByDniAndActivo(dni, 1)
                .filter(existing -> personal.getIdPersonal() == null || !existing.getIdPersonal().equals(personal.getIdPersonal()))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Ya existe un personal activo con ese DNI.");
                });

        return personalRepository.save(personal);
    }

    @Transactional
    public void desactivar(Long id) {
        Personal personal = personalRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No existe el personal solicitado."));

        personal.setActivo(0);
        personalRepository.save(personal);
    }
}
