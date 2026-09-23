# Pedidos360 - Backend

Microservicios del sistema **Pedidos360** (Evaluación Parcial N°1 - DSY1107 Desarrollo Cloud Native I).

Arquitectura desplegada en **AWS**:

```
Angular (front) ──> AWS API Gateway ──> BFF Gateway (EC2) ──> Microservicios (EC2) ──> RDS PostgreSQL
                         │                     │
                    valida JWT            valida JWT
                    (Cognito)        (issuer, aud, firma,
                                       exp, roles)
```

## Módulos

| Módulo | Puerto | Descripción |
|---|---|---|
| `common-security` | — | Librería compartida: filtro y configuración para validar el JWT del IDaaS (issuer, audience, firma, vigencia y roles) |
| `bff-gateway` | 8080 | BFF: recibe las llamadas del frontend vía API Gateway, valida el token y enruta a los microservicios |
| `cliente-service` | 8081 | Microservicio de clientes (entidades JPA + repositorios → RDS) |
| `producto-service` | 8082 | Microservicio de productos |
| `pedido-service` | 8083 | Microservicio de pedidos |

## Requisitos

- JDK 21
- Maven 3.9+
- Base de datos PostgreSQL (local o **Amazon RDS**)

## Compilar

```bash
mvn clean package
```

## Ejecutar (desarrollo local)

```bash
mvn spring-boot:run -pl cliente-service
mvn spring-boot:run -pl producto-service
mvn spring-boot:run -pl pedido-service
mvn spring-boot:run -pl bff-gateway
```

## Configuración (variables de entorno en EC2)

| Variable | Descripción |
|---|---|
| `COGNITO_ISSUER_URI` | `https://cognito-idp.<region>.amazonaws.com/<user-pool-id>` |
| `COGNITO_CLIENT_ID` | Client ID de la app registrada en Cognito (audience) |
| `DB_URL` | JDBC de RDS, p. ej. `jdbc:postgresql://<rds-endpoint>:5432/pedidos360` |
| `DB_USER` / `DB_PASSWORD` | Credenciales de RDS (nunca en el repositorio) |
| `CLIENTE_SERVICE_URL` / `PRODUCTO_SERVICE_URL` / `PEDIDO_SERVICE_URL` | URLs internas de los microservicios |
