package com.panificadora.sapi.inventario.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.panificadora.sapi.catalogo.infrastructure.persistence.ProductoRepository;
import com.panificadora.sapi.inventario.domain.exception.ReglaNegocioException;
import com.panificadora.sapi.inventario.domain.model.Lote;
import com.panificadora.sapi.inventario.infrastructure.persistence.LoteRepository;
import com.panificadora.sapi.inventario.infrastructure.persistence.MovimientoRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Prueba unitaria de {@code listarLotesVigentes(id_insumo)}.
 * Sin Spring ni base de datos: el repositorio se simula devolviendo los lotes
 * desordenados para comprobar que el servicio aplica estrictamente RN-01.
 */
@ExtendWith(MockitoExtension.class)
class InventarioServiceTest {

    private static final Long ID_INSUMO = 7L;

    @Mock
    private MovimientoRepository movimientoRepository;

    @Mock
    private LoteRepository loteRepository;

    @Mock
    private ProductoRepository productoRepository;

    @InjectMocks
    private InventarioService inventarioService;

    @Test
    void listarLotesVigentesDebeOrdenarPorVencimientoAscendenteYExcluirNoVigentes() {
        LocalDate hoy = LocalDate.now();

        Lote venceEnTreintaDias = lote(3L, hoy.plusDays(30), "10.00");
        Lote vencido = lote(4L, hoy.minusDays(1), "10.00");
        Lote venceEnCincoDias = lote(1L, hoy.plusDays(5), "15.00");
        Lote agotado = lote(5L, hoy.plusDays(2), "0.00");
        Lote venceEnQuinceDias = lote(2L, hoy.plusDays(15), "20.00");

        when(loteRepository.findDisponiblesPorInsumo(ID_INSUMO))
                .thenReturn(List.of(venceEnTreintaDias, vencido, venceEnCincoDias,
                        agotado, venceEnQuinceDias));

        List<Lote> resultado = inventarioService.listarLotesVigentes(ID_INSUMO);

        assertEquals(3, resultado.size(), "Solo deben quedar los lotes vigentes con saldo.");
        assertEquals(List.of(1L, 2L, 3L),
                resultado.stream().map(Lote::getId).toList(),
                "El orden debe ser estrictamente por fecha_vencimiento ascendente.");
        assertEquals(hoy.plusDays(5), resultado.get(0).getFechaVencimiento());
        assertEquals(hoy.plusDays(15), resultado.get(1).getFechaVencimiento());
        assertEquals(hoy.plusDays(30), resultado.get(2).getFechaVencimiento());
    }

    @Test
    void listarLotesVigentesSinInsumoDebeLanzarReglaDeNegocio() {
        assertThrows(ReglaNegocioException.class,
                () -> inventarioService.listarLotesVigentes(null));
    }

    private Lote lote(Long id, LocalDate vencimiento, String disponible) {
        return Lote.builder()
                .id(id)
                .fechaVencimiento(vencimiento)
                .cantidad(new BigDecimal("100.00"))
                .cantidadDisponible(new BigDecimal(disponible))
                .build();
    }
}
