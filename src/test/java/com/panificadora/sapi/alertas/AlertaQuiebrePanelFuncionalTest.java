package com.panificadora.sapi.alertas;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.panificadora.sapi.alertas.application.dto.AlertaStockView;
import com.panificadora.sapi.alertas.application.service.AlertaService;
import com.panificadora.sapi.alertas.domain.model.NivelAlerta;
import com.panificadora.sapi.alertas.domain.model.OrigenSugerencia;
import com.panificadora.sapi.catalogo.domain.model.Categoria;
import com.panificadora.sapi.catalogo.domain.model.Insumo;
import com.panificadora.sapi.catalogo.domain.model.UmbralSeguridad;
import com.panificadora.sapi.catalogo.domain.model.UnidadMedida;
import com.panificadora.sapi.catalogo.infrastructure.persistence.CategoriaRepository;
import com.panificadora.sapi.catalogo.infrastructure.persistence.InsumoRepository;
import com.panificadora.sapi.catalogo.infrastructure.persistence.UmbralSeguridadRepository;
import com.panificadora.sapi.inventario.domain.model.Lote;
import com.panificadora.sapi.inventario.domain.model.Movimiento;
import com.panificadora.sapi.inventario.domain.model.TipoMovimiento;
import com.panificadora.sapi.inventario.infrastructure.persistence.LoteRepository;
import com.panificadora.sapi.inventario.infrastructure.persistence.MovimientoRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Prueba funcional / de aceptación de RF-04 con el insumo
 * <b>LEC-001 / Leche en polvo</b>.
 *
 * <p>Se provoca artificialmente un quiebre de stock (10 kg disponibles frente a
 * un umbral mínimo de 15 kg) y se valida que el administrador vea la alerta
 * semaforizada en rojo de manera inmediata en su panel, sin revisar el almacén
 * ni reiniciar la aplicación.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
@Transactional
class AlertaQuiebrePanelFuncionalTest {

    private static final String CODIGO = "LEC-001";
    private static final String NOMBRE = "Leche en polvo";
    private static final BigDecimal STOCK_ACTUAL = new BigDecimal("10.00");
    private static final BigDecimal UMBRAL_MINIMO = new BigDecimal("15.00");
    private static final BigDecimal STOCK_CRITICO = new BigDecimal("5.00");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AlertaService alertaService;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private InsumoRepository insumoRepository;

    @Autowired
    private UmbralSeguridadRepository umbralSeguridadRepository;

    @Autowired
    private MovimientoRepository movimientoRepository;

    @Autowired
    private LoteRepository loteRepository;

    @BeforeEach
    void provocarQuiebreArtificialDeLecheEnPolvo() {
        Categoria insumos = categoriaRepository.findByNombreIgnoreCase("Insumos")
                .orElseGet(() -> categoriaRepository.save(Categoria.builder()
                        .nombre("Insumos")
                        .descripcion("Materia prima de producción")
                        .build()));

        Insumo leche = insumoRepository.findByCodigoIgnoreCase(CODIGO)
                .orElseGet(() -> insumoRepository.save(Insumo.builder()
                        .codigo(CODIGO)
                        .nombre(NOMBRE)
                        .descripcion("Insumo de prueba para validar el quiebre de stock")
                        .categoria(insumos)
                        .unidadMedida(UnidadMedida.KILOGRAMO)
                        .build()));

        if (!umbralSeguridadRepository.existsByInsumoId(leche.getId())) {
            UmbralSeguridad umbral = umbralSeguridadRepository.save(
                    UmbralSeguridad.builder()
                            .insumo(leche)
                            .stockMinimo(UMBRAL_MINIMO)
                            .stockCritico(STOCK_CRITICO)
                            .build());
            leche.setUmbral(umbral);
        }

        if (loteRepository.findDisponiblesPorInsumo(leche.getId()).isEmpty()) {
            Movimiento ingreso = movimientoRepository.save(Movimiento.builder()
                    .tipoMovimiento(TipoMovimiento.INGRESO)
                    .idPersonal(101L)
                    .observacion("Stock artificial para la prueba de quiebre")
                    .build());

            loteRepository.save(Lote.builder()
                    .movimiento(ingreso)
                    .insumo(leche)
                    .codigoLote("L-LEC-QUIEBRE")
                    .cantidad(STOCK_ACTUAL)
                    .cantidadDisponible(STOCK_ACTUAL)
                    .costoUnitario(new BigDecimal("3.00"))
                    .fechaVencimiento(LocalDate.now().plusDays(30))
                    .build());
        }
    }

    @Test
    @WithMockUser(authorities = "ADMINISTRADOR")
    @DisplayName("El administrador ve el rojo de LEC-001 de inmediato en /alertas")
    void elAdministradorVeLaAlertaRojaDeInmediato() throws Exception {
        AlertaStockView leche = alertaService.evaluarTodas().stream()
                .filter(alerta -> CODIGO.equals(alerta.getCodigo()))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "LEC-001 debe aparecer en el panel de alertas."));

        assertEquals(NivelAlerta.ROJO, leche.getNivel(),
                "10 kg <= 15 kg: el semáforo debe estar en rojo.");
        assertEquals(0, leche.getStockActual().compareTo(STOCK_ACTUAL),
                "El stock artificial debe ser 10 kg.");
        assertEquals(0, leche.getStockMinimo().compareTo(UMBRAL_MINIMO),
                "El umbral mínimo debe ser 15 kg.");
        assertEquals(0, leche.getFaltante().compareTo(new BigDecimal("5")),
                "Faltan 5 kg para volver al mínimo.");
        assertEquals(OrigenSugerencia.REPOSICION_MANUAL, leche.getOrigenSugerencia(),
                "Sin salidas en 7 días la sugerencia debe ser manual.");
        assertNotNull(leche.getCantidadSugerida(),
                "El panel debe indicar cuánto comprar.");

        mockMvc.perform(get("/alertas"))
                .andExpect(status().isOk())
                .andExpect(view().name("alertas/panel"))
                .andExpect(content().string(containsString(CODIGO)))
                .andExpect(content().string(containsString(NOMBRE)))
                .andExpect(content().string(containsString("Quiebre")))
                .andExpect(content().string(containsString("text-bg-danger")))
                .andExpect(content().string(containsString("Reposición manual estándar")));
    }

    @Test
    @WithMockUser(authorities = "ADMINISTRADOR")
    @DisplayName("El inicio del administrador anuncia que hay alertas rojas")
    void elInicioDelAdministradorAnunciaLasAlertasRojas() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("shared/dashboard"))
                .andExpect(content().string(containsString("Ver alertas rojas")));
    }

    @Test
    @WithMockUser(authorities = "OPERARIO_ALMACEN")
    @DisplayName("El operario de almacén no puede ver el panel de alertas")
    void elOperarioNoAccedeAlPanelDeAlertas() throws Exception {
        mockMvc.perform(get("/alertas"))
                .andExpect(status().isForbidden());
    }
}
