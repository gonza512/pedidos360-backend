package cl.pedidos360.common.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;

/**
 * Responde 403 con error JSON cuando el token es valido pero el rol del
 * usuario no autoriza el acceso al recurso.
 */
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        SecurityErrorWriter.write(response, 403, "forbidden",
                "No tiene permisos (rol insuficiente) para acceder a este recurso",
                request.getRequestURI());
    }
}
