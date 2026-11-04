package com.panificadora.sapi.despacho.infrastructure.web;

import com.panificadora.sapi.despacho.application.DespachoService;
import com.panificadora.sapi.despacho.domain.Despacho;
import com.panificadora.sapi.inventario.application.service.InventarioService;
import com.panificadora.sapi.inventario.domain.exception.ReglaNegocioException;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Adaptador de entrada del módulo Despacho. Solo traduce HTTP y delega en el
 * caso de uso; el stock ya no se recibe como parámetro desde la vista.
 */
@Controller
@RequestMapping("/despacho")
@RequiredArgsConstructor
public class DespachoController {

    private final DespachoService despachoService;
    private final InventarioService inventarioService;

    @GetMapping
    public String mostrarFormulario(Model model) {
        cargarInsumos(model);
        return "despacho/formulario";
    }

    @PostMapping("/registrar")
    public String registrarSalida(@RequestParam Long idInsumo,
                                  @RequestParam BigDecimal cantidad,
                                  @RequestParam(defaultValue = "101") Long idPersonal,
                                  RedirectAttributes flash) {
        try {
            Despacho despacho = despachoService.registrarSalida(idInsumo, cantidad, idPersonal);
            flash.addFlashAttribute("mensajeExito",
                    "Salida registrada: movimiento #" + despacho.getMovimiento().getId()
                            + " por " + despacho.getCantidad().stripTrailingZeros().toPlainString()
                            + " unidades de " + despacho.getInsumo().getNombre() + ".");
        } catch (ReglaNegocioException | IllegalArgumentException ex) {
            flash.addFlashAttribute("mensajeError", ex.getMessage());
        }
        return "redirect:/despacho";
    }

    private void cargarInsumos(Model model) {
        model.addAttribute("insumos", inventarioService.listarInsumos());
        model.addAttribute("stockPorInsumo", inventarioService.stockVigentePorInsumo());
    }
}
