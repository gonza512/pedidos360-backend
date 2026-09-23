package cl.pedidos360.bff.config;

import cl.pedidos360.bff.proxy.BffProxyFilter;
import cl.pedidos360.bff.proxy.BffRoutes;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Configuracion del RestClient usado por el proxy del BFF para comunicarse
 * con los microservicios internos (timeouts definidos).
 */
@Configuration
@EnableConfigurationProperties(BffRoutes.class)
public class ProxyConfig {

    @Bean
    public RestClient.Builder restClientBuilder() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(15));
        return RestClient.builder().requestFactory(requestFactory);
    }

    @Bean
    public BffProxyFilter bffProxyFilter(RestClient.Builder restClientBuilder, BffRoutes routes) {
        return new BffProxyFilter(restClientBuilder.build(), routes);
    }
}
