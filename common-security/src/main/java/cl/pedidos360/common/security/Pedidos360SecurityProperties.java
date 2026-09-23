package cl.pedidos360.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades de validacion del JWT para todos los servicios.
 * Se configuran bajo el prefijo {@code pedidos360.security}.
 */
@ConfigurationProperties(prefix = "pedidos360.security")
public class Pedidos360SecurityProperties {

    /** Audience esperada (client id de la aplicacion registrada en el IDaaS). */
    private String audience = "pedidos360-client";

    /** Claim que contiene los grupos/roles del usuario (Cognito: cognito:groups). */
    private String rolesClaim = "cognito:groups";

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public String getRolesClaim() {
        return rolesClaim;
    }

    public void setRolesClaim(String rolesClaim) {
        this.rolesClaim = rolesClaim;
    }
}
