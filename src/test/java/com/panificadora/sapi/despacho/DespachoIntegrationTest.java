package com.panificadora.sapi.despacho;

import com.panificadora.sapi.despacho.application.DespachoService;
import com.panificadora.sapi.despacho.domain.Despacho;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("demo") 
class DespachoIntegrationTest {

    @Autowired
    private DespachoService despachoService;

    @Test
    void registrarSalidaDebeGuardarCronometriaYPersonal() {
        Despacho salidaGuardada = despachoService.registrarSalida("PAN-001", 10.0, 105L, 100.0);

        assertNotNull(salidaGuardada.getIdMovimiento(), "El registro debe tener un ID autogenerado");
        assertNotNull(salidaGuardada.getFechaHora(), "Debe registrarse la cronometría exacta automáticamente");
        assertEquals(105L, salidaGuardada.getIdPersonal(), "El ID del panadero debe coincidir");
    }
}