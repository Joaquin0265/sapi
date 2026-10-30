package com.panificadora.sapi.shared.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Redirección raíz. Cuando existan los 5 módulos, este controlador pasa a ser
 * el "dashboard" del sistema.
 */
@Controller
public class HomeController {

    @GetMapping("/")
    public String inicio() {
        return "redirect:/catalogo";
    }
}
