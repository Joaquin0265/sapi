package com.panificadora.sapi.alertas;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.panificadora.sapi.catalogo.domain.model.Insumo;
import com.panificadora.sapi.catalogo.infrastructure.persistence.InsumoRepository;
import com.panificadora.sapi.despacho.application.DespachoService;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Prueba funcional de RF-04 / RF-05: el administrador ve en su panel la alerta
 * semaforizada en rojo y las proyecciones, con datos reales del perfil demo.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class AlertaWebIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DespachoService despachoService;

    @Autowired
    private InsumoRepository insumoRepository;

    /**
     * Prueba funcional de RF-04: se provoca el quiebre de forma artificial
     * (retiro que deja LEV-001 bajo su mínimo) y el administrador debe ver el
     * semáforo rojo de inmediato, sin reiniciar ni recalcular nada a mano.
     */
    @Test
    @WithMockUser(authorities = "ADMINISTRADOR")
    @Transactional
    void unRetiroQueDejaElInsumoBajoElMinimoEnciendeElRojoDeInmediato() throws Exception {
        Insumo levadura = insumoRepository.findByCodigoIgnoreCase("LEV-001").orElseThrow();
        // Demo: LEV-001 tiene 40 kg y mínimo 10 kg (verde). Retirar 31 deja 9.
        despachoService.registrarSalida(levadura.getId(), new BigDecimal("31.00"), 101L);

        mockMvc.perform(get("/alertas").param("nivel", "ROJO"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("LEV-001")))
                .andExpect(content().string(containsString("Quiebre")));

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("shared/dashboard"))
                .andExpect(content().string(containsString("Ver alertas rojas")));
    }

    @Test
    @WithMockUser(authorities = "ADMINISTRADOR")
    void panelMuestraLaAlertaRojaYLaProyeccion() throws Exception {
        mockMvc.perform(get("/alertas"))
                .andExpect(status().isOk())
                .andExpect(view().name("alertas/panel"))
                .andExpect(content().string(containsString("HAR-001")))
                .andExpect(content().string(containsString("Quiebre")))
                .andExpect(content().string(containsString("Compra sugerida")))
                .andExpect(content().string(containsString("Histórico 7 días")));
    }

    @Test
    @WithMockUser(authorities = "ADMINISTRADOR")
    void filtroPorNivelRojoSoloDevuelveQuiebres() throws Exception {
        mockMvc.perform(get("/alertas").param("nivel", "ROJO"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("HAR-001")))
                .andExpect(content().string(containsString("MAN-001")));
    }

    @Test
    @WithMockUser(authorities = "OPERARIO_ALMACEN")
    void operarioNoPuedeEntrarAlPanel() throws Exception {
        mockMvc.perform(get("/alertas"))
                .andExpect(status().isForbidden());
    }
}
