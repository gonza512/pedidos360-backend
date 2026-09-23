package cl.pedidos360.bff;

import cl.pedidos360.bff.proxy.BffProxyFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas basicas de autenticacion y autorizacion del BFF.
 *
 * El decoder y el proxy se reemplazan para aislar la prueba de seguridad:
 * sin token -> 401, token invalido -> 401, sin rol -> 403, con rol -> 200.
 */
@SpringBootTest
@AutoConfigureMockMvc
class BffSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private BffProxyFilter bffProxyFilter;

    private Jwt jwt(List<String> groups) {
        return new Jwt("token", Instant.now(), Instant.now().plusSeconds(300),
                Map.of("alg", "RS256"),
                Map.of("sub", "usuario-1",
                        "iss", "https://cognito-idp.us-east-1.amazonaws.com/us-east-1_ABC",
                        "aud", List.of("pedidos360-client"),
                        "cognito:groups", groups));
    }

    @Test
    void sinTokenResponde401() throws Exception {
        mockMvc.perform(get("/api/pedidos"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("unauthorized"));
    }

    @Test
    void tokenInvalidoResponde401() throws Exception {
        when(jwtDecoder.decode(anyString())).thenThrow(
                new org.springframework.security.oauth2.jwt.JwtException("Jwt expired"));

        mockMvc.perform(get("/api/pedidos").header("Authorization", "Bearer vencido"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Token invalido")));
    }

    @Test
    void sinRolAdminResponde403AlCrearProducto() throws Exception {
        when(jwtDecoder.decode("usuario-normal")).thenReturn(jwt(List.of("USER")));

        mockMvc.perform(post("/api/productos").header("Authorization", "Bearer usuario-normal"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("forbidden"));
    }

    @Test
    void conRolAdminPasaLaAutorizacion() throws Exception {
        when(jwtDecoder.decode("admin-token")).thenReturn(jwt(List.of("ADMIN")));

        mockMvc.perform(post("/api/productos").header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk());
    }

    @Test
    void usuarioAutenticadoPuedeLeer() throws Exception {
        when(jwtDecoder.decode("usuario-token")).thenReturn(jwt(List.of("USER")));

        mockMvc.perform(get("/api/pedidos").header("Authorization", "Bearer usuario-token"))
                .andExpect(status().isOk());
    }
}
