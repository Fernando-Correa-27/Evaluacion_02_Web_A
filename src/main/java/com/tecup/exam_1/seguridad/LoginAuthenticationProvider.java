package com.tecup.exam_1.seguridad;

import com.tecup.exam_1.exception.NegocioException;
import com.tecup.exam_1.model.Usuario;
import com.tecup.exam_1.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashSet;
import java.util.Set;

@Component
public class LoginAuthenticationProvider implements AuthenticationProvider {

    private final AuthService authService;

    public LoginAuthenticationProvider(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String identificador = authentication.getName();
        String password = authentication.getCredentials() == null ? "" : authentication.getCredentials().toString();
        try {
            Usuario usuario = authService.login(identificador, password, ipActual());
            return new UsernamePasswordAuthenticationToken(
                    usuario.getUsername(), null, autoridadesDe(usuario));
        } catch (NegocioException ex) {
            throw new BadCredentialsException(ex.getMessage());
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private Set<GrantedAuthority> autoridadesDe(Usuario usuario) {
        Set<GrantedAuthority> autoridades = new HashSet<>();
        if (usuario.getRol() != null) {
            autoridades.add(new SimpleGrantedAuthority(
                    "ROLE_" + usuario.getRol().getNombre().toUpperCase().replace(" ", "_")));
            if (usuario.getRol().getPermisos() != null) {
                usuario.getRol().getPermisos().forEach(permiso -> autoridades.add(new SimpleGrantedAuthority(
                        "PERMISO:" + permiso.getModulo().toUpperCase() + ":" + permiso.getNombre().toUpperCase())));
            }
        }
        return autoridades;
    }

    private String ipActual() {
        try {
            RequestAttributes atributos = RequestContextHolder.getRequestAttributes();
            if (atributos instanceof ServletRequestAttributes sra) {
                HttpServletRequest request = sra.getRequest();
                String ip = request.getHeader("X-Forwarded-For");
                if (ip == null || ip.isBlank()) {
                    ip = request.getRemoteAddr();
                }
                return ip != null && ip.contains(",") ? ip.split(",")[0].trim() : ip;
            }
        } catch (Exception ex) {
            return null;
        }
        return null;
    }
}