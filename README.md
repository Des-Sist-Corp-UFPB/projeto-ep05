# Sweet Delights — Monorepo

Este repositório contém dois projetos que juntos formam a plataforma **Sweet Delights**.

```
projeto-ep05/
├── .env.example              # Variáveis da stack de produção completa
├── docker/                   # Orquestração da stack completa (ver docker/README.md)
│   ├── compose/
│   │   ├── dev.yml              ← ambiente de desenvolvimento
│   │   ├── single.yml           ← produção (imagem única, usado pelo deploy automático)
│   │   ├── prod.yml             ← alternativa: 4 containers separados
│   │   └── test.yml             ← testes isolados do backend
│   └── single/                  # Dockerfile + configs usados só pelo modo single
├── backend/                  # API + painéis admin/sysadmin (Spring Boot + Thymeleaf)
│   └── docker/                  # Dockerfiles usados só em dev/test local do backend
│       ├── Dockerfile
│       ├── Dockerfile.dev
│       └── Dockerfile.test
├── frontend-cliente/         # Loja do cliente (React + Vite)
├── nginx/                    # Proxy reverso central (produção e dev)
├── TESTING.md                # Documentação centralizada de testes
└── README.md
```

---

## backend/

Aplicação Spring Boot com:
- **API REST** — consumida pelo frontend React (clientes)
- **Painel Admin** — gerenciamento de produtos, cupons, categorias e pedidos (Thymeleaf)
- **Painel SysAdmin** — gerenciamento de admins e configurações do sistema (Thymeleaf)
- Autenticação JWT, Flyway, Docker

### Rodar localmente

```bash
# Configurar variáveis de ambiente
cp backend/.env.example backend/.env

# Subir a stack completa de desenvolvimento (backend + banco + adminer)
docker compose -f docker/compose/dev.yml up
```

---

## Produção

A stack de produção (Postgres + app) é orquestrada por **um único arquivo, na
raiz**: `docker/compose/single.yml`. O serviço `app` é **uma única imagem**
(gerada por `docker/single/Dockerfile`) que contém o React já buildado, o
Spring Boot e um Nginx interno, todos rodando no mesmo container via
`supervisord` — é o modo pensado para servidores onde só se consegue publicar
um container por grupo/projeto (ex.: PaaS, ou o servidor da disciplina).

```bash
cp .env.example .env   # preencha os valores reais, nunca comite o .env
docker compose -f docker/compose/single.yml --env-file .env pull
docker compose -f docker/compose/single.yml --env-file .env up -d
```

Dentro do container, o Nginx interno escuta na porta 80 e roteia por caminho
(`/api`, `/admin`, `/sysadmin`, `/produtos`, `/logout`, `/actuator`,
`/webjars`, `/images`, `/css`, `/js` → Spring Boot local; o resto → React). O
compose publica só essa porta 80 do container para o host, numa porta fixa do
host (`127.0.0.1:8105`) combinada previamente com o responsável pelo servidor
da disciplina (`dsc.rodrigor.com`) — o proxy central da turma encaminha o
domínio do grupo para essa porta.

O deploy automático (GitHub Actions, `.github/workflows/deploy.yml`) builda e
publica a imagem única no GHCR e depois conecta via SSH no servidor para
rodar `pull` + `up -d` com esse mesmo compose. Segredos necessários no
repositório (Settings → Secrets and variables → Actions): `SSH_DEPLOY_KEY`,
`SSH_USERNAME`, `DEPLOY_PATH` (caminho da raiz do monorepo no servidor,
contendo o `.env` já preenchido) e `VITE_MP_PUBLIC_KEY`.

> Arquitetura anterior (4 containers separados: postgres, backend, frontend,
> nginx) ainda existe em `docker/compose/prod.yml`, caso um dia seja preciso
> escalar frontend e backend de forma independente — mas não é mais o que o
> deploy automático usa.

---

## frontend-cliente/

SPA em React (Vite) para o fluxo do cliente: navegação por categorias, detalhes de produto, carrinho, checkout e perfil.

### Rodar localmente

```bash
cd frontend-cliente
npm install
npm run dev
```

Veja `frontend-cliente/README.md` para configuração da URL da API e outros detalhes.

---

## Testes

Veja [`TESTING.md`](./TESTING.md) para a documentação completa — como rodar, o que é testado e os thresholds de cobertura.

---

## Cobertura de Testes

Relatórios de cobertura gerados e versionados na pasta [`cobertura/`](./cobertura/):

| Módulo   | Ferramenta  | Cobertura (linhas) | Caminho do relatório |
|----------|-------------|---------------------|------------------------|
| Backend  | JaCoCo      | **99,41%** (linhas) / 99,10% (instruções) | [`cobertura/backend/index.html`](./cobertura/backend/index.html) |
| Frontend | Vitest (v8) | **99,71%** (statements/lines) / 93,31% (branches) / 93,02% (functions) | [`cobertura/frontend/index.html`](./cobertura/frontend/index.html) |

Como gerar novamente:

```bash
# Backend
cd backend
mvn clean test jacoco:report
cp -r target/site/jacoco/* ../cobertura/backend/

# Frontend
cd frontend-cliente
npx vitest run --coverage
cp -r coverage/* ../cobertura/frontend/
```

---

## Log de Auditoria

O sistema audita as principais ações realizadas pelos perfis **SYSADMIN**, **ADMIN** e **CLIENTE**, além de eventos automáticos do sistema (papel `SYSTEM`).

**O que é auditado**
- Login com sucesso e tentativas de login com falha (categoria `AUTH`), capturados automaticamente a partir dos eventos do Spring Security.
- Ações administrativas sobre produtos, categorias, cupons e pedidos (categorias `PRODUTO`, `PEDIDO`, etc.), registradas pelos controllers de admin/sysadmin.
- Ações de gerenciamento de usuários/admins (categoria `USER_MGMT`).
- Cada registro guarda: papel do ator, identificador do ator (e-mail), categoria, descrição da ação, ID do recurso afetado (quando aplicável) e resultado (`SUCCESS`/`FAILURE`).

**Onde fica armazenado**
- Tabela `log_auditoria` (PostgreSQL), criada pela migration `V5__criar_tabela_log_auditoria.sql`.
- Principais colunas: `papel_ator`, `ator`, `categoria`, `descricao`, `recurso_id`, `resultado`, `criado_em`.
- Consultável pela tela `/sysadmin/logs`, com filtros por papel, ator e período.

**Como foi implementado**
- Um **service dedicado** (`AuditoriaService`) centraliza a escrita dos logs, sempre em transação própria (`REQUIRES_NEW`), garantindo que a auditoria seja persistida mesmo que a transação principal sofra rollback e que falhas no log nunca derrubem a requisição.
- Eventos de autenticação são capturados por um **listener** (`AuthAuditoriaListener`) que escuta os eventos nativos do Spring Security (`AuthenticationSuccessEvent` / `AbstractAuthenticationFailureEvent`).
- Ações de negócio (produtos, pedidos, administração) chamam `AuditoriaService` diretamente a partir dos controllers responsáveis.

**Classes/arquivos envolvidos**
- `backend/src/main/java/br/ufpb/dsc/mercado/audit/LogAuditoria.java` — entidade JPA do registro.
- `backend/src/main/java/br/ufpb/dsc/mercado/audit/AuditoriaService.java` — service central de escrita e leitura dos logs.
- `backend/src/main/java/br/ufpb/dsc/mercado/audit/AuthAuditoriaListener.java` — listener de eventos de login/falha de autenticação.
- `backend/src/main/java/br/ufpb/dsc/mercado/audit/LogAuditoriaRepository.java`, `LogAuditoriaRepositoryCustom.java` e `LogAuditoriaRepositoryImpl.java` — persistência e filtros de consulta.
- `backend/src/main/resources/db/migration/V5__criar_tabela_log_auditoria.sql` — criação da tabela.
- Consumido em: `backend/src/main/java/br/ufpb/dsc/mercado/controller/PedidoAdminRestController.java`, `ProdutoController.java`, `AdminController.java`, `SysAdminController.java`.
- Testes: `backend/src/test/java/br/ufpb/dsc/mercado/audit/` (`LogAuditoriaTest`, `AuditoriaServiceTest`, `AuthAuditoriaListenerTest`, `LogAuditoriaRepositoryImplTest`).

---

## Observabilidade (Grafana / OpenTelemetry)

O backend é instrumentado com **OpenTelemetry** e envia traces, métricas e logs para o servidor central da disciplina, hospedado em `otel.dsc.rodrigor.com`. Não é preciso subir Grafana localmente — a stack (Prometheus + Tempo + Loki + Grafana, o "LGTM") é compartilhada por todas as equipes.

- **service.name da equipe:** `dsc-eq05`
- **Painel:** <https://otel.dsc.rodrigor.com> (Explore → filtrar por `service.name = dsc-eq05`)

### Como funciona

A instrumentação usa dois níveis, como recomendado no guia da disciplina (`docs/opentelemetry.md`):

**Automática (zero código)** — o **agente Java** (`opentelemetry-javaagent.jar`) é baixado durante o build da imagem Docker e anexado à JVM via `-javaagent`. Ele instrumenta sozinho:
- todas as requisições HTTP recebidas pelo Spring (Tomcat/Jetty embutido);
- todas as queries JDBC ao PostgreSQL;
- chamadas HTTP de saída (ex.: para o Mercado Pago);
- métricas da JVM (heap, threads, GC);
- logs via Logback, já correlacionados com `trace_id`/`span_id`.

**Manual** — spans de negócio adicionados com a anotação `@WithSpan`, para enriquecer o trace com o que só a regra de negócio sabe que é importante:

| Span | Onde | Arquivo | Atributos |
|------|------|---------|-----------|
| `mercadopago-cobranca` | Cobrança do pedido via Mercado Pago | `MercadoPagoService.cobrarComToken` | `pedido.valor`, `pedido.parcelas`, `pedido.descricao`, `mercadopago.status`, `mercadopago.payment_id` |
| `aplicar-cupom` | Validação de cupom no checkout | `CupomService.validarCupom` | `cupom.codigo`, `cupom.tipo`, `cupom.desconto` |

Os dois métodos são chamados dentro de `PedidoService.criarPedido` (finalizar pedido), então aparecem aninhados no mesmo trace da requisição `POST` de checkout — junto com os spans automáticos de HTTP e SQL.

> ⚠️ O token de pagamento (`Cartao.tokenPagamento`) nunca é anotado como atributo de span — é um dado sensível e fica de fora da telemetria de propósito.

### Configuração

A instrumentação é controlada por variáveis de ambiente, já com os valores certos definidos direto nas imagens Docker (`docker/single/Dockerfile`, `backend/docker/Dockerfile`, `backend/docker/Dockerfile.dev`):

```bash
OTEL_SERVICE_NAME=dsc-eq05
OTEL_EXPORTER_OTLP_ENDPOINT=https://otel.dsc.rodrigor.com
OTEL_EXPORTER_OTLP_PROTOCOL=http/protobuf
OTEL_TRACES_EXPORTER=otlp
OTEL_METRICS_EXPORTER=otlp
OTEL_LOGS_EXPORTER=otlp
```

A única variável que **não** vem no Dockerfile — porque é segredo — é o token de autenticação da turma:

```bash
OTEL_EXPORTER_OTLP_HEADERS=Authorization=Bearer <token-distribuído-no-discord>
```

Ela é injetada em runtime via `.env` (ver `.env.example` na raiz e em `backend/`) e passada ao container pelos `docker-compose` (`dev.yml`, `single.yml`, `prod.yml`). Sem o token, a aplicação sobe normalmente — só a ingestão de telemetria responde `401` e nada aparece no Grafana.

### Dependências (instrumentação manual)

```xml
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-api</artifactId>
</dependency>
<dependency>
    <groupId>io.opentelemetry.instrumentation</groupId>
    <artifactId>opentelemetry-instrumentation-annotations</artifactId>
</dependency>
```

Só a API — o SDK real (quem de fato coleta e exporta) vem do agente Java anexado via `-javaagent`. Sem o agente rodando (ex.: em `mvn test`), as anotações `@WithSpan` viram *no-op*: não quebram nada, só não geram spans.

### Como ver os dados

1. Suba a aplicação normalmente (`docker compose -f docker/compose/dev.yml ... up` ou o deploy de produção) com o token configurado.
2. Gere tráfego: navegue pelo catálogo, faça login, finalize um pedido com cupom.
3. Abra <https://otel.dsc.rodrigor.com> → **Explore** → fonte **Tempo** → busque por `service.name = dsc-eq05`.
4. Abra um trace de checkout e observe a cascata: requisição HTTP → `criarPedido` → queries SQL → `aplicar-cupom` → `mercadopago-cobranca`.
5. Para logs: fonte **Loki**, consulta `{service_name="dsc-eq05"}`. Clicar numa linha com `trace_id` leva direto ao trace correspondente no Tempo.
6. Aba **Dashboards** para métricas de latência, throughput e uso de memória da JVM, coletadas automaticamente.

---

## Integrações com Serviços Externos

O projeto consome dois serviços externos: **Mercado Pago** (pagamentos) e **ViaCEP** (consulta de endereço por CEP).

### Mercado Pago

**Serviço:** [Mercado Pago](https://www.mercadopago.com.br/) (API de pagamentos).

**Para que é usado:** processar a cobrança dos pedidos no checkout. A tokenização do cartão acontece no **frontend**, via SDK JS do Mercado Pago; o **backend** recebe apenas o token gerado e usa a Payment API para efetivar a cobrança — o número do cartão nunca trafega nem é armazenado pelo nosso backend.

**Fluxo:**
1. Frontend chama o SDK JS do Mercado Pago e gera um token a partir dos dados do cartão.
2. Frontend envia o token (mais dados mascarados) para a API do backend.
3. Backend salva o cartão associando-o ao token.
4. Na criação do pedido, o backend usa o token salvo para cobrar via Payment API do Mercado Pago.

**Classes/arquivos envolvidos**
- Backend:
  - `backend/src/main/java/br/ufpb/dsc/mercado/config/MercadoPagoConfiguration.java` — inicializa o SDK com o access token.
  - `backend/src/main/java/br/ufpb/dsc/mercado/service/MercadoPagoService.java` — chama a Payment API (`cobrarComToken`) e trata o resultado/erros da cobrança.
  - `backend/src/test/java/br/ufpb/dsc/mercado/service/MercadoPagoServiceTest.java` — testes do serviço de pagamento.
- Frontend:
  - `frontend-cliente/src/pages/Checkout/Checkout.jsx` — carrega o SDK JS do Mercado Pago e gera o token do cartão no navegador.

**Configuração (variáveis de ambiente, sem expor segredos)**
- Backend: `MERCADOPAGO_ACCESS_TOKEN` (lida em `application-dev.yml` / `application-prod.yml`, propriedade `mercadopago.access-token`).
- Frontend: `VITE_MP_PUBLIC_KEY` (chave pública, usada pelo SDK JS no navegador).

> Observação: o PostgreSQL usado pelo projeto é infraestrutura básica da disciplina e não é considerado integração externa para fins desta avaliação.

---

### ViaCEP

**Serviço:** [ViaCEP](https://viacep.com.br/) (API pública de consulta de endereço a partir do CEP).

**Para que é usado:** preencher automaticamente rua, bairro, cidade e estado ao cadastrar/editar um endereço, evitando digitação manual e erros de digitação pelo cliente.

**Fluxo:**
1. Cliente digita o CEP no formulário (perfil ou cadastro de endereço).
2. O frontend chama diretamente a API pública do ViaCEP (`https://viacep.com.br/ws/{cep}/json/`) via `fetch`, sem passar pelo backend.
3. Se o CEP for válido, os campos de endereço são preenchidos automaticamente com o retorno; se inválido/inexistente, os campos ficam livres para digitação manual.

**Classes/arquivos envolvidos**
- Frontend:
  - `frontend-cliente/src/api/cep.js` — função `buscarCEP`, que chama a API do ViaCEP.
  - `frontend-cliente/src/pages/Profile/Profile.jsx` — usa `buscarCEP` ao editar o endereço no perfil.
  - `frontend-cliente/src/pages/Addresses/Address.jsx` — usa `buscarCEP` ao cadastrar/editar endereços.
  - `frontend-cliente/src/test/apiLayer.test.js` — testes da função `buscarCEP`.

**Configuração:** nenhuma — é uma API pública e gratuita, sem chave de acesso ou variável de ambiente.

---

## Assistentes de IA — Servidor MCP (`pedidos-mcp`)

O backend expõe um **servidor MCP** (Model Context Protocol) que permite a qualquer
assistente de IA compatível (Claude Desktop, Cursor, etc.) consultar o catálogo e
operar pedidos da loja em nome de um cliente, via linguagem natural.

**O que é exposto (tools)**
- `catalogo(busca)` — lista/pesquisa os produtos ativos do cardápio.
- `rastrearPedido(clienteEmail, pedidoId)` — consulta status, itens e totais de um
  pedido, confirmando antes que o pedido pertence ao e-mail informado.
- `montarPedido(clienteEmail, itens, enderecoId, cartaoId, codigoCupom)` — cria e
  finaliza um novo pedido, usando um endereço e um cartão **já cadastrados** pelo
  cliente na loja (a tool não coleta nem manipula dados de cartão diretamente).

**Como foi implementado**
- Camada fina de tools (`@Tool`) que apenas chama os *services* de negócio já
  existentes (`ProdutoService`, `PedidoService`, `UsuarioService`) — nenhuma regra
  de negócio nova foi escrita para o MCP.
- Toda tool que lê ou altera um pedido identifica o cliente pelo e-mail e confere
  que o recurso pertence a ele antes de agir (não há sessão/cookie no transporte MCP).
- Toda tool de escrita (`montarPedido`) registra um evento no **log de auditoria**
  (`AuditoriaService.registrarCliente`, categoria `PEDIDO`), da mesma forma que as
  ações equivalentes feitas pela API/telas normais.
- Servidor iniciado automaticamente pelo `spring-ai-starter-mcp-server-webmvc`,
  configurado em `application.yml` (`spring.ai.mcp.server.name=pedidos-mcp`) e
  disponível em `/mcp` (e `/sse`, transporte HTTP/SSE).

**Classes/arquivos envolvidos**
- `backend/src/main/java/br/ufpb/dsc/mercado/mcp/PedidosTools.java` — as tools.
- `backend/src/main/java/br/ufpb/dsc/mercado/mcp/McpConfig.java` — registra as tools no servidor MCP.
- `backend/src/main/resources/application.yml` — configuração do servidor MCP.
- `backend/src/main/java/br/ufpb/dsc/mercado/config/SecurityConfig.java` — libera `/mcp` e `/sse`.
- Testes: `backend/src/test/java/br/ufpb/dsc/mercado/mcp/PedidosToolsTest.java`.

**Limitação conhecida / próximos passos:** o endpoint `/mcp` hoje é público
(`permitAll`), pensado para uso local/demonstração (ex.: Claude Desktop via stdio
ou um ambiente de teste). Antes de expor esse servidor publicamente em produção,
o recomendado é acrescentar uma camada de autenticação no transporte MCP (ex.:
API key ou token por requisição), já que hoje qualquer chamador pode invocar
`montarPedido` para qualquer e-mail de cliente cadastrado.



---

## Perfis de usuário

| Perfil    | Interface                | Acesso      |
|-----------|--------------------------|-------------|
| Cliente   | React (frontend-cliente) | `/`         |
| Admin     | Thymeleaf (backend)      | `/admin`    |
| SysAdmin  | Thymeleaf (backend)      | `/sysadmin` |
