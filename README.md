# Customer Service

API REST para gerenciamento de clientes, integracao externa de Score, seguranca OAuth2/JWT com Keycloak e mensageria assincrona com RabbitMQ.

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

- Customer Service: expoe os endpoints REST de clientes, valida regras de negocio, consulta Score externo e publica/consome eventos RabbitMQ.
- PostgreSQL: armazena clientes e eventos processados para idempotencia.
- Keycloak: emite os tokens JWT usados pela aplicacao como OAuth2 Resource Server.
- RabbitMQ: transporta eventos de criacao de cliente e alteracao de status.
- WireMock: simula o servico externo de Score para testes locais.

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

1. A aplicacao usa uma arquitetura simples, sem CQRS ou Command Handler, porque o escopo do teste tecnico nao exige essa separacao.
2. O codigo evita abstracoes genericas prematuras, como `GenericService`, `BaseController` ou `EventBus` generico.
3. O ambiente local usa PostgreSQL em Docker em vez de H2, mantendo o comportamento mais proximo do banco real esperado.
4. CPF e imutavel apos a criacao do cliente. O `PUT /customers/{id}` altera apenas `name`, `email` e `status`; se `cpf` for enviado no body, a API retorna `400 Bad Request`.
5. Swagger/OpenAPI foi incluido para facilitar a avaliacao e a execucao manual dos endpoints.
6. A autenticacao fica no Keycloak. A aplicacao atua como OAuth2 Resource Server, validando JWT, sem armazenar usuarios ou executar login.
7. As roles seguem o contrato: `USER` pode executar consultas e `ADMIN` pode executar consultas e operacoes de escrita. Requisicoes sem token retornam `401`; tokens validos sem permissao retornam `403`.
8. A integracao de Score usa WireMock como servico externo fake, evitando criar um segundo microsservico Spring Boot apenas para o teste.
9. A integracao de Score tem timeout de 2 segundos. Resposta normal retorna `200`, cliente inexistente retorna `404`, erro 5xx do Score retorna `503` e timeout retorna `504`.
10. A mensageria usa Spring AMQP diretamente, com eventos especificos e configuracao explicita de exchange, filas e routing keys.
11. A idempotencia do consumo de `CUSTOMER_STATUS_CHANGE` usa `eventId` unico na tabela `processed_events`. Mensagens duplicadas nao alteram novamente o cliente, e a atualizacao de status ocorre na mesma transacao do registro do evento processado.

## Running the application

Prerequisitos:

- Java 17 ou superior
- Maven
- Docker Compose

Suba os servicos de infraestrutura:

```bash
docker compose up -d
```

Execute os testes:

```bash
mvn clean test
```

Inicie a aplicacao:

```bash
mvn spring-boot:run
```

Servicos locais:

- Aplicacao: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui/index.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- RabbitMQ Management: http://localhost:15672
- WireMock Score Service: http://localhost:8081
- Keycloak: http://localhost:8082
- PostgreSQL: localhost:5432

## Authentication

Configuracao local:

- Realm: `customer-service`
- Client ID: `customer-service`
- Usuario USER: `customer-user` / `user123`
- Usuario ADMIN: `customer-admin` / `admin123`

Endpoint para obtencao de token:

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

No Postman, crie uma requisicao `POST` para o endpoint de token, selecione `x-www-form-urlencoded` no body e informe os campos `grant_type`, `client_id`, `username` e `password`. Depois copie o `access_token` retornado e use em `Authorization: Bearer <token>` nas chamadas da API.

No Swagger UI, clique em `Authorize`, cole o token JWT no formato `Bearer <token>` e execute os endpoints.

Matriz de autorizacao:

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

O servico externo de Score e simulado localmente com WireMock e fica disponivel em:

```text
http://localhost:8081
```

Cenarios disponiveis:

- `GET /scores/12345678901` -> `200 OK` com score `750`
- `GET /scores/99999999999` -> `500 Internal Server Error`
- `GET /scores/88888888888` -> resposta com atraso de 5 segundos

Como o Customer Service possui timeout de 2 segundos na chamada ao Score, o cenario lento resulta em `504 Gateway Timeout` quando acessado por `GET /customers/{id}/score`.

## RabbitMQ

RabbitMQ Management fica disponivel em:

```text
http://localhost:15672
```

Credenciais locais:

- Usuario: `guest`
- Senha: `guest`

Configuracao de mensageria:

- Exchange: `customer.exchange`
- Routing key de criacao: `customer.created`
- Routing key de alteracao de status: `customer.status.change`
- Fila de criacao: `customer.created.queue`
- Fila de alteracao de status: `customer.status.change.queue`

Evento publicado ao criar cliente:

```json
{
  "eventId": "UUID",
  "eventType": "CUSTOMER_CREATED",
  "customerId": 123,
  "createdAt": "2026-08-11T15:30:00Z"
}
```

Evento consumido para alteracao de status:

```json
{
  "eventId": "UUID",
  "eventType": "CUSTOMER_STATUS_CHANGE",
  "customerId": 123,
  "status": "INACTIVE"
}
```

A idempotencia do consumo e garantida pela tabela `processed_events`. Se uma mensagem com o mesmo `eventId` for entregue novamente, ela e ignorada e o status do cliente nao e alterado outra vez.

## Tests

Execute:

```bash
mvn clean test
```

Os testes cobrem os principais fluxos de criacao, consulta, atualizacao, exclusao, validacoes, CPF imutavel, integracao de Score, seguranca, mensageria e idempotencia.

## Error behavior

| HTTP status | Situacao |
| --- | --- |
| `400 Bad Request` | Payload invalido ou tentativa de alterar CPF no PUT |
| `401 Unauthorized` | Requisicao sem token JWT |
| `403 Forbidden` | Token valido sem permissao suficiente |
| `404 Not Found` | Cliente nao encontrado |
| `409 Conflict` | CPF ja cadastrado |
| `503 Service Unavailable` | Servico externo de Score retornou erro 5xx |
| `504 Gateway Timeout` | Timeout na chamada ao servico externo de Score |

## Development notes

O projeto prioriza implementacao explicita e legivel, com responsabilidades separadas apenas quando existe uma necessidade concreta. A estrutura evita camadas e padroes adicionais que nao trazem beneficio direto para o escopo do teste tecnico.
