package cl.pedidos360.bff.proxy;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.filter.OncePerRequestFilter;
import cl.pedidos360.common.security.SecurityErrorWriter;

import java.io.IOException;
import java.time.Duration;
import java.util.Locale;
import java.util.Set;

/**
 * Proxy del BFF: una vez autenticada y autorizada la peticion (el filtro se
 * ejecuta despues de la autorizacion de Spring Security), reenvia la llamada
 * al microservicio interno correspondiente conservando la cabecera
 * Authorization, de modo que el microservicio tambien valide el token.
 *
 * {@code /api/clientes/** -> cliente-service}
 * {@code /api/productos/** -> producto-service}
 * {@code /api/pedidos/**   -> pedido-service}
 */
public class BffProxyFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(BffProxyFilter.class);

    private static final String API_PREFIX = "/api/";
    private static final Set<String> HOP_BY_HOP = Set.of(
            "host", "connection", "keep-alive", "proxy-authenticate", "proxy-authorization",
            "te", "trailer", "transfer-encoding", "upgrade", "content-length", "accept-encoding");

    private final RestClient restClient;
    private final BffRoutes routes;

    public BffProxyFilter(RestClient restClient, BffRoutes routes) {
        this.restClient = restClient;
        this.routes = routes;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Si no hay autenticacion, no se proxya: deja que la seguridad responda 401/403.
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String uri = request.getRequestURI();
        if (!uri.startsWith(API_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String sinPrefijo = uri.substring(API_PREFIX.length());
        int idx = sinPrefijo.indexOf('/');
        String recurso = idx >= 0 ? sinPrefijo.substring(0, idx) : sinPrefijo;
        String resto = idx >= 0 ? sinPrefijo.substring(idx) : "";

        String baseUrl = routes.getRoutes().get(recurso);
        if (baseUrl == null) {
            // Recurso desconocido: se delega al servlet para que responda 404.
            filterChain.doFilter(request, response);
            return;
        }

        String query = request.getQueryString();
        String targetUrl = baseUrl + resto + (StringUtils.hasText(query) ? "?" + query : "");
        byte[] body = request.getInputStream().readAllBytes();

        try {
            ProxyResponse proxied = restClient
                    .method(HttpMethod.valueOf(request.getMethod()))
                    .uri(targetUrl)
                    .headers(headers -> request.getHeaderNames().asIterator().forEachRemaining(name -> {
                        if (!HOP_BY_HOP.contains(name.toLowerCase(Locale.ROOT))) {
                            headers.set(name, request.getHeader(name));
                        }
                    }))
                    .body(body.length > 0 ? body : null)
                    .exchange((req, res) -> {
                        byte[] payload = res.getBody() == null ? new byte[0] : res.getBody().readAllBytes();
                        return new ProxyResponse(res.getStatusCode(), res.getHeaders(), payload);
                    });

            writeBack(response, proxied, uri);
        } catch (IllegalArgumentException e) {
            SecurityErrorWriter.write(response, 400, "bad_request",
                    "Metodo HTTP no soportado: " + request.getMethod(), uri);
        } catch (RestClientException e) {
            log.error("No se pudo contactar el servicio interno '{}' -> {}", recurso, targetUrl, e);
            SecurityErrorWriter.write(response, 502, "bad_gateway",
                    "Servicio interno '" + recurso + "' no disponible", uri);
        }
    }

    private void writeBack(HttpServletResponse response, ProxyResponse proxied, String path) throws IOException {
        HttpStatusCode status = proxied.status();
        response.setStatus(status.value());
        proxied.headers().forEach((name, values) -> {
            String lower = name.toLowerCase(Locale.ROOT);
            if (!HOP_BY_HOP.contains(lower)) {
                values.forEach(v -> response.addHeader(name, v));
            }
        });
        response.getOutputStream().write(proxied.body());
    }

    private record ProxyResponse(HttpStatusCode status, org.springframework.http.HttpHeaders headers, byte[] body) {
    }
}
