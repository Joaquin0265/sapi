package com.panificadora.sapi.despacho;

import com.panificadora.sapi.despacho.application.DespachoService;
import com.panificadora.sapi.despacho.infrastructure.DespachoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class DespachoServiceTest {

    @Mock
    private DespachoRepository despachoRepository;

    @InjectMocks
    private DespachoService despachoService;

    @Test
    void retirar50kgCuandoHay20kgDebeLanzarError() {
        Double cantidadSolicitada = 50.0;
        Double stockActual = 20.0;

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            despachoService.registrarSalida("PAN-001", cantidadSolicitada, 101L, stockActual);
        });

        assertEquals("Stock insuficiente", exception.getMessage());
    }
}