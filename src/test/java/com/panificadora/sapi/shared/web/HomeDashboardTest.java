package com.panificadora.sapi.shared.web;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * El administrador recibe el aviso rojo en su inicio; el operario va directo a
 * despacho (antes el inicio lo mandaba a Catálogo, donde recibía 403).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class HomeDashboardTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(authorities = "ADMINISTRADOR")
    void administradorVeElAvisoDeQuiebreEnElInicio() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("shared/dashboard"))
                .andExpect(content().string(containsString("Ver alertas rojas")));
    }

    @Test
    @WithMockUser(authorities = "OPERARIO_ALMACEN")
    void operarioEsRedirigidoADespacho() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/despacho"));
    }
}
