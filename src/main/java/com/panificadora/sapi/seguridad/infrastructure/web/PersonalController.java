package com.panificadora.sapi.seguridad.infrastructure.web;

import com.panificadora.sapi.seguridad.application.service.PersonalService;
import com.panificadora.sapi.seguridad.domain.Personal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/personal")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMINISTRADOR')")
public class PersonalController {

    private final PersonalService personalService;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("personales", personalService.listarActivos());
        return "seguridad/personal/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("personal", new Personal());
        return "seguridad/personal/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@Valid @ModelAttribute("personal") Personal personal,
                          BindingResult resultado,
                          RedirectAttributes flash,
                          Model model) {
        if (resultado.hasErrors()) {
            return "seguridad/personal/formulario";
        }

        try {
            personalService.guardar(personal);
            flash.addFlashAttribute("exito", "Personal registrado correctamente.");
            return "redirect:/personal";
        } catch (IllegalArgumentException ex) {
            resultado.rejectValue("dni", "dni.invalid", ex.getMessage());
            return "seguridad/personal/formulario";
        }
    }

    @PostMapping("/{id}/desactivar")
    public String desactivar(@PathVariable Long id, RedirectAttributes flash) {
        try {
            personalService.desactivar(id);
            flash.addFlashAttribute("exito", "Personal inhabilitado correctamente.");
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/personal";
    }
}
