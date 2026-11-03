package com.panificadora.sapi.inventario.infrastructure.web;

import com.panificadora.sapi.inventario.application.dto.IngresoLoteForm;
import com.panificadora.sapi.inventario.application.service.InventarioService;
import com.panificadora.sapi.inventario.domain.exception.ReglaNegocioException;
import com.panificadora.sapi.inventario.domain.model.Movimiento;
import jakarta.validation.Valid;
import java.beans.PropertyEditorSupport;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Adaptador de entrada web del módulo Inventario.
 * Solo traduce HTTP; las reglas FIFO y transaccionales viven en el servicio.
 */
@Controller
@RequestMapping("/inventario")
@RequiredArgsConstructor
public class InventarioController {

    private final InventarioService inventarioService;

    /* ---------------------------------------------------------------- */
    /*  Ingreso de lotes                                                 */
    /* ---------------------------------------------------------------- */

    @GetMapping("/ingreso")
    public String mostrarIngreso(Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", formularioNuevo());
        }
        cargarInsumos(model);
        return "inventario/ingreso";
    }

    @PostMapping("/ingreso")
    public String registrarIngreso(@Valid @ModelAttribute("form") IngresoLoteForm form,
                                   BindingResult resultado,
                                   Model model,
                                   RedirectAttributes flash) {

        if (!resultado.hasErrors()) {
            try {
                Movimiento movimiento = inventarioService.registrarIngreso(form);
                flash.addFlashAttribute("exito",
                        "Ingreso registrado: movimiento #" + movimiento.getId()
                                + " y su lote en una sola transacción.");
                return "redirect:/inventario/lotes?idInsumo=" + form.getIdInsumo();
            } catch (ReglaNegocioException ex) {
                resultado.reject("regla.negocio", ex.getMessage());
            } catch (DataIntegrityViolationException ex) {
                resultado.reject("regla.negocio",
                        "No se pudo registrar el ingreso: los datos violan una restricción de la base.");
            }
        }

        cargarInsumos(model);
        return "inventario/ingreso";
    }

    /* ---------------------------------------------------------------- */
    /*  Consultas FIFO                                                   */
    /* ---------------------------------------------------------------- */

    @GetMapping("/lotes")
    public String listarLotes(@RequestParam(name = "idInsumo", required = false) Long idInsumo,
                              Model model) {
        model.addAttribute("lotes",
                idInsumo == null
                        ? inventarioService.listarTodosLosLotesVigentes()
                        : inventarioService.listarLotesVigentes(idInsumo));
        model.addAttribute("idInsumo", idInsumo);
        model.addAttribute("hoy", LocalDate.now());
        cargarInsumos(model);
        return "inventario/lotes";
    }

    @GetMapping("/reporte-caducidad")
    public String reporteCaducidad(@RequestParam(name = "dias", defaultValue = "30") int dias,
                                   Model model) {
        int umbral = Math.max(0, dias);
        model.addAttribute("lotes", inventarioService.generarReporteCaducidad(umbral));
        model.addAttribute("dias", umbral);
        model.addAttribute("hoy", LocalDate.now());
        return "inventario/reporte-caducidad";
    }

    /* ---------------------------------------------------------------- */
    /*  Apoyo                                                            */
    /* ---------------------------------------------------------------- */

    private IngresoLoteForm formularioNuevo() {
        IngresoLoteForm form = new IngresoLoteForm();
        form.setFechaVencimiento(LocalDate.now().plusDays(30));
        form.setIdPersonal(101L);
        return form;
    }

    private void cargarInsumos(Model model) {
        model.addAttribute("insumos", inventarioService.listarInsumos());
    }

    /** Igual que en Catálogo: evita que es_PE interprete "12.50" como 1250. */
    @InitBinder
    public void registrarBinderDecimal(WebDataBinder binder) {
        DecimalFormatSymbols simbolos = new DecimalFormatSymbols(Locale.ROOT);
        simbolos.setDecimalSeparator('.');
        DecimalFormat formato = new DecimalFormat("#0.##", simbolos);
        formato.setParseBigDecimal(true);

        binder.registerCustomEditor(BigDecimal.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String texto) {
                if (texto == null || texto.isBlank()) {
                    setValue(null);
                    return;
                }
                try {
                    setValue(formato.parse(texto.trim()));
                } catch (ParseException ex) {
                    setValue(null);
                }
            }

            @Override
            public String getAsText() {
                Object valor = getValue();
                return valor == null ? "" : formato.format(valor);
            }
        });
    }
}
