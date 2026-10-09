package com.tecup.exam_1.config;

import com.tecup.exam_1.model.Usuario;
import com.tecup.exam_1.seguridad.SeguridadService;
import com.tecup.exam_1.service.RolService;
import com.tecup.exam_1.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UsuarioService usuarioService;
    private final RolService rolService;
    private final SeguridadService seguridadService;

    public LoginSuccessHandler(UsuarioService usuarioService,
                               RolService rolService,
                               SeguridadService seguridadService) {
        this.usuarioService = usuarioService;
        this.rolService = rolService;
        this.seguridadService = seguridadService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        Usuario usuario = usuarioService.obtenerPorUsername(authentication.getName());
        seguridadService.iniciarSesion(request.getSession(true), usuario, rolService.clavesPermisos(usuario.getRol()));
        response.sendRedirect(request.getContextPath() + "/dashboard");
    }
}