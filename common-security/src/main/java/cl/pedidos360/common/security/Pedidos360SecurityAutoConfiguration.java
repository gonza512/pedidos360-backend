package cl.pedidos360.common.security;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

/**
 * Auto-configuracion que expone el filtro de validacion JWT y los manejadores
 * de error (401/403) para que cada microservicio y el BFF solo tengan que
 * registrarlos en su cadena de seguridad.
 *
 * Depende del decoder que arma Spring Boot a partir de
 * {@code spring.security.oauth2.resourceserver.jwt.issuer-uri}, el cual
 * resuelve las claves JWKS del IDaaS y valida issuer, firma y vigencia.
 */
@AutoConfiguration(after = OAuth2ResourceServerAutoConfiguration.class)
@EnableConfigurationProperties(Pedidos360SecurityProperties.class)
public class Pedidos360SecurityAutoConfiguration {

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtDecoder decoder,
                                                           Pedidos360SecurityProperties properties) {
        return new JwtAuthenticationFilter(decoder, properties);
    }

    @Bean
    public AuthenticationEntryPoint jwtAuthenticationEntryPoint() {
        return new JwtAuthenticationEntryPoint();
    }

    @Bean
    public AccessDeniedHandler jwtAccessDeniedHandler() {
        return new JwtAccessDeniedHandler();
    }
}
