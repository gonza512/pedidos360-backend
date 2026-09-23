package cl.pedidos360.common.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

/**
 * Responde 401 con error JSON cuando un recurso protegido es consumido
 * sin token (o el filtro nunca llego a autenticar la peticion).
 */
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        SecurityErrorWriter.write(response, 401, "unauthorized",
                "Autenticacion requerida: se espera un token JWT valido en la cabecera Authorization",
                request.getRequestURI());
    }
}
