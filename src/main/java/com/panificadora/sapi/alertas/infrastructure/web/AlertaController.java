package com.panificadora.sapi.alertas.infrastructure.web;

import com.panificadora.sapi.alertas.application.dto.ResumenAlertas;
import com.panificadora.sapi.alertas.application.service.AlertaService;
import com.panificadora.sapi.alertas.domain.model.NivelAlerta;
import com.panificadora.sapi.alertas.domain.model.SugerenciaCompra;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Adaptador de entrada del panel de alertas.
 *
 * <p>RF-04: el administrador ve el semáforo de todos los insumos en una sola
 * pantalla, sin revisar existencias de memoria.</p>
 */
@Controller
@RequestMapping("/alertas")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMINISTRADOR')")
public class AlertaController {

    private final AlertaService alertaService;

    @GetMapping
    public String panel(@RequestParam(name = "nivel", required = false) NivelAlerta nivel,
                        Model model) {
        ResumenAlertas resumen = alertaService.resumir();

        model.addAttribute("resumen", resumen);
        model.addAttribute("alertas", nivel == null ? resumen.getAlertas() : alertaService.evaluarPorNivel(nivel));
        model.addAttribute("nivelFiltro", nivel);
        model.addAttribute("niveles", NivelAlerta.values());
        model.addAttribute("diasVentana", SugerenciaCompra.DIAS_VENTANA);
        model.addAttribute("horizonteDias", SugerenciaCompra.HORIZONTE_DIAS);
        return "alertas/panel";
    }
}
