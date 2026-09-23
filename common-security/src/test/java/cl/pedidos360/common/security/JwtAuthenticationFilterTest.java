package cl.pedidos360.common.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas basicas del filtro de validacion JWT.
 */
class JwtAuthenticationFilterTest {

    private JwtDecoder decoder;
    private Pedidos360SecurityProperties properties;
    private JwtAuthenticationFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        decoder = mock(JwtDecoder.class);
        properties = new Pedidos360SecurityProperties();
        properties.setAudience("pedidos360-client");
        properties.setRolesClaim("cognito:groups");
        filter = new JwtAuthenticationFilter(decoder, properties);
        request = new MockHttpServletRequest("GET", "/api/pedidos");
        response = new MockHttpServletResponse();
        chain = mock(FilterChain.class);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Jwt jwt(List<String> audiences, List<String> groups) {
        return new Jwt("token-value", Instant.now(), Instant.now().plusSeconds(300),
                Map.of("alg", "RS256"),
                Map.of("sub", "usuario-1",
                        "iss", "https://cognito-idp.us-east-1.amazonaws.com/us-east-1_ABC",
                        "aud", audiences,
                        "cognito:groups", groups));
    }

    @Test
    void sinTokenContinuaSinAutenticar() throws Exception {
        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void tokenValidoSeAutenticaYExtraeRolesDesdeLosClaims() throws Exception {
        when(decoder.decode("token-bueno"))
                .thenReturn(jwt(List.of("pedidos360-client"), List.of("admin")));

        Authentication[] captured = new Authentication[1];
        doAnswer(invocation -> {
            captured[0] = SecurityContextHolder.getContext().getAuthentication();
            return null;
        }).when(chain).doFilter(any(), any());

        request.addHeader("Authorization", "Bearer token-bueno");
        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(captured[0]).isInstanceOf(org.springframework.security.oauth2.server.resource
                .authentication.JwtAuthenticationToken.class);
        assertThat(captured[0].getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_ADMIN");
        assertThat(captured[0].getName()).isEqualTo("usuario-1");
    }

    @Test
    void audienceInvalidoResponde401SinContinuar() throws Exception {
        when(decoder.decode("token-otra-audience"))
                .thenReturn(jwt(List.of("otra-aplicacion"), List.of("admin")));

        request.addHeader("Authorization", "Bearer token-otra-audience");
        filter.doFilter(request, response, chain);

        verify(chain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("audience");
        assertThat(response.getContentType()).contains("application/json");
    }

    @Test
    void tokenExpiradoResponde401() throws Exception {
        when(decoder.decode("token-expirado")).thenThrow(new JwtException("Jwt expired at 2025-01-01T00:00:00Z"));

        request.addHeader("Authorization", "Bearer token-expirado");
        filter.doFilter(request, response, chain);

        verify(chain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("Token invalido");
    }

    @Test
    void firmaInvalidaResponde401() throws Exception {
        when(decoder.decode("token-falsificado")).thenThrow(new JwtException("Failed to authenticate"));

        request.addHeader("Authorization", "Bearer token-falsificado");
        filter.doFilter(request, response, chain);

        verify(chain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(401);
    }
}
