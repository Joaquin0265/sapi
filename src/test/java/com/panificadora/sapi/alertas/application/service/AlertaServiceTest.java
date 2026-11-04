package com.panificadora.sapi.alertas.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.panificadora.sapi.alertas.application.dto.AlertaStockView;
import com.panificadora.sapi.alertas.domain.model.NivelAlerta;
import com.panificadora.sapi.alertas.domain.model.OrigenSugerencia;
import com.panificadora.sapi.alertas.infrastructure.persistence.SalidaInsumoRepository;
import com.panificadora.sapi.alertas.infrastructure.persistence.StockInsumoRepository;
import com.panificadora.sapi.catalogo.domain.model.Insumo;
import com.panificadora.sapi.catalogo.domain.model.UmbralSeguridad;
import com.panificadora.sapi.catalogo.domain.model.UnidadMedida;
import com.panificadora.sapi.catalogo.infrastructure.persistence.InsumoRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Prueba unitaria del motor: combina stock real, umbrales y retiros históricos
 * sin tocar base de datos.
 */
@ExtendWith(MockitoExtension.class)
class AlertaServiceTest {

    @Mock
    private InsumoRepository insumoRepository;

    @Mock
    private StockInsumoRepository stockInsumoRepository;

    @Mock
    private SalidaInsumoRepository salidaInsumoRepository;

    @InjectMocks
    private AlertaService alertaService;

    @Test
    void evaluaSemaforoYOrdenaPrimeroLosQuiebres() {
        Insumo harina = insumo(1L, "HAR-001", "Harina Especial", "50.00", "20.00");
        Insumo azucar = insumo(2L, "AZU-001", "Azúcar rubia", "30.00", "10.00");
        Insumo sal = insumo(3L, "SAL-001", "Sal industrial", null, null);

        // Las proyecciones se crean ANTES de encadenar when(...).thenReturn(...);
        // si se crean dentro del thenReturn, Mockito reporta UnfinishedStubbing.
        var stockHarina = stock(1L, "30.00");
        var stockAzucar = stock(2L, "33.00");
        var stockSal = stock(3L, "0.00");
        var salidaHarina = salida(1L, "35.00");
        var salidaAzucar = salida(2L, "14.00");

        when(insumoRepository.findByActivoTrueOrderByNombreAsc())
                .thenReturn(List.of(harina, azucar, sal));
        when(stockInsumoRepository.stockVigentePorInsumo(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(stockHarina, stockAzucar, stockSal));
        when(salidaInsumoRepository.salidasDesde(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(salidaHarina, salidaAzucar));

        List<AlertaStockView> alertas = alertaService.evaluarTodas();

        assertEquals(List.of("HAR-001", "AZU-001", "SAL-001"),
                alertas.stream().map(AlertaStockView::getCodigo).toList(),
                "Primero los rojos, luego los ámbar y al final los sin umbral.");

        AlertaStockView rojo = alertas.get(0);
        assertEquals(NivelAlerta.ROJO, rojo.getNivel());
        assertEquals(new BigDecimal("20.00"), rojo.getFaltante());
        assertEquals(OrigenSugerencia.HISTORICO, rojo.getOrigenSugerencia());
        assertEquals(new BigDecimal("40.00"), rojo.getCantidadSugerida());

        AlertaStockView amarillo = alertas.get(1);
        assertEquals(NivelAlerta.AMARILLO, amarillo.getNivel());

        AlertaStockView sinUmbral = alertas.get(2);
        assertEquals(NivelAlerta.SIN_UMBRAL, sinUmbral.getNivel());
        assertNull(sinUmbral.getStockMinimo());
    }

    @Test
    void sinRetirosDevuelveReposicionManualEstandar() {
        Insumo manteca = insumo(4L, "MAN-001", "Manteca vegetal", "15.00", "5.00");

        var stockManteca = stock(4L, "8.00");

        when(insumoRepository.findByActivoTrueOrderByNombreAsc()).thenReturn(List.of(manteca));
        when(stockInsumoRepository.stockVigentePorInsumo(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(stockManteca));
        when(salidaInsumoRepository.salidasDesde(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of());

        AlertaStockView alerta = alertaService.evaluarTodas().get(0);

        assertEquals(NivelAlerta.ROJO, alerta.getNivel());
        assertEquals(OrigenSugerencia.REPOSICION_MANUAL, alerta.getOrigenSugerencia());
        assertEquals(new BigDecimal("7.00"), alerta.getCantidadSugerida(),
                "Reposición manual: mínimo 15 - stock 8.");
        assertNull(alerta.getDiasCobertura());
    }

    @Test
    void lecheEnPolvoConStock10YUmbral15DisparaAlertaRojaYOmiteHistoricoSinSalidas() {
        Insumo lecheEnPolvo = insumo(6L, "LEC-001", "Leche en polvo", "15.00", "5.00");

        var stockLeche = stock(6L, "10.00");

        when(insumoRepository.findByActivoTrueOrderByNombreAsc()).thenReturn(List.of(lecheEnPolvo));
        when(stockInsumoRepository.stockVigentePorInsumo(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(stockLeche));
        when(salidaInsumoRepository.salidasDesde(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of());

        AlertaStockView alerta = alertaService.evaluarTodas().get(0);

        assertEquals("LEC-001", alerta.getCodigo());
        assertEquals("Leche en polvo", alerta.getNombre());
        assertEquals(NivelAlerta.ROJO, alerta.getNivel(),
                "Stock actual 10 kg <= umbral mínimo 15 kg debe disparar alerta ROJO.");
        assertEquals(new BigDecimal("5.00"), alerta.getFaltante(),
                "Faltante = 15.00 - 10.00 = 5.00 kg.");
        assertEquals(OrigenSugerencia.REPOSICION_MANUAL, alerta.getOrigenSugerencia(),
                "Sin salidas en los últimos 7 días debe omitir el cálculo histórico.");
        assertEquals(new BigDecimal("5.00"), alerta.getCantidadSugerida(),
                "Reposición manual sugerida = 15.00 - 10.00 = 5.00 kg.");
        assertNull(alerta.getDiasCobertura());
        assertNull(alerta.getFechaEstimadaQuiebre());
    }

    @Test
    void insumoSanitarioSinLotesNiUmbralNoRompeElCalculo() {
        Insumo sal = insumo(5L, "SAL-001", "Sal industrial", null, null);

        when(insumoRepository.findByActivoTrueOrderByNombreAsc()).thenReturn(List.of(sal));
        when(stockInsumoRepository.stockVigentePorInsumo(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of());
        when(salidaInsumoRepository.salidasDesde(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of());

        AlertaStockView alerta = alertaService.evaluarTodas().get(0);

        assertEquals(NivelAlerta.SIN_UMBRAL, alerta.getNivel());
        assertEquals(0, alerta.getStockActual().signum());
        assertEquals(0, alerta.getCantidadSugerida().signum());
        assertTrue(alerta.isSinUmbral());
    }

    private Insumo insumo(Long id, String codigo, String nombre,
                          String minimo, String critico) {
        Insumo insumo = Insumo.builder()
                .id(id)
                .codigo(codigo)
                .nombre(nombre)
                .unidadMedida(UnidadMedida.KILOGRAMO)
                .activo(Boolean.TRUE)
                .build();
        if (minimo != null) {
            insumo.setUmbral(UmbralSeguridad.builder()
                    .insumo(insumo)
                    .stockMinimo(new BigDecimal(minimo))
                    .stockCritico(new BigDecimal(critico))
                    .build());
        }
        return insumo;
    }

    private StockInsumoRepository.StockPorInsumo stock(Long idInsumo, String stock) {
        StockInsumoRepository.StockPorInsumo proyeccion =
                mock(StockInsumoRepository.StockPorInsumo.class);
        when(proyeccion.getIdInsumo()).thenReturn(idInsumo);
        when(proyeccion.getStock()).thenReturn(new BigDecimal(stock));
        return proyeccion;
    }

    private SalidaInsumoRepository.SalidaPorInsumo salida(Long idInsumo, String salidas) {
        SalidaInsumoRepository.SalidaPorInsumo proyeccion =
                mock(SalidaInsumoRepository.SalidaPorInsumo.class);
        when(proyeccion.getIdInsumo()).thenReturn(idInsumo);
        when(proyeccion.getSalidas()).thenReturn(new BigDecimal(salidas));
        return proyeccion;
    }
}
