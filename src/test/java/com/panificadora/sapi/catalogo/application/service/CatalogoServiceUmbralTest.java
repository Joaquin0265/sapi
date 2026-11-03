package com.panificadora.sapi.catalogo.application.service;

import com.panificadora.sapi.catalogo.domain.exception.ReglaNegocioException;
import com.panificadora.sapi.catalogo.domain.model.Insumo;
import com.panificadora.sapi.catalogo.domain.model.UmbralSeguridad;
import com.panificadora.sapi.catalogo.infrastructure.persistence.CategoriaRepository;
import com.panificadora.sapi.catalogo.infrastructure.persistence.InsumoRepository;
import com.panificadora.sapi.catalogo.infrastructure.persistence.ProductoRepository;
import com.panificadora.sapi.catalogo.infrastructure.persistence.UmbralSeguridadRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Prueba unitaria de verificación de la regla RF-01 / RF-02:
 * {@code registrarUmbral} debe rechazar un stock crítico mayor o igual que el
 * stock mínimo, incluso antes de consultar la base de datos.
 */
@ExtendWith(MockitoExtension.class)
class CatalogoServiceUmbralTest {

    private static final Long ID_INSUMO = 1L;

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private CategoriaRepository categoriaRepository;

    @Mock
    private InsumoRepository insumoRepository;

    @Mock
    private UmbralSeguridadRepository umbralSeguridadRepository;

    @InjectMocks
    private CatalogoService catalogoService;

    // ------------------------------------------------------------------
    // Casos inválidos: la regla se rompe antes de tocar la BD
    // ------------------------------------------------------------------

    @Test
    void registrarUmbral_rechazaCriticoIgualAlMinimo() {
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> catalogoService.registrarUmbral(
                        ID_INSUMO, new BigDecimal("50.00"), new BigDecimal("50.00")));

        assertTrue(ex.getMessage().contains("menor"),
                "El mensaje debe explicar que el crítico tiene que ser menor que el mínimo.");
        verifyNoInteractions(insumoRepository, umbralSeguridadRepository);
    }

    @Test
    void registrarUmbral_rechazaCriticoMayorAlMinimo() {
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> catalogoService.registrarUmbral(
                        ID_INSUMO, new BigDecimal("50.00"), new BigDecimal("80.00")));

        assertTrue(ex.getMessage().contains("menor"));
        verifyNoInteractions(insumoRepository, umbralSeguridadRepository);
    }

    @Test
    void registrarUmbral_rechazaStockMinimoNegativo() {
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> catalogoService.registrarUmbral(
                        ID_INSUMO, new BigDecimal("-1.00"), new BigDecimal("-5.00")));

        assertTrue(ex.getMessage().contains("negativo"));
        verifyNoInteractions(insumoRepository, umbralSeguridadRepository);
    }

    @Test
    void registrarUmbral_rechazaValoresNulos() {
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> catalogoService.registrarUmbral(ID_INSUMO, null, null));

        assertTrue(ex.getMessage().contains("stock mínimo"));
        verifyNoInteractions(insumoRepository, umbralSeguridadRepository);
    }

    @Test
    void registrarUmbral_rechazaInsumoInexistente() {
        when(insumoRepository.findByIdAndActivoTrue(ID_INSUMO)).thenReturn(Optional.empty());

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> catalogoService.registrarUmbral(
                        ID_INSUMO, new BigDecimal("50.00"), new BigDecimal("20.00")));

        assertTrue(ex.getMessage().contains("no existe"));
        verify(umbralSeguridadRepository, never()).save(any());
    }

    // ------------------------------------------------------------------
    // Casos válidos: crea o actualiza el umbral
    // ------------------------------------------------------------------

    @Test
    void registrarUmbral_creaElUmbralCuandoElInsumoNoTieneUno() {
        Insumo insumo = insumoActivo();
        when(insumoRepository.findByIdAndActivoTrue(ID_INSUMO)).thenReturn(Optional.of(insumo));
        when(umbralSeguridadRepository.findByInsumoId(ID_INSUMO)).thenReturn(Optional.empty());
        when(umbralSeguridadRepository.save(any(UmbralSeguridad.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        UmbralSeguridad resultado = catalogoService.registrarUmbral(
                ID_INSUMO, new BigDecimal("50.00"), new BigDecimal("20.00"));

        assertEquals(new BigDecimal("50.00"), resultado.getStockMinimo());
        assertEquals(new BigDecimal("20.00"), resultado.getStockCritico());
        assertSame(insumo, resultado.getInsumo());
        assertSame(resultado, insumo.getUmbral(), "La relación 1:1 debe quedar coherente en memoria.");
        verify(umbralSeguridadRepository).save(any(UmbralSeguridad.class));
    }

    @Test
    void registrarUmbral_actualizaElUmbralExistente() {
        Insumo insumo = insumoActivo();
        UmbralSeguridad existente = UmbralSeguridad.builder()
                .id(9L)
                .insumo(insumo)
                .stockMinimo(new BigDecimal("30.00"))
                .stockCritico(new BigDecimal("10.00"))
                .build();

        when(insumoRepository.findByIdAndActivoTrue(ID_INSUMO)).thenReturn(Optional.of(insumo));
        when(umbralSeguridadRepository.findByInsumoId(ID_INSUMO)).thenReturn(Optional.of(existente));
        when(umbralSeguridadRepository.save(any(UmbralSeguridad.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        UmbralSeguridad resultado = catalogoService.registrarUmbral(
                ID_INSUMO, new BigDecimal("60.00"), new BigDecimal("25.00"));

        assertSame(existente, resultado, "Debe reutilizar la fila, no crear otra.");
        assertEquals(new BigDecimal("60.00"), resultado.getStockMinimo());
        assertEquals(new BigDecimal("25.00"), resultado.getStockCritico());
    }

    @Test
    void registrarUmbral_aceptaCriticoEnCero() {
        Insumo insumo = insumoActivo();
        when(insumoRepository.findByIdAndActivoTrue(ID_INSUMO)).thenReturn(Optional.of(insumo));
        when(umbralSeguridadRepository.findByInsumoId(ID_INSUMO)).thenReturn(Optional.empty());
        when(umbralSeguridadRepository.save(any(UmbralSeguridad.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        UmbralSeguridad resultado = catalogoService.registrarUmbral(
                ID_INSUMO, new BigDecimal("10.00"), BigDecimal.ZERO);

        assertEquals(BigDecimal.ZERO, resultado.getStockCritico());
    }

    private Insumo insumoActivo() {
        return Insumo.builder()
                .id(ID_INSUMO)
                .codigo("HAR-001")
                .nombre("Harina Especial")
                .activo(Boolean.TRUE)
                .build();
    }
}
