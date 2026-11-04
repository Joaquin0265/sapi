package com.panificadora.sapi.despacho;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.panificadora.sapi.catalogo.domain.model.Categoria;
import com.panificadora.sapi.catalogo.domain.model.Insumo;
import com.panificadora.sapi.catalogo.domain.model.UnidadMedida;
import com.panificadora.sapi.catalogo.infrastructure.persistence.CategoriaRepository;
import com.panificadora.sapi.catalogo.infrastructure.persistence.InsumoRepository;
import com.panificadora.sapi.despacho.application.DespachoService;
import com.panificadora.sapi.despacho.domain.Despacho;
import com.panificadora.sapi.inventario.domain.model.Lote;
import com.panificadora.sapi.inventario.domain.model.Movimiento;
import com.panificadora.sapi.inventario.domain.model.TipoMovimiento;
import com.panificadora.sapi.inventario.infrastructure.persistence.LoteRepository;
import com.panificadora.sapi.inventario.infrastructure.persistence.MovimientoRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integración del despacho: cabecera EGRESO + detalle DESPACHO + descuento del
 * lote, todo en la misma transacción.
 */
@SpringBootTest
@ActiveProfiles("demo")
@Transactional
class DespachoIntegrationTest {

    private static final String CODIGO_INSUMO = "TEST-DSP-001";

    @Autowired
    private DespachoService despachoService;

    @Autowired
    private InsumoRepository insumoRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private LoteRepository loteRepository;

    @Autowired
    private MovimientoRepository movimientoRepository;

    private Insumo insumo;

    @BeforeEach
    void prepararInsumoConLote() {
        Categoria insumos = categoriaRepository.findByNombreIgnoreCase("Insumos")
                .orElseGet(() -> categoriaRepository.save(Categoria.builder()
                        .nombre("Insumos")
                        .descripcion("Materia prima de producción")
                        .build()));

        insumo = insumoRepository.findByCodigoIgnoreCase(CODIGO_INSUMO)
                .orElseGet(() -> insumoRepository.save(Insumo.builder()
                        .codigo(CODIGO_INSUMO)
                        .nombre("Insumo de prueba de despacho")
                        .categoria(insumos)
                        .unidadMedida(UnidadMedida.KILOGRAMO)
                        .build()));

        Movimiento ingreso = movimientoRepository.save(Movimiento.builder()
                .tipoMovimiento(TipoMovimiento.INGRESO)
                .idPersonal(101L)
                .observacion("Stock para prueba de despacho")
                .build());

        loteRepository.save(Lote.builder()
                .movimiento(ingreso)
                .insumo(insumo)
                .codigoLote("L-DSP-001")
                .cantidad(new BigDecimal("20.00"))
                .cantidadDisponible(new BigDecimal("20.00"))
                .fechaVencimiento(LocalDate.now().plusDays(30))
                .build());
    }

    @Test
    void registrarSalidaDebePersistirCronometriaPersonalYDescontarStock() {
        Despacho salida = despachoService.registrarSalida(
                insumo.getId(), new BigDecimal("10.00"), 105L);

        assertNotNull(salida.getId(), "El detalle debe tener ID autogenerado.");
        assertNotNull(salida.getFechaHora(), "Debe registrarse la cronometría exacta.");
        assertEquals(105L, salida.getMovimiento().getIdPersonal());
        assertEquals(TipoMovimiento.EGRESO, salida.getMovimiento().getTipoMovimiento());
        assertEquals(insumo.getId(), salida.getInsumo().getId());

        BigDecimal disponible = loteRepository.findDisponiblesPorInsumo(insumo.getId())
                .get(0).getCantidadDisponible();
        assertEquals(0, disponible.compareTo(new BigDecimal("10.00")),
                "El lote debe quedar con 10 unidades disponibles.");
    }
}
