package cl.pedidos360.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Filtro que extrae el Bearer token de la cabecera Authorization y lo valida
 * contra el IDaaS (Amazon Cognito) usando el {@link org.springframework.security.oauth2.jwt.JwtDecoder}
 * de Spring Security, el cual verifica:
 *
 * <ul>
 *   <li><b>firma</b> del token (claves JWKS publicas del IDaaS),</li>
 *   <li><b>issuer</b> (que lo emitio el IDaaS configurado),</li>
 *   <li><b>vigencia</b> (claims exp / nbf / iat).</li>
 * </ul>
 *
 * Ademas de lo anterior, este filtro valida el <b>audience</b> esperado y
 * extrae los <b>roles</b> desde el claim configurado para autorizar por rol.
 *
 * Ante cualquier falla responde con un error JSON y codigo 401.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final org.springframework.security.oauth2.jwt.JwtDecoder decoder;
    private final Pedidos360SecurityProperties properties;

    public JwtAuthenticationFilter(org.springframework.security.oauth2.jwt.JwtDecoder decoder,
                                   Pedidos360SecurityProperties properties) {
        this.decoder = decoder;
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            // Sin token: se continua; si la ruta esta protegida, el EntryPoint respondera 401.
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(BEARER_PREFIX.length()).trim();
        try {
            // Verifica firma, issuer y vigencia (exp/nbf).
            Jwt jwt = decoder.decode(token);

            if (!audienceMatches(jwt)) {
                SecurityErrorWriter.write(response, 401, "unauthorized",
                        "Token con audience invalido: se esperaba '" + properties.getAudience() + "'",
                        request.getRequestURI());
                return;
            }

            JwtAuthenticationToken authentication =
                    new JwtAuthenticationToken(jwt, extractAuthorities(jwt));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            chain.doFilter(request, response);
        } catch (JwtException | IllegalArgumentException e) {
            // Firma invalida, token expirado, issuer desconocido, etc.
            SecurityErrorWriter.write(response, 401, "unauthorized",
                    "Token invalido: " + e.getMessage(), request.getRequestURI());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private boolean audienceMatches(Jwt jwt) {
        String expected = properties.getAudience();
        if (expected == null || expected.isBlank()) {
            return true;
        }
        return jwt.getAudience().contains(expected);
    }

    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        Object claim = jwt.getClaim(properties.getRolesClaim());
        List<String> groups = switch (claim) {
            case null -> List.of();
            case List<?> list -> list.stream().map(String::valueOf).toList();
            case String s -> Arrays.stream(s.split(","))
                    .map(String::trim)
                    .filter(v -> !v.isEmpty())
                    .toList();
            default -> List.of();
        };
        return groups.stream()
                .map(g -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + g.toUpperCase(Locale.ROOT)))
                .toList();
    }
}
