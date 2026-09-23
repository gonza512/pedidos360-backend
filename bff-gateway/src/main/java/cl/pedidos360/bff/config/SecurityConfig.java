package cl.pedidos360.bff.config;

import cl.pedidos360.bff.proxy.BffProxyFilter;
import cl.pedidos360.common.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Cadena de seguridad del BFF:
 *
 * <ol>
 *   <li>{@link JwtAuthenticationFilter} - valida el JWT (firma, issuer,
 *       vigencia, audience) y carga los roles desde los claims.</li>
 *   <li>Autorizacion por rol - GET autenticado; altas de pedidos solo
 *       usuarios autenticados; mantenimiento de productos/clientes y
 *       modificacion/eliminacion de pedidos solo rol ADMIN.</li>
 *   <li>{@link BffProxyFilter} - se ejecuta una vez autorizada la peticion
 *       y la reenvia al microservicio interno.</li>
 * </ol>
 *
 * Sin token -> 401; token valido sin rol -> 403; ambos en formato JSON.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AuthenticationEntryPoint authenticationEntryPoint;
    private final AccessDeniedHandler accessDeniedHandler;
    private final BffProxyFilter bffProxyFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          AuthenticationEntryPoint authenticationEntryPoint,
                          AccessDeniedHandler accessDeniedHandler,
                          BffProxyFilter bffProxyFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
        this.bffProxyFilter = bffProxyFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health").permitAll()
                        // Lectura: cualquier usuario autenticado
                        .requestMatchers(HttpMethod.GET, "/api/**").authenticated()
                        // Altas: cualquier usuario autenticado puede generar pedidos
                        .requestMatchers(HttpMethod.POST, "/api/pedidos", "/api/pedidos/**").authenticated()
                        // Mantenimiento de catalogo y clientes: solo ADMIN
                        .requestMatchers(HttpMethod.POST, "/api/productos/**", "/api/clientes/**").hasRole("ADMIN")
                        // Modificar/eliminar: solo ADMIN
                        .requestMatchers(HttpMethod.PUT, "/api/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(jwtAuthenticationFilter, AuthorizationFilter.class)
                .addFilterAfter(bffProxyFilter, AuthorizationFilter.class);
        return http.build();
    }

    /**
     * Permite el consumo del API desde el frontend Angular (dev y prod).
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${cors.allowed-origins}") List<String> allowedOrigins) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
