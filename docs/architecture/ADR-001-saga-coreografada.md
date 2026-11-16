# ADR-001 - Participação do Billing em Saga coreografada

- Status: aceita no escopo do Billing Service
- Data: 15/11/2026
- Revisão externa: os contratos devem ser ratificados pelos responsáveis dos
  serviços consumidores e produtores

## Contexto

O fluxo de uma ordem de serviço atravessa serviços com bancos independentes.
Uma transação ACID global não está disponível e nenhum serviço pode escrever no
banco de outro. O Billing precisa receber a solicitação de orçamento, registrar
o resultado local e comunicar a decisão sem assumir a responsabilidade pelos
estados internos da OS ou da execução.

## Decisão

O Billing participará de uma Saga coreografada por eventos no RabbitMQ.

- O OS Service publica `os.quote.requested.v1` quando possuir os dados
  necessários para gerar o orçamento.
- O Billing executa sua transação local e publica eventos que descrevem o
  resultado.
- O OS Service decide como refletir esses fatos no estado da OS.
- O Billing consome `os.cancelled.v1` para compensar o orçamento local quando a
  transição ainda for permitida.
- Eventos são imutáveis, versionados e processados pelo menos uma vez.
- Idempotência, Inbox, Outbox, retry e DLQ serão implementados na etapa de
  mensageria.

Não haverá um orquestrador dentro do Billing. Se o grupo escolher posteriormente
um orquestrador global, ele deverá utilizar contratos explícitos sem acessar o
banco do Billing.

## Limites da decisão

Esta decisão cobre somente o trecho de orçamento da Saga:

`solicitação -> criação -> envio -> aprovação/rejeição/cancelamento`

O gatilho do pagamento ainda depende da decisão do grupo sobre quando a cobrança
ocorre. Por isso, eventos de pagamento e execução não fazem parte da versão
inicial do contrato assíncrono.

## Consequências

### Positivas

- reduz o acoplamento entre os serviços;
- evita acesso cruzado a bancos;
- permite evolução independente dos consumidores;
- oferece rastreabilidade por `correlationId` e `causationId`;
- combina com RabbitMQ, já previsto na solução.

### Custos e riscos

- consistência passa a ser eventual;
- consumidores precisam tolerar duplicidade e eventos fora de ordem;
- a publicação precisa de Outbox para não se perder após a persistência;
- contratos incompatíveis exigem uma nova versão, nunca alteração silenciosa;
- falhas definitivas precisam de DLQ, alerta e procedimento de reprocessamento.

## Alternativas rejeitadas

- **Transação distribuída:** aumenta acoplamento e não é adequada aos bancos
  independentes da solução.
- **Acesso direto ao banco da OS:** viola um requisito obrigatório do desafio.
- **Orquestrador no Billing:** mistura responsabilidades e transforma o serviço
  financeiro no coordenador global da oficina.
- **Somente REST síncrono:** não oferece o desacoplamento e a resiliência
  esperados para a Saga.
