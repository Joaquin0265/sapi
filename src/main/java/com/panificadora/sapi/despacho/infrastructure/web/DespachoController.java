package com.panificadora.sapi.despacho.infrastructure.web;

import com.panificadora.sapi.despacho.application.DespachoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/despacho")
@RequiredArgsConstructor
public class DespachoController {

    private final DespachoService despachoService;

    @GetMapping
    public String mostrarFormulario() {
        return "despacho/formulario"; 
    }

    @PostMapping("/registrar")
    public String registrarSalida(@RequestParam String codigoProducto, 
                                  @RequestParam Double cantidad, 
                                  @RequestParam Long idPersonal, 
                                  Model model) {
        try {
            // Para este MVP asumimos un stock actual de consulta rápida (ej. 100 kg)
            // En integración real, esto llamaría al módulo de Inventario.
            Double stockActualBD = 100.0; 
            
            despachoService.registrarSalida(codigoProducto, cantidad, idPersonal, stockActualBD);
            model.addAttribute("mensajeExito", "Salida registrada al instante.");
        } catch (IllegalArgumentException e) {
            model.addAttribute("mensajeError", e.getMessage());
        }
        return "despacho/formulario";
    }
}