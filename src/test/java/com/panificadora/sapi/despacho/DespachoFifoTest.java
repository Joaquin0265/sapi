package com.panificadora.sapi.despacho;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.panificadora.sapi.catalogo.domain.model.Categoria;
import com.panificadora.sapi.catalogo.domain.model.Insumo;
import com.panificadora.sapi.catalogo.domain.model.UnidadMedida;
import com.panificadora.sapi.catalogo.infrastructure.persistence.CategoriaRepository;
import com.panificadora.sapi.catalogo.infrastructure.persistence.InsumoRepository;
import com.panificadora.sapi.despacho.application.DespachoService;
import com.panificadora.sapi.despacho.infrastructure.DespachoRepository;
import com.panificadora.sapi.inventario.domain.exception.ReglaNegocioException;
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
 * Verifica RN-01 en la práctica: un retiro consume primero el lote que vence
 * antes y, si no alcanza, no descuenta nada ni registra el despacho.
 */
@SpringBootTest
@ActiveProfiles("demo")
@Transactional
class DespachoFifoTest {

    private static final String CODIGO_INSUMO = "TEST-FIFO-001";

    @Autowired
    private DespachoService despachoService;

    @Autowired
    private DespachoRepository despachoRepository;

    @Autowired
    private InsumoRepository insumoRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private LoteRepository loteRepository;

    @Autowired
    private MovimientoRepository movimientoRepository;

    private Insumo insumo;
    private Lote vencePrimero;
    private Lote venceDespues;

    @BeforeEach
    void prepararDosLotes() {
        Categoria insumos = categoriaRepository.findByNombreIgnoreCase("Insumos")
                .orElseGet(() -> categoriaRepository.save(Categoria.builder()
                        .nombre("Insumos")
                        .descripcion("Materia prima de producción")
                        .build()));

        insumo = insumoRepository.findByCodigoIgnoreCase(CODIGO_INSUMO)
                .orElseGet(() -> insumoRepository.save(Insumo.builder()
                        .codigo(CODIGO_INSUMO)
                        .nombre("Insumo de prueba FIFO")
                        .categoria(insumos)
                        .unidadMedida(UnidadMedida.KILOGRAMO)
                        .build()));

        Movimiento ingreso = movimientoRepository.save(Movimiento.builder()
                .tipoMovimiento(TipoMovimiento.INGRESO)
                .idPersonal(101L)
                .observacion("Stock FIFO de prueba")
                .build());

        LocalDate hoy = LocalDate.now();
        vencePrimero = loteRepository.save(Lote.builder()
                .movimiento(ingreso)
                .insumo(insumo)
                .codigoLote("L-FIFO-01")
                .cantidad(new BigDecimal("10.00"))
                .cantidadDisponible(new BigDecimal("10.00"))
                .fechaVencimiento(hoy.plusDays(2))
                .build());

        venceDespues = loteRepository.save(Lote.builder()
                .movimiento(ingreso)
                .insumo(insumo)
                .codigoLote("L-FIFO-02")
                .cantidad(new BigDecimal("10.00"))
                .cantidadDisponible(new BigDecimal("10.00"))
                .fechaVencimiento(hoy.plusDays(20))
                .build());
    }

    @Test
    void descuentaPrimeroElLoteQueVenceAntes() {
        despachoService.registrarSalida(insumo.getId(), new BigDecimal("15.00"), 101L);

        BigDecimal primero = loteRepository.findById(vencePrimero.getId())
                .orElseThrow().getCantidadDisponible();
        BigDecimal segundo = loteRepository.findById(venceDespues.getId())
                .orElseThrow().getCantidadDisponible();

        assertEquals(0, primero.compareTo(new BigDecimal("0.00")),
                "El lote que vence antes debe agotarse primero.");
        assertEquals(0, segundo.compareTo(new BigDecimal("5.00")),
                "El excedente sale del lote con vencimiento posterior.");
    }

    @Test
    void stockInsuficienteNoDescuentaNiRegistraDespacho() {
        long despachosAntes = despachoRepository.count();

        assertThrows(ReglaNegocioException.class,
                () -> despachoService.registrarSalida(
                        insumo.getId(), new BigDecimal("999.00"), 101L));

        assertEquals(0, loteRepository.findById(vencePrimero.getId())
                        .orElseThrow().getCantidadDisponible()
                        .compareTo(new BigDecimal("10.00")),
                "No debe descontarse ningún lote.");
        assertEquals(0, loteRepository.findById(venceDespues.getId())
                        .orElseThrow().getCantidadDisponible()
                        .compareTo(new BigDecimal("10.00")),
                "No debe descontarse ningún lote.");
        assertEquals(despachosAntes, despachoRepository.count(),
                "No debe quedar un despacho huérfano.");
    }
}
