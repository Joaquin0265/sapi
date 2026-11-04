package com.panificadora.sapi.shared.web;

import com.panificadora.sapi.alertas.application.dto.ResumenAlertas;
import com.panificadora.sapi.alertas.application.service.AlertaService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Inicio del sistema.
 *
 * <p>El administrador ve un tablero con el aviso rojo cuando algún insumo está
 * en quiebre (RF-04). El operario de almacén va directo a su pantalla de
 * despacho, que es la única que tiene permitida.</p>
 */
@Controller
@RequiredArgsConstructor
public class HomeController {

    private static final String ROL_ADMINISTRADOR = "ADMINISTRADOR";

    private final AlertaService alertaService;

    @GetMapping("/")
    public String inicio(Authentication autenticacion, Model model) {
        if (!tieneRol(autenticacion, ROL_ADMINISTRADOR)) {
            return "redirect:/despacho";
        }

        ResumenAlertas resumen = alertaService.resumir();
        model.addAttribute("resumen", resumen);
        model.addAttribute("alertasDestacadas", resumen.getAlertas().stream().limit(5).toList());
        return "shared/dashboard";
    }

    private boolean tieneRol(Authentication autenticacion, String rol) {
        return autenticacion != null && autenticacion.getAuthorities().stream()
                .anyMatch(autoridad -> rol.equals(autoridad.getAuthority()));
    }
}
