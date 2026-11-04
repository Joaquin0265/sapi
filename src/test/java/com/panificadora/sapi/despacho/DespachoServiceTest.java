package com.panificadora.sapi.despacho;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.panificadora.sapi.catalogo.domain.model.Insumo;
import com.panificadora.sapi.catalogo.infrastructure.persistence.InsumoRepository;
import com.panificadora.sapi.despacho.application.DespachoService;
import com.panificadora.sapi.despacho.infrastructure.DespachoRepository;
import com.panificadora.sapi.inventario.application.service.InventarioService;
import com.panificadora.sapi.inventario.domain.exception.ReglaNegocioException;
import com.panificadora.sapi.inventario.infrastructure.persistence.MovimientoRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Prueba unitaria de la salida de insumos: si Inventario rechaza el consumo
 * FIFO por stock insuficiente, no se persiste ni cabecera ni detalle.
 */
@ExtendWith(MockitoExtension.class)
class DespachoServiceTest {

    @Mock
    private DespachoRepository despachoRepository;

    @Mock
    private MovimientoRepository movimientoRepository;

    @Mock
    private InsumoRepository insumoRepository;

    @Mock
    private InventarioService inventarioService;

    @InjectMocks
    private DespachoService despachoService;

    @Test
    void retirarMasDeLoDisponibleNoPersisteNada() {
        Insumo harina = Insumo.builder().id(1L).codigo("HAR-001").activo(Boolean.TRUE).build();
        when(insumoRepository.findByIdAndActivoTrue(1L)).thenReturn(Optional.of(harina));
        doThrow(new ReglaNegocioException("Stock insuficiente: faltan 30 unidades."))
                .when(inventarioService).consumirFifo(eq(1L), any(BigDecimal.class));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> despachoService.registrarSalida(1L, new BigDecimal("50"), 101L));

        assertEquals("Stock insuficiente: faltan 30 unidades.", ex.getMessage());
        verifyNoInteractions(despachoRepository, movimientoRepository);
    }

    @Test
    void cantidadCeroNoTocaLaBase() {
        assertThrows(ReglaNegocioException.class,
                () -> despachoService.registrarSalida(1L, BigDecimal.ZERO, 101L));

        verifyNoInteractions(insumoRepository, inventarioService,
                despachoRepository, movimientoRepository);
    }

    @Test
    void insumoInexistenteNoDescuentaStock() {
        when(insumoRepository.findByIdAndActivoTrue(99L)).thenReturn(Optional.empty());

        assertThrows(ReglaNegocioException.class,
                () -> despachoService.registrarSalida(99L, BigDecimal.ONE, 101L));

        verifyNoInteractions(inventarioService, despachoRepository, movimientoRepository);
    }
}
