# Mecanica Billing Service

Microsserviço responsável por orçamentos e pagamentos das ordens de serviço da
oficina mecânica.

## Estado atual

O desenvolvimento foi iniciado pelo fluxo de orçamento. O primeiro incremento
já permite:

- criar e consultar um orçamento;
- calcular o total a partir de peças e mão de obra;
- enviar o orçamento para aprovação;
- aprovar ou rejeitar o orçamento;
- persistir no MongoDB;
- validar estados, valores e duplicidade por OS;
- consumir solicitações e cancelamentos pelo RabbitMQ de forma idempotente;
- publicar eventos por Outbox com confirmação do broker;
- aplicar retry e encaminhar falhas definitivas para DLQ;
- consultar o contrato OpenAPI e métricas operacionais.

Pagamento e Mercado Pago estão planejados para o próximo incremento.

## Stack

- Java 17;
- Spring Boot 3.5;
- MongoDB;
- RabbitMQ;
- Maven e JaCoCo;
- Springdoc OpenAPI;
- Actuator e Prometheus;
- Docker Compose.

## Executar localmente

Pré-requisitos:

- Java 17 configurado em `JAVA_HOME`;
- Docker Desktop ou Docker Engine com Compose.

Suba as dependências:

```bash
docker compose up -d --wait
```

Compile, execute os testes e valide a cobertura:

```bash
./mvnw clean verify
```

Inicie a aplicação:

```bash
./mvnw spring-boot:run
```

No Windows também é possível utilizar `mvnw.cmd`.

## Endpoints

| Método | Rota | Descrição |
| --- | --- | --- |
| `POST` | `/api/v1/quotes` | Criar orçamento em rascunho |
| `GET` | `/api/v1/quotes/{id}` | Consultar por ID |
| `GET` | `/api/v1/quotes?osId={osId}` | Consultar por OS |
| `POST` | `/api/v1/quotes/{id}/submit` | Enviar para aprovação |
| `POST` | `/api/v1/quotes/{id}/approve` | Aprovar |
| `POST` | `/api/v1/quotes/{id}/reject` | Rejeitar |

Exemplo de criação:

```bash
curl -X POST http://localhost:8080/api/v1/quotes \
  -H "Content-Type: application/json" \
  -d '{
    "osId": "OS-001",
    "customerId": "CUSTOMER-001",
    "items": [
      {
        "description": "Troca de óleo",
        "type": "LABOR",
        "quantity": 1,
        "unitPrice": 120.00
      },
      {
        "description": "Filtro de óleo",
        "type": "PART",
        "quantity": 1,
        "unitPrice": 45.50
      }
    ],
    "currency": "BRL",
    "validUntil": "2026-12-01T12:00:00Z"
  }'
```

## URLs locais

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8080/api-docs>
- Health: <http://localhost:8080/actuator/health>
- Prometheus: <http://localhost:8080/actuator/prometheus>
- RabbitMQ Management: <http://localhost:15672>
- Cobertura JaCoCo: `target/site/jacoco/index.html`

## Configuração

| Variável | Valor padrão |
| --- | --- |
| `SERVER_PORT` | `8080` |
| `MONGODB_URI` | `mongodb://localhost:27017/billing?replicaSet=rs0` |
| `RABBITMQ_HOST` | `localhost` |
| `RABBITMQ_PORT` | `5672` |
| `RABBITMQ_USERNAME` | `guest` |
| `RABBITMQ_PASSWORD` | `guest` |
| `RABBITMQ_EXCHANGE` | `mecanica.events` |
| `RABBITMQ_DLX` | `mecanica.events.dlx` |
| `OUTBOX_POLL_INTERVAL_MS` | `1000` |
| `OUTBOX_MAX_ATTEMPTS` | `10` |

Credenciais reais não devem ser versionadas.

## Qualidade

O comando `./mvnw verify` executa os testes e reprova o build se a cobertura de
linhas ficar abaixo de 80%. No primeiro incremento:

- 40 testes;
- 92,06% de cobertura de linhas;
- 79,49% de cobertura de branches.

## Integração e Saga

O Billing participa de uma Saga coreografada por eventos no RabbitMQ. O fluxo
implementado cobre a solicitação, criação, envio, aprovação, rejeição e
cancelamento de orçamentos sem acessar o banco do OS Service. Inbox, Outbox,
publisher confirms, retry e DLQ garantem processamento pelo menos uma vez com
efeitos idempotentes.

- [Decisão arquitetural da Saga](docs/architecture/ADR-001-saga-coreografada.md)
- [Fluxo, topologia e compensações](docs/architecture/SAGA-BILLING.md)
- [Contrato AsyncAPI](docs/contracts/asyncapi.yaml)
- [Exemplos de eventos](docs/contracts/examples)

O contrato de pagamento será fechado somente após o grupo definir em qual etapa
do fluxo ocorre a cobrança.

## Planejamento

- [Entregáveis por etapa](docs/ENTREGAVEIS.md)
- [Análise completa e checklist da Fase 4](docs/FASE4-CHECKLIST.md)

A próxima entrega é implementar pagamentos e a integração sandbox com o Mercado
Pago.
