package cl.pedidos360.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Escribe la respuesta de error de seguridad en formato JSON,
 * con codigos HTTP adecuados (401 / 403 / 502).
 */
public final class SecurityErrorWriter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private SecurityErrorWriter() {
    }

    public static void write(HttpServletResponse response, int status, String error, String message, String path)
            throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status);
        body.put("error", error);
        body.put("message", message == null ? "" : message);
        body.put("path", path == null ? "" : path);

        MAPPER.writeValue(response.getOutputStream(), body);
    }
}
