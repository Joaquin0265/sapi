package com.panificadora.sapi.inventario;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
 * Humo de las vistas Thymeleaf del módulo: comprueba que las plantillas
 * renderizan con datos reales del perfil demo.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class InventarioWebIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(authorities = "OperarioAlmacen")
    void formularioDeIngresoRenderiza() throws Exception {
        mockMvc.perform(get("/inventario/ingreso"))
                .andExpect(status().isOk())
                .andExpect(view().name("inventario/ingreso"));
    }

    @Test
    @WithMockUser(authorities = "OperarioAlmacen")
    void listadoFifoRenderiza() throws Exception {
        mockMvc.perform(get("/inventario/lotes"))
                .andExpect(status().isOk())
                .andExpect(view().name("inventario/lotes"));
    }

    @Test
    @WithMockUser(authorities = "OperarioAlmacen")
    void reporteDeCaducidadRenderiza() throws Exception {
        mockMvc.perform(get("/inventario/reporte-caducidad").param("dias", "30"))
                .andExpect(status().isOk())
                .andExpect(view().name("inventario/reporte-caducidad"));
    }
}
