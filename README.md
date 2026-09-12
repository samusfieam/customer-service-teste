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
