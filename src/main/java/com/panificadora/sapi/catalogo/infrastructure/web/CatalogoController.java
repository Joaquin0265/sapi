package com.panificadora.sapi.catalogo.infrastructure.web;

import com.panificadora.sapi.catalogo.application.dto.ProductoForm;
import com.panificadora.sapi.catalogo.application.dto.UmbralForm;
import com.panificadora.sapi.catalogo.application.service.CatalogoService;
import com.panificadora.sapi.catalogo.domain.exception.ReglaNegocioException;
import com.panificadora.sapi.catalogo.domain.model.Producto;
import com.panificadora.sapi.catalogo.domain.model.UmbralSeguridad;
import com.panificadora.sapi.catalogo.domain.model.UnidadMedida;
import jakarta.validation.Valid;
import java.beans.PropertyEditorSupport;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Adaptador de entrada web (Spring MVC clásico, no REST).
 *
 * <p>Responsabilidades: leer parámetros HTTP, delegar en {@link CatalogoService}
 * y elegir la vista. Cero reglas de negocio aquí.</p>
 */
@Controller
@RequestMapping("/catalogo")
@RequiredArgsConstructor
public class CatalogoController {

    private final CatalogoService catalogoService;

    /* ---------------------------------------------------------------- */
    /*  Lectura                                                          */
    /* ---------------------------------------------------------------- */

    @GetMapping
    public String listar(@RequestParam(name = "q", required = false) String q, Model model) {
        model.addAttribute("productos", catalogoService.listarProductos(q));
        model.addAttribute("filtro", q == null ? "" : q.trim());
        return "catalogo/lista";
    }

    /**
     * Pantalla del propietario: buscador de insumos + umbral vigente de cada uno.
     * El formulario de edición vive en un modal de la misma página.
     */
    @GetMapping("/insumos")
    public String listarInsumos(@RequestParam(name = "q", required = false) String q, Model model) {
        model.addAttribute("insumos", catalogoService.listarInsumos(q));
        model.addAttribute("filtro", q == null ? "" : q.trim());
        return "catalogo/insumos";
    }

    /**
     * Configura el umbral de un insumo (RF-01 / RF-02).
     *
     * <p>El controlador solo valida el formato del formulario y delega la regla
     * de negocio (crítico &lt; mínimo) en {@link CatalogoService}. Responde con
     * POST-Redirect-GET para que refrescar no reenvíe el formulario.</p>
     */
    @PostMapping("/insumos/{id}/umbral")
    public String configurarUmbral(@PathVariable Long id,
                                   @Valid @ModelAttribute("umbralForm") UmbralForm form,
                                   BindingResult resultado,
                                   @RequestParam(name = "q", required = false) String q,
                                   RedirectAttributes flash) {

        if (resultado.hasErrors()) {
            String mensaje = resultado.getAllErrors().stream()
                    .findFirst()
                    .map(ObjectError::getDefaultMessage)
                    .orElse("Revise los valores del umbral.");
            flash.addFlashAttribute("error", mensaje);
            return "redirect:/catalogo/insumos" + sufijoFiltro(q);
        }

        try {
            UmbralSeguridad umbral = catalogoService.registrarUmbral(
                    id, form.getStockMinimo(), form.getStockCritico());
            flash.addFlashAttribute("exito",
                    "Umbral actualizado. Mínimo: " + umbral.getStockMinimo().toPlainString()
                            + " · Crítico: " + umbral.getStockCritico().toPlainString() + ".");
        } catch (ReglaNegocioException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        } catch (DataIntegrityViolationException ex) {
            // Respaldo: si otra sesión viola CHK_UMBRALES, la BD también protege.
            flash.addFlashAttribute("error",
                    "No se pudo guardar el umbral: el stock crítico debe ser menor que el mínimo.");
        }
        return "redirect:/catalogo/insumos" + sufijoFiltro(q);
    }

    /* ---------------------------------------------------------------- */
    /*  Formularios                                                      */
    /* ---------------------------------------------------------------- */

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("form", catalogoService.prepararAlta());
        cargarCombos(model);
        return "catalogo/formulario";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model, RedirectAttributes flash) {
        try {
            model.addAttribute("form", catalogoService.prepararEdicion(id));
        } catch (ReglaNegocioException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
            return "redirect:/catalogo";
        }
        cargarCombos(model);
        return "catalogo/formulario";
    }

    /* ---------------------------------------------------------------- */
    /*  Comandos                                                         */
    /* ---------------------------------------------------------------- */

    @PostMapping("/guardar")
    public String guardar(@Valid @ModelAttribute("form") ProductoForm form,
                          BindingResult resultado,
                          Model model,
                          RedirectAttributes flash) {

        if (!resultado.hasErrors()) {
            try {
                if (form.esAlta()) {
                    Producto creado = catalogoService.registrar(form);
                    flash.addFlashAttribute("exito",
                            "Producto " + creado.getCodigo() + " registrado correctamente.");
                } else {
                    Producto actualizado = catalogoService.actualizar(form);
                    flash.addFlashAttribute("exito",
                            "Producto " + actualizado.getCodigo() + " actualizado correctamente.");
                }
                // Patrón POST-Redirect-GET: evita el reenvío del formulario al refrescar.
                return "redirect:/catalogo";
            } catch (ReglaNegocioException ex) {
                resultado.reject("regla.negocio", ex.getMessage());
            } catch (DataIntegrityViolationException ex) {
                // Carrera entre dos altas simultáneas: la BD gana y avisamos al usuario.
                resultado.reject("regla.negocio",
                        "No se pudo guardar: el código ya existe o los datos violan una restricción.");
            }
        }

        cargarCombos(model);
        return "catalogo/formulario";
    }

    @PostMapping("/{id}/desactivar")
    public String desactivar(@PathVariable Long id, RedirectAttributes flash) {
        try {
            catalogoService.desactivar(id);
            flash.addFlashAttribute("exito", "Producto desactivado. Se conserva su historial.");
        } catch (ReglaNegocioException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/catalogo";
    }

    @PostMapping("/{id}/reactivar")
    public String reactivar(@PathVariable Long id, RedirectAttributes flash) {
        try {
            catalogoService.reactivar(id);
            flash.addFlashAttribute("exito", "Producto reactivado correctamente.");
        } catch (ReglaNegocioException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/catalogo";
    }

    /* ---------------------------------------------------------------- */
    /*  Apoyo                                                            */
    /* ---------------------------------------------------------------- */

    private void cargarCombos(Model model) {
        model.addAttribute("categorias", catalogoService.listarCategorias());
        model.addAttribute("unidades", UnidadMedida.values());
    }

    /** Conserva el texto de búsqueda al volver del POST del modal. */
    private String sufijoFiltro(String q) {
        return (q == null || q.isBlank()) ? "" : "?q=" + URLEncoder.encode(q.trim(), StandardCharsets.UTF_8);
    }

    /**
     * Los {@code <input type="number">} envían siempre el decimal con punto
     * ("12.50"). Sin este binder, un locale es_PE interpretaría "12.50" como 1250.
     */
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

    /** Expone las etiquetas de unidad al template sin lógica en la vista. */
    @ModelAttribute("unidadesDisponibles")
    public UnidadMedida[] unidadesDisponibles() {
        return UnidadMedida.values();
    }
}
