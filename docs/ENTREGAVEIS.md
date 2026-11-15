# Entregáveis por etapa

Este roteiro divide a entrega do Billing Service em incrementos demonstráveis.
Um item só deve ser marcado como concluído depois de validado.

Legenda:

- `[x]` concluído;
- `[ ]` pendente;
- itens não decididos pelo grupo permanecem pendentes mesmo que exista uma
  recomendação técnica.

## Etapa 0 - Escopo e contratos

**Entrega:** fronteiras do serviço e contratos acordados entre os integrantes.

- [x] Analisar os requisitos oficiais da Fase 4.
- [x] Delimitar responsabilidades do Billing Service.
- [x] Mapear riscos, dependências e critérios de aceite.
- [ ] Confirmar MongoDB no Billing e banco SQL em outro serviço.
- [ ] Confirmar Saga coreografada ou definir o orquestrador.
- [ ] Definir em qual momento do fluxo ocorre o pagamento.
- [ ] Definir estados oficiais da OS e eventos entre serviços.
- [ ] Definir a origem dos itens e preços do orçamento.
- [ ] Aprovar exemplos JSON dos contratos assíncronos.

**Aceite:** OS, Billing e execução conseguem evoluir sem banco ou modelo
interno compartilhado.

## Etapa 1 - Fundação executável

**Entrega:** aplicação Spring Boot compilável, configuração externa e ambiente
local de dependências.

- [x] Criar projeto Maven com Java 17 e Spring Boot 3.x.
- [x] Adicionar Maven Wrapper.
- [x] Configurar Web, Validation, MongoDB, RabbitMQ, Actuator e OpenAPI.
- [x] Externalizar conexão, porta e credenciais por variáveis de ambiente.
- [x] Criar ambiente local para MongoDB e RabbitMQ com Docker Compose.
- [x] Expor health check e métricas Prometheus.
- [x] Validar `./mvnw verify`.
- [x] Validar inicialização com as dependências locais.

**Aceite:** o projeto compila, os testes passam e o health check responde com
as dependências disponíveis.

## Etapa 2 - MVP de orçamento

**Entrega:** primeiro fluxo funcional do Billing, do rascunho à decisão.

- [x] Modelar orçamento, item, tipo e estados.
- [x] Calcular valores com `BigDecimal`.
- [x] Validar itens, moeda, prazo e transições.
- [x] Persistir orçamento no MongoDB com índice único por OS.
- [x] Implementar criação e consulta por ID ou OS.
- [x] Implementar envio para aprovação.
- [x] Implementar aprovação e rejeição.
- [x] Padronizar respostas de erro.
- [x] Documentar endpoints com OpenAPI.
- [x] Validar o fluxo com MongoDB real.
- [ ] Definir e implementar versionamento de orçamento com o grupo.

**Aceite:** uma OS possui orçamento consistente e apenas transições válidas são
persistidas.

## Etapa 3 - Mensageria e participação na Saga

**Entrega:** integração assíncrona confiável com o OS Service.

- [ ] Versionar envelope e payloads dos eventos.
- [ ] Configurar exchange, filas e routing keys.
- [ ] Consumir abertura/cancelamento da OS de forma idempotente.
- [ ] Publicar eventos de orçamento criado, enviado, aprovado e rejeitado.
- [ ] Implementar Inbox para deduplicação.
- [ ] Implementar Outbox para publicação confiável.
- [ ] Configurar retry com backoff e DLQ.
- [ ] Propagar `eventId`, `correlationId` e `causationId`.
- [ ] Implementar falhas e compensações aprovadas pelo grupo.
- [ ] Testar duplicidade, indisponibilidade e reprocessamento.

**Aceite:** mensagens repetidas não duplicam efeitos e eventos persistidos não
são perdidos quando o broker fica indisponível.

## Etapa 4 - Pagamento e Mercado Pago

**Entrega:** cobrança sandbox segura e confirmação assíncrona.

- [ ] Modelar pagamento e sua máquina de estados.
- [ ] Impedir pagamento para orçamento não aprovado.
- [ ] Criar adapter da API do Mercado Pago.
- [ ] Criar cobrança/preferência com chave de idempotência.
- [ ] Persistir referência externa e dados seguros de checkout.
- [ ] Implementar webhook e validar sua assinatura.
- [ ] Consultar o pagamento no provedor antes de confirmar.
- [ ] Tratar notificações duplicadas e fora de ordem.
- [ ] Publicar eventos de pagamento pendente, aprovado, rejeitado e falho.
- [ ] Implementar reconciliação de pagamentos pendentes.
- [ ] Implementar estorno apenas se aprovado como compensação.
- [ ] Validar o fluxo no sandbox.

**Aceite:** uma cobrança é criada uma única vez e somente a confirmação
verificada no Mercado Pago avança a OS.

## Etapa 5 - Testes, cobertura e BDD

**Entrega:** qualidade automatizada e evidência de cobertura.

- [x] Configurar JaCoCo com gate mínimo de 80%.
- [x] Criar testes unitários iniciais de domínio, aplicação e API.
- [x] Validar cobertura inicial acima de 80% (96,12% de linhas).
- [ ] Adicionar testes de integração com MongoDB e RabbitMQ.
- [ ] Testar contratos HTTP e de eventos.
- [ ] Testar o adapter Mercado Pago sem produção.
- [ ] Implementar cenário BDD ponta a ponta com os demais serviços.
- [ ] Implementar cenário BDD de falha/compensação.
- [ ] Configurar SonarQube ou SonarCloud.
- [ ] Publicar evidências de cobertura e quality gate.

**Aceite:** o pipeline bloqueia cobertura inferior a 80% ou quality gate
reprovado, e o BDD comprova o fluxo distribuído.

## Etapa 6 - Observabilidade

**Entrega:** rastreamento operacional de uma OS entre serviços.

- [x] Expor health check e endpoint Prometheus.
- [ ] Estruturar logs e remover dados sensíveis.
- [ ] Propagar trace/correlation ID por HTTP e RabbitMQ.
- [ ] Integrar métricas, logs e traces às ferramentas da Fase 3.
- [ ] Medir orçamentos, pagamentos, erros, retries, DLQ e latência.
- [ ] Criar dashboard e alertas mínimos.
- [ ] Documentar reprocessamento seguro da DLQ.

**Aceite:** uma OS é localizada de ponta a ponta por um mesmo identificador e
uma falha aparece no monitoramento.

## Etapa 7 - Container, Kubernetes e infraestrutura

**Entrega:** serviço reproduzível e implantável no cluster.

- [ ] Criar Dockerfile multi-stage e executar como usuário não root.
- [ ] Criar `.dockerignore`.
- [ ] Criar Deployment e Service.
- [ ] Configurar ConfigMap e referências a Secret.
- [ ] Configurar liveness, readiness, recursos e rollout.
- [ ] Configurar Ingress somente para endpoints públicos.
- [ ] Definir persistência ou serviço gerenciado do MongoDB.
- [ ] Criar Terraform acordado para a infraestrutura.
- [ ] Validar manifests em um cluster.

**Aceite:** uma imagem imutável sobe no Kubernetes, passa nas probes e não
contém credenciais.

## Etapa 8 - CI/CD e governança

**Entrega:** pipeline independente, da validação do PR ao deploy.

- [ ] Criar CI de PR com build, testes, cobertura e qualidade.
- [ ] Adicionar scans de dependências, secrets e imagem.
- [ ] Publicar imagem versionada em registry.
- [ ] Automatizar deploy no Kubernetes.
- [ ] Separar secrets por ambiente.
- [ ] Configurar rollback ou rollout seguro.
- [ ] Proteger `main` com PR e checks obrigatórios.
- [ ] Registrar evidência de execução bem-sucedida.

**Aceite:** código reprovado não entra na principal e código aprovado gera
imagem e deploy rastreáveis.

## Etapa 9 - Documentação e demonstração

**Entrega:** repositório auditável e material para a entrega do grupo.

- [ ] Atualizar README com arquitetura, execução e variáveis.
- [ ] Documentar e justificar a estratégia Saga.
- [ ] Publicar diagramas de contexto, componentes e sequência.
- [ ] Manter Swagger ou Postman atualizado.
- [ ] Reunir evidências de cobertura, qualidade, deploy e observabilidade.
- [ ] Entregar ao grupo os dados do Billing para o PDF final.
- [ ] Ensaiar fluxo feliz completo.
- [ ] Ensaiar falha, retry/DLQ e compensação.
- [ ] Participar do vídeo de até 15 minutos.

**Aceite:** outra pessoa executa, integra e demonstra o Billing usando somente
o repositório e suas evidências.

## Caminho crítico

1. Concluir decisões da Etapa 0.
2. Validar as Etapas 1 e 2.
3. Implementar Saga e mensageria da Etapa 3.
4. Implementar Mercado Pago da Etapa 4.
5. Fechar testes e BDD da Etapa 5.
6. Concluir operação, infraestrutura, CI/CD e evidências.
