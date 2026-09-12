# Customer Service

## Swagger / OpenAPI

Com a aplicacao em execucao local, acesse o Swagger UI em:

http://localhost:8080/swagger-ui/index.html

A documentacao OpenAPI em JSON fica disponivel em:

http://localhost:8080/v3/api-docs

## Score fake com WireMock

O servico externo de Score e simulado localmente com WireMock e fica disponivel em:

http://localhost:8081

Cenarios disponiveis:

- GET /scores/12345678901 -> 200 com score 750
- GET /scores/99999999999 -> 500
- GET /scores/88888888888 -> resposta com atraso de 5 segundos

## Keycloak

O Keycloak local fica disponivel em:

http://localhost:8082

Configuracao de desenvolvimento:

- Realm: customer-service
- Client ID: customer-service
- Usuario USER: customer-user / user123
- Usuario ADMIN: customer-admin / admin123

Para obter um token USER:

```bash
curl -X POST http://localhost:8082/realms/customer-service/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=customer-service" \
  -d "username=customer-user" \
  -d "password=user123"
```

Para obter um token ADMIN:

```bash
curl -X POST http://localhost:8082/realms/customer-service/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=customer-service" \
  -d "username=customer-admin" \
  -d "password=admin123"
```
