# Customer Service

API REST para gerenciamento de clientes, integração externa de Score, segurança OAuth2/JWT com Keycloak e mensageria assíncrona com RabbitMQ.

## Architecture Overview

```text
Client / Swagger / Postman
        |
        v
Customer Service
  |      |       |
  v      v       v
Postgres Keycloak RabbitMQ
         |
         v
      JWT validation

Customer Service -> HTTP -> WireMock Score Service
```

Responsabilidades principais:

- Customer Service: expõe os endpoints REST de clientes, valida regras de negócio, consulta Score externo e publica/consome eventos RabbitMQ.
- PostgreSQL: armazena clientes e eventos processados para idempotência.
- Keycloak: emite os tokens JWT usados pela aplicação como OAuth2 Resource Server.
- RabbitMQ: transporta eventos de criação de cliente e alteração de status.
- WireMock: simula o serviço externo de Score para testes locais.

## Technology Stack

- Java 17
- Spring Boot 3.x
- Spring Web
- Spring Data JPA
- Spring Validation
- Spring Security OAuth2 Resource Server
- Spring AMQP
- PostgreSQL
- RabbitMQ
- Keycloak
- WireMock
- Swagger/OpenAPI com springdoc-openapi
- Maven
- Docker Compose
- JUnit 5 e Mockito

## Architecture Decisions

1. A aplicação usa uma arquitetura simples, sem CQRS ou Command Handler, porque o escopo do teste técnico não exige essa separação.
2. O código evita abstrações genéricas prematuras, como `GenericService`, `BaseController` ou `EventBus` genérico.
3. O ambiente local usa PostgreSQL em Docker em vez de H2, mantendo o comportamento mais próximo do banco real esperado.
4. CPF é imutável após a criação do cliente. O `PUT /customers/{id}` altera apenas `name`, `email` e `status`; se `cpf` for enviado no body, a API retorna `400 Bad Request`.
5. Swagger/OpenAPI foi incluído para facilitar a avaliação e a execução manual dos endpoints.
6. A autenticação fica no Keycloak. A aplicação atua como OAuth2 Resource Server, validando JWT, sem armazenar usuários ou executar login.
7. As roles seguem o contrato: `USER` pode executar consultas e `ADMIN` pode executar consultas e operações de escrita. Requisições sem token retornam `401`; tokens válidos sem permissão retornam `403`.
8. A integração de Score usa WireMock como serviço externo fake, evitando criar um segundo microsserviço Spring Boot apenas para o teste.
9. A integração de Score tem timeout de 2 segundos. Resposta normal retorna `200`, cliente inexistente retorna `404`, erro 5xx do Score retorna `503` e timeout retorna `504`.
10. A mensageria usa Spring AMQP diretamente, com eventos específicos e configuração explícita de exchange, filas e routing keys.
11. A idempotência do consumo de `CUSTOMER_STATUS_CHANGE` usa `eventId` único na tabela `processed_events`. Mensagens duplicadas não alteram novamente o cliente, e a atualização de status ocorre na mesma transação do registro do evento processado.

## Running the application

Pré-requisitos:

- Java 17 ou superior
- Maven
- Docker Compose

Suba os serviços de infraestrutura:

```bash
docker compose up -d
```

Execute os testes:

```bash
mvn clean test
```

Inicie a aplicação:

```bash
mvn spring-boot:run
```

Serviços locais:

- Aplicação: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui/index.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- RabbitMQ Management: http://localhost:15672
- WireMock Score Service: http://localhost:8081
- Keycloak: http://localhost:8082
- PostgreSQL: localhost:5432

## Authentication

Configuração local:

- Realm: `customer-service`
- Client ID: `customer-service`
- Usuário USER: `customer-user` / `user123`
- Usuário ADMIN: `customer-admin` / `admin123`

Endpoint para obtenção de token:

```text
POST http://localhost:8082/realms/customer-service/protocol/openid-connect/token
Content-Type: application/x-www-form-urlencoded
```

Token USER:

```bash
curl -X POST http://localhost:8082/realms/customer-service/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=customer-service" \
  -d "username=customer-user" \
  -d "password=user123"
```

Token ADMIN:

```bash
curl -X POST http://localhost:8082/realms/customer-service/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=customer-service" \
  -d "username=customer-admin" \
  -d "password=admin123"
```

No Postman, crie uma requisição `POST` para o endpoint de token, selecione `x-www-form-urlencoded` no body e informe os campos `grant_type`, `client_id`, `username` e `password`. Depois copie o `access_token` retornado e use em `Authorization: Bearer <token>` nas chamadas da API.

No Swagger UI, clique em `Authorize`, cole o token JWT no formato `Bearer <token>` e execute os endpoints.

Matriz de autorização:

| Endpoint | USER | ADMIN |
| --- | --- | --- |
| `GET /customers/**` | Permitido | Permitido |
| `POST /customers` | Negado | Permitido |
| `PUT /customers/{id}` | Negado | Permitido |
| `DELETE /customers/{id}` | Negado | Permitido |

## API Endpoints

- `POST /customers`
- `GET /customers`
- `GET /customers?status=ACTIVE`
- `GET /customers/{id}`
- `GET /customers/search?name=joao`
- `PUT /customers/{id}`
- `DELETE /customers/{id}`
- `GET /customers/{id}/score`

## Score fake com WireMock

O serviço externo de Score é simulado localmente com WireMock e fica disponível em:

```text
http://localhost:8081
```

Cenários disponíveis:

- `GET /scores/12345678901` -> `200 OK` com score `750`
- `GET /scores/99999999999` -> `500 Internal Server Error`
- `GET /scores/88888888888` -> resposta com atraso de 5 segundos

Como o Customer Service possui timeout de 2 segundos na chamada ao Score, o cenário lento resulta em `504 Gateway Timeout` quando acessado por `GET /customers/{id}/score`.

## RabbitMQ

RabbitMQ Management fica disponível em:

```text
http://localhost:15672
```

Credenciais locais:

- Usuário: `customer`
- Senha: `customer`

Configuração de mensageria:

- Exchange: `customer.exchange`
- Routing key de criação: `customer.created`
- Routing key de alteração de status: `customer.status.change`
- Fila de criação: `customer.created.queue`
- Fila de alteração de status: `customer.status.change.queue`

Evento publicado ao criar cliente:

```json
{
  "eventId": "UUID",
  "eventType": "CUSTOMER_CREATED",
  "customerId": 123,
  "createdAt": "2026-08-11T15:30:00Z"
}
```

Evento consumido para alteração de status:

```json
{
  "eventId": "UUID",
  "eventType": "CUSTOMER_STATUS_CHANGE",
  "customerId": 123,
  "status": "INACTIVE"
}
```

A idempotência do consumo é garantida pela tabela `processed_events`. Se uma mensagem com o mesmo `eventId` for entregue novamente, ela é ignorada e o status do cliente não é alterado outra vez.

## Tests

Execute:

```bash
mvn clean test
```

Os testes cobrem os principais fluxos de criação, consulta, atualização, exclusão, validações, CPF imutável, integração de Score, segurança, mensageria e idempotência.

## Error behavior

| HTTP status | Situação |
| --- | --- |
| `400 Bad Request` | Payload inválido ou tentativa de alterar CPF no PUT |
| `401 Unauthorized` | Requisição sem token JWT |
| `403 Forbidden` | Token válido sem permissão suficiente |
| `404 Not Found` | Cliente não encontrado |
| `409 Conflict` | CPF já cadastrado |
| `503 Service Unavailable` | Serviço externo de Score retornou erro 5xx |
| `504 Gateway Timeout` | Timeout na chamada ao serviço externo de Score |

## Development notes

O projeto prioriza implementação explícita e legível, com responsabilidades separadas apenas quando existe uma necessidade concreta. A estrutura evita camadas e padrões adicionais que não trazem benefício direto para o escopo do teste técnico.
