package cl.pedidos360.bff;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * BFF (Backend For Frontend) de Pedidos360.
 * Recibe las llamadas del frontend a traves de AWS API Gateway,
 * valida el JWT emitido por el IDaaS (Amazon Cognito) y enruta
 * hacia los microservicios internos.
 */
@SpringBootApplication
public class BffApplication {

    public static void main(String[] args) {
        SpringApplication.run(BffApplication.class, args);
    }
}
