package com.tecup.exam_1.controller.web;

import com.tecup.exam_1.seguridad.SeguridadService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

    private final SeguridadService seguridadService;

    public LoginController(SeguridadService seguridadService) {
        this.seguridadService = seguridadService;
    }

    @GetMapping("/login")
    public String mostrarLogin(@RequestParam(required = false) String error,
                               @RequestParam(required = false) String salida,
                               HttpSession session,
                               Model model) {
        if (seguridadService.estaAutenticado(session)) {
            return "redirect:/dashboard";
        }
        if (session != null) {
            Object mensajeError = session.getAttribute("loginError");
            if (mensajeError != null) {
                session.removeAttribute("loginError");
                model.addAttribute("error", mensajeError);
            }
        }
        if (error != null && !model.containsAttribute("error")) {
            model.addAttribute("error", "Usuario o contraseña incorrectos");
        }
        if (salida != null) {
            model.addAttribute("exito", "La sesión se cerró correctamente");
        }
        return "login";
    }
}