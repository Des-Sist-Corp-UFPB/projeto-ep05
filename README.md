# Sweet Delights — Monorepo

Este repositório contém dois projetos que juntos formam a plataforma **Sweet Delights**.

```
projeto-ep05/
├── docker/                  # Orquestração da stack completa
│   ├── docker-compose.dev.yml   ← ambiente de desenvolvimento
│   └── docker-compose.prod.yml  ← ambiente de produção
├── backend/                 # API + painéis admin/sysadmin (Spring Boot + Thymeleaf)
│   └── docker/
│       ├── Dockerfile
│       ├── Dockerfile.dev
│       ├── Dockerfile.test
│       └── docker-compose.test.yml  ← testes isolados do backend
├── frontend-cliente/        # Loja do cliente (React + Vite)
├── TESTING.md               # Documentação centralizada de testes
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
docker compose -f docker/docker-compose.dev.yml up
```

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

## Perfis de usuário

| Perfil    | Interface                | Acesso      |
|-----------|--------------------------|-------------|
| Cliente   | React (frontend-cliente) | `/`         |
| Admin     | Thymeleaf (backend)      | `/admin`    |
| SysAdmin  | Thymeleaf (backend)      | `/sysadmin` |
