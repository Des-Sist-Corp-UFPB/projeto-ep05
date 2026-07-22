# Sweet Delights — Monorepo

Este repositório contém dois projetos que juntos formam a plataforma **Sweet Delights**.

```
projeto-ep05/
├── .env.example              # Variáveis da stack de produção completa
├── docker/                   # Orquestração da stack completa (ver docker/README.md)
│   ├── compose/
│   │   ├── dev.yml              ← ambiente de desenvolvimento
│   │   ├── prod.yml             ← ambiente de produção (ÚNICA fonte de verdade)
│   │   ├── single.yml           ← alternativa: tudo em 1 container
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

A stack de produção (Postgres + backend + frontend + nginx) é orquestrada por
**um único arquivo, na raiz**: `docker/compose/prod.yml`. Não existe
(nem deve existir) outra cópia desse compose em `backend/docker/` — rode
sempre a partir da raiz do monorepo:

```bash
cp .env.example .env   # preencha os valores reais, nunca comite o .env
docker compose -f docker/compose/prod.yml --env-file .env pull
docker compose -f docker/compose/prod.yml --env-file .env up -d
```

O nginx é o único serviço com porta publicada ao host. No servidor da
disciplina (`dsc.rodrigor.com`), o proxy central da turma encaminha o domínio
do grupo para uma porta fixa em `127.0.0.1` — por isso `docker/compose/prod.yml`
publica o nginx numa porta fixa (não configurável por `.env`), combinada
previamente com o responsável pelo servidor. Backend e frontend nunca
publicam porta para o host: todo o tráfego externo passa pelo nginx, que
roteia por caminho (`/api`, `/admin`, `/sysadmin` → backend; o resto → React).

O deploy automático (GitHub Actions, `.github/workflows/deploy.yml`) builda e
publica as 3 imagens no GHCR e depois conecta via SSH no servidor para rodar
`pull` + `up -d` com esse mesmo compose. Segredos necessários no repositório
(Settings → Secrets and variables → Actions): `SSH_DEPLOY_KEY`,
`SSH_USERNAME`, `DEPLOY_PATH` (caminho da raiz do monorepo no servidor) e
`VITE_MP_PUBLIC_KEY`.

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

## Integração com Serviço Externo

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
