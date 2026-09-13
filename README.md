# Customer Service

API REST para gerenciamento de clientes, integração externa de Score, segurança OAuth2/JWT com Keycloak e mensageria assíncrona com RabbitMQ.

## Quick Start

Pré-requisitos:

- Java 17+
- Maven
- Docker / Docker Compose

Suba a infraestrutura:

```bash
docker compose up -d
```

Execute os testes rápidos:

```bash
mvn clean test
```

Inicie a aplicação:

```bash
mvn spring-boot:run
```

Acesse:

- Swagger UI: http://localhost:8080/swagger-ui/index.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- RabbitMQ Management: http://localhost:15672
- Keycloak: http://localhost:8082
- WireMock Score Service: http://localhost:8081

---

## Visão Geral da Arquitetura

```text
                  +-------------------+
                  | Swagger / Postman |
                  +---------+---------+
                            |
                            v
                  +-------------------+
                  | Customer Service  |
                  +----+---------+----+
                       |         |
             +---------+         +----------------+
             |                                  |
             v                                  v
       PostgreSQL                         Score Service
                                             |
                                             v
                                          WireMock

Customer Service <---- JWT ---- Keycloak

Customer Service ---- events ----> RabbitMQ
        |
        +---- Transactional Outbox
```

Responsabilidades principais:

- **Customer Service**: expõe APIs REST, aplica regras de negócio, segurança, integração de Score e mensageria.
- **PostgreSQL**: persiste clientes, eventos processados e eventos da Outbox.
- **Keycloak**: autentica usuários e emite Access Tokens JWT.
- **RabbitMQ**: transporta eventos de criação e alteração de status de clientes.
- **WireMock**: simula o serviço externo de Score e cenários de falha.

---

## Stack

- Java 17
- Spring Boot 4.1.x
- Spring Web
- Spring Data JPA
- Spring Validation
- Spring Security OAuth2 Resource Server
- Spring AMQP
- PostgreSQL
- RabbitMQ
- Keycloak
- WireMock
- Swagger / OpenAPI
- Maven
- Docker Compose
- JUnit 5 / Mockito
- Testcontainers / Toxiproxy para testes de integração específicos

---

## Decisões Arquiteturais

### 1. Simplicidade e contratos explícitos

O projeto prioriza código explícito e legível.

CQRS, Command Handler, `GenericService`, `BaseController` e abstrações genéricas de mensageria foram evitados porque não adicionariam benefício proporcional ao escopo atual.

PostgreSQL foi escolhido em vez de H2 para manter a execução local próxima de um banco relacional real, sem exigir instalação local.

O CPF é tratado como identificador imutável após a criação. O `PUT /customers/{id}` altera apenas `name`, `email` e `status`. Tentativas de alterar o CPF são rejeitadas com `400 Bad Request`.

### 2. Segurança

O Keycloak é responsável por autenticação e emissão dos JWTs.

O Customer Service atua exclusivamente como OAuth2 Resource Server: valida os tokens e aplica autorização por roles.

- `USER`: operações de consulta.
- `ADMIN`: consultas e operações de escrita.
- Sem token válido: `401 Unauthorized`.
- Token válido sem permissão: `403 Forbidden`.

A aplicação não implementa login próprio nem persiste usuários.

### 3. Integração externa de Score

O serviço de Score é acessado por HTTP através de um client dedicado.

WireMock foi escolhido para simular o serviço externo sem a necessidade de criar um segundo microsserviço completo.

O client possui timeout explícito de 2 segundos.

Comportamento esperado:

| Cenário | Resposta |
| --- | --- |
| Score disponível | `200 OK` |
| Cliente inexistente | `404 Not Found` |
| Score indisponível / erro 5xx | `503 Service Unavailable` |
| Timeout do Score | `504 Gateway Timeout` |

### 4. Mensageria confiável

A criação de clientes usa **Transactional Outbox** para evitar dual write entre PostgreSQL e RabbitMQ.

Cliente e evento `CUSTOMER_CREATED` são persistidos na mesma transação. Um processo assíncrono publica os eventos pendentes no RabbitMQ e marca `publishedAt` após a publicação.

A entrega segue o modelo **at-least-once**. Uma mensagem pode eventualmente ser republicada, portanto consumidores devem ser idempotentes.

O consumo de `CUSTOMER_STATUS_CHANGE` utiliza `eventId` único em `processed_events`. O registro do evento e a alteração do cliente participam da mesma transação, e duplicatas concorrentes são arbitradas pela constraint do banco.

Mensagens com falha definitiva são encaminhadas para uma DLQ, evitando reprocessamento infinito.

Não foi implementado retry sofisticado, delayed retry ou recuperação automática da DLQ para manter a solução proporcional ao escopo do teste.

### 5. Testabilidade e experiência de avaliação

Swagger/OpenAPI foi incluído para permitir exploração e execução rápida dos endpoints.

A suíte padrão permanece rápida e não depende de Testcontainers.

Cenários de infraestrutura mais caros, como indisponibilidade e recuperação do RabbitMQ durante publicação da Outbox, ficam em testes de integração separados utilizando Testcontainers e Toxiproxy.

---

## Autenticação

Configuração local:

| Item | Valor |
| --- | --- |
| Realm | `customer-service` |
| Client ID | `customer-service` |
| USER | `customer-user / user123` |
| ADMIN | `customer-admin / admin123` |

As credenciais acima existem apenas para desenvolvimento local e avaliação técnica.

### Obtendo um token USER

```bash
curl -X POST http://localhost:8082/realms/customer-service/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=customer-service" \
  -d "username=customer-user" \
  -d "password=user123"
```

### Obtendo um token ADMIN

```bash
curl -X POST http://localhost:8082/realms/customer-service/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=customer-service" \
  -d "username=customer-admin" \
  -d "password=admin123"
```

### Postman

Crie uma requisição:

```text
POST http://localhost:8082/realms/customer-service/protocol/openid-connect/token
```

Em `Body -> x-www-form-urlencoded`:

| Key | Value |
| --- | --- |
| `grant_type` | `password` |
| `client_id` | `customer-service` |
| `username` | `customer-user` ou `customer-admin` |
| `password` | senha correspondente |

Copie o `access_token` retornado e use:

```text
Authorization -> Bearer Token
```

nas chamadas da API.

### Swagger

1. Obtenha um `access_token`.
2. Abra http://localhost:8080/swagger-ui/index.html.
3. Clique em `Authorize`.
4. Cole somente o JWT.
5. Execute os endpoints.

Matriz de autorização:

| Operação | USER | ADMIN |
| --- | --- | --- |
| `GET /customers/**` | Permitido | Permitido |
| `POST /customers` | Negado | Permitido |
| `PUT /customers/{id}` | Negado | Permitido |
| `DELETE /customers/{id}` | Negado | Permitido |

---

## API Endpoints

| Método | Endpoint | Descrição |
| --- | --- | --- |
| `POST` | `/customers` | Cadastra cliente |
| `GET` | `/customers` | Lista clientes |
| `GET` | `/customers?status=ACTIVE` | Filtra por status |
| `GET` | `/customers/{id}` | Consulta por ID |
| `GET` | `/customers/search?name=joao` | Consulta por nome |
| `PUT` | `/customers/{id}` | Atualiza cliente |
| `DELETE` | `/customers/{id}` | Exclui cliente |
| `GET` | `/customers/{id}/score` | Consulta Score externo |

---

## Score fake com WireMock

WireMock fica disponível em:

```text
http://localhost:8081
```

Cenários configurados:

| Requisição | Comportamento |
| --- | --- |
| `GET /scores/12345678901` | `200`, score `750`, `LOW_RISK` |
| `GET /scores/99999999999` | `500 Internal Server Error` |
| `GET /scores/88888888888` | resposta após 5 segundos |

Como o Customer Service possui timeout de 2 segundos, o cenário lento resulta em `504 Gateway Timeout` através de:

```text
GET /customers/{id}/score
```

---

## RabbitMQ

RabbitMQ Management:

```text
http://localhost:15672
```

Credenciais:

```text
customer / customer
```

Topologia:

```text
customer.exchange
   |
   +-- customer.created
   |      |
   |      +--> customer.created.queue
   |
   +-- customer.status.change
          |
          +--> customer.status.change.queue
                       |
                       +--> customer.status.change.dlq
                            em caso de falha definitiva
```

### CUSTOMER_CREATED

```json
{
  "eventId": "7ba85b17-7d33-4d17-9101-569a362239f2",
  "eventType": "CUSTOMER_CREATED",
  "customerId": 123,
  "createdAt": "2026-08-11T15:30:00Z"
}
```

Esse evento é persistido inicialmente na Transactional Outbox e publicado posteriormente no RabbitMQ.

### CUSTOMER_STATUS_CHANGE

```json
{
  "eventId": "cbca5352-22ad-48f2-aaf2-704735bc7737",
  "eventType": "CUSTOMER_STATUS_CHANGE",
  "customerId": 123,
  "status": "INACTIVE"
}
```

O `eventId` é utilizado para garantir idempotência.

Mensagens duplicadas não aplicam novamente o efeito de negócio.

Mensagens que não podem ser processadas são rejeitadas sem requeue infinito e encaminhadas para:

```text
customer.status.change.dlq
```

---

## Testes

### Testes rápidos

```bash
mvn test
```

Essa é a suíte recomendada para validação normal do projeto.

Ela não exige Testcontainers nem inicializa infraestrutura adicional.

### Testes de integração

```bash
mvn verify -Pintegration
```

Esses testes exercitam cenários específicos com infraestrutura real utilizando Testcontainers.

O cenário de Outbox utiliza:

- PostgreSQL
- RabbitMQ
- Toxiproxy

para validar automaticamente indisponibilidade e recuperação da comunicação com o broker.

Docker deve estar disponível para executar essa suíte.

---

## Comportamento de Erros

| HTTP | Situação |
| --- | --- |
| `400 Bad Request` | Payload inválido ou tentativa de alteração de CPF |
| `401 Unauthorized` | Token ausente ou inválido |
| `403 Forbidden` | Token válido sem autorização |
| `404 Not Found` | Cliente inexistente |
| `409 Conflict` | CPF já cadastrado |
| `503 Service Unavailable` | Falha do serviço externo de Score |
| `504 Gateway Timeout` | Timeout na integração de Score |

---

## Notas de Desenvolvimento

A implementação prioriza simplicidade, comportamento explícito e consistência nos cenários de falha.

Padrões adicionais foram introduzidos somente quando resolvem riscos concretos do problema, como Transactional Outbox, idempotência e DLQ, evitando complexidade arquitetural sem benefício proporcional ao escopo.
