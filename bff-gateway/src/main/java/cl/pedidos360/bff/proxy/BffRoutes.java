package cl.pedidos360.bff.proxy;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Rutas internas del BFF: recurso consumido por el frontend -> URL base
 * del microservicio que lo implementa.
 *
 * Configurado en application.yml bajo {@code bff.routes}.
 */
@ConfigurationProperties(prefix = "bff")
public class BffRoutes {

    private Map<String, String> routes = new HashMap<>();

    public Map<String, String> getRoutes() {
        return routes;
    }

    public void setRoutes(Map<String, String> routes) {
        this.routes = routes;
    }
}
