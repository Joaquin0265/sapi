package com.panificadora.sapi.inventario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.panificadora.sapi.catalogo.domain.model.Categoria;
import com.panificadora.sapi.catalogo.domain.model.Insumo;
import com.panificadora.sapi.catalogo.domain.model.UnidadMedida;
import com.panificadora.sapi.catalogo.infrastructure.persistence.CategoriaRepository;
import com.panificadora.sapi.catalogo.infrastructure.persistence.InsumoRepository;
import com.panificadora.sapi.inventario.application.dto.IngresoLoteForm;
import com.panificadora.sapi.inventario.application.service.InventarioService;
import com.panificadora.sapi.inventario.domain.exception.ReglaNegocioException;
import com.panificadora.sapi.inventario.domain.model.Lote;
import com.panificadora.sapi.inventario.domain.model.Movimiento;
import com.panificadora.sapi.inventario.domain.model.TipoMovimiento;
import com.panificadora.sapi.inventario.infrastructure.persistence.LoteRepository;
import com.panificadora.sapi.inventario.infrastructure.persistence.MovimientoRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Prueba de integración del registro transaccional de MOVIMIENTO (cabecera)
 * y LOTE (detalle) sobre H2 en modo Oracle.
 */
@SpringBootTest
@ActiveProfiles("demo")
class IngresoLoteIntegrationTest {

    private static final String CODIGO_INSUMO_PRUEBA = "TEST-INS-001";

    @Autowired
    private InventarioService inventarioService;

    @Autowired
    private MovimientoRepository movimientoRepository;

    @Autowired
    private LoteRepository loteRepository;

    @Autowired
    private InsumoRepository insumoRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    private Insumo insumo;

    @BeforeEach
    void prepararInsumo() {
        Categoria insumos = categoriaRepository.findByNombreIgnoreCase("Insumos")
                .orElseGet(() -> categoriaRepository.save(Categoria.builder()
                        .nombre("Insumos")
                        .descripcion("Materia prima de producción")
                        .build()));

        insumo = insumoRepository.findByCodigoIgnoreCase(CODIGO_INSUMO_PRUEBA)
                .orElseGet(() -> insumoRepository.save(Insumo.builder()
                        .codigo(CODIGO_INSUMO_PRUEBA)
                        .nombre("Insumo de prueba transaccional")
                        .descripcion("Creado por la prueba de integración")
                        .categoria(insumos)
                        .unidadMedida(UnidadMedida.KILOGRAMO)
                        .build()));
    }

    @Test
    void registrarIngresoDebeGuardarMovimientoYLoteEnUnaSolaTransaccion() {
        long movimientosAntes = movimientoRepository.count();
        long lotesAntes = loteRepository.count();

        IngresoLoteForm form = new IngresoLoteForm();
        form.setIdInsumo(insumo.getId());
        form.setCodigoLote("L-INT-001");
        form.setCantidad(new BigDecimal("25.50"));
        form.setCostoUnitario(new BigDecimal("2.45"));
        form.setFechaVencimiento(LocalDate.now().plusDays(45));
        form.setObservacion("Ingreso de prueba de integración");
        form.setIdPersonal(105L);

        Movimiento movimiento = inventarioService.registrarIngreso(form);

        assertNotNull(movimiento.getId(), "La cabecera debe tener ID autogenerado.");
        assertEquals(TipoMovimiento.INGRESO, movimiento.getTipoMovimiento());
        assertNotNull(movimiento.getFechaMovimiento());
        assertEquals(105L, movimiento.getIdPersonal());

        assertTrue(movimientoRepository.findById(movimiento.getId()).isPresent(),
                "La cabecera MOVIMIENTO debe estar persistida.");

        List<Lote> lotesDelMovimiento = loteRepository.findByMovimientoId(movimiento.getId());
        assertEquals(1, lotesDelMovimiento.size(),
                "Cada ingreso debe generar exactamente un lote de detalle.");

        Lote lote = lotesDelMovimiento.get(0);
        assertNotNull(lote.getId());
        assertEquals(movimiento.getId(), lote.getMovimiento().getId());
        assertEquals(insumo.getId(), lote.getInsumo().getId());
        assertEquals("L-INT-001", lote.getCodigoLote());
        assertEquals(0, lote.getCantidad().compareTo(new BigDecimal("25.50")));
        assertEquals(0, lote.getCantidadDisponible().compareTo(new BigDecimal("25.50")));
        assertEquals(LocalDate.now().plusDays(45), lote.getFechaVencimiento());
        assertNotNull(lote.getFechaIngreso());

        assertEquals(movimientosAntes + 1, movimientoRepository.count());
        assertEquals(lotesAntes + 1, loteRepository.count());
    }

    @Test
    void registrarIngresoConInsumoInexistenteNoDebePersistirCabecera() {
        long movimientosAntes = movimientoRepository.count();

        IngresoLoteForm form = new IngresoLoteForm();
        form.setIdInsumo(999_999L);
        form.setCantidad(new BigDecimal("10.00"));
        form.setFechaVencimiento(LocalDate.now().plusDays(15));

        assertThrows(ReglaNegocioException.class,
                () -> inventarioService.registrarIngreso(form));

        assertEquals(movimientosAntes, movimientoRepository.count(),
                "Si el detalle no puede registrarse, la cabecera no debe quedar huérfana.");
        assertFalse(movimientoRepository.findById(999_999L).isPresent());
    }
}
