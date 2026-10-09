package com.tecup.exam_1.aspect;

import com.tecup.exam_1.model.Usuario;
import com.tecup.exam_1.seguridad.SeguridadService;
import com.tecup.exam_1.service.AuditoriaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class AuditoriaAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaAspect.class);

    private final AuditoriaService auditoriaService;

    public AuditoriaAspect(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @Around("@annotation(registro)")
    public Object registrarOperacion(ProceedingJoinPoint joinPoint, RegistrarAuditoria registro) throws Throwable {
        long inicio = System.currentTimeMillis();
        Object resultado = joinPoint.proceed();
        long duracion = System.currentTimeMillis() - inicio;

        try {
            Usuario usuario = usuarioActual();
            if (usuario == null) {
                usuario = usuarioArgumento(joinPoint);
            }
            String detalle = descripcion(registro, joinPoint) + " (" + duracion + " ms)";
            auditoriaService.registrar(usuario, registro.accion(), registro.entidad(), detalle, ipActual());
        } catch (Exception ex) {
            log.error("No se pudo registrar la auditoría: {}", ex.getMessage());
        }
        return resultado;
    }

    private Usuario usuarioArgumento(ProceedingJoinPoint joinPoint) {
        for (Object argumento : joinPoint.getArgs()) {
            if (argumento instanceof Usuario usuario) {
                return usuario;
            }
        }
        return null;
    }

    private String descripcion(RegistrarAuditoria registro, ProceedingJoinPoint joinPoint) {
        if (registro.descripcion() != null && !registro.descripcion().isBlank()) {
            return registro.descripcion();
        }
        return "Operación sobre " + registro.entidad()
                + " (" + joinPoint.getSignature().getDeclaringType().getSimpleName()
                + "." + joinPoint.getSignature().getName() + ")";
    }

    private Usuario usuarioActual() {
        HttpSession session = sesionActual();
        if (session != null && session.getAttribute(SeguridadService.ATTR_USUARIO) instanceof Usuario usuario) {
            return usuario;
        }
        return null;
    }

    private HttpSession sesionActual() {
        try {
            RequestAttributes atributos = RequestContextHolder.getRequestAttributes();
            if (atributos instanceof ServletRequestAttributes sra) {
                return sra.getRequest().getSession(false);
            }
        } catch (Exception ex) {
            log.debug("Sin contexto de petición web: {}", ex.getMessage());
        }
        return null;
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
            log.debug("Sin contexto de petición web: {}", ex.getMessage());
        }
        return null;
    }
}