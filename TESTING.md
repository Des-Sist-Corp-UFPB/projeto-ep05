# Sweet Delights — Guia de Testes

Este documento centraliza tudo sobre testes do monorepo: como rodar, o que cada suite cobre e onde encontrar os arquivos.

---

## Visão Geral

| Módulo | Framework | Qtd. de arquivos de teste | Cobertura mínima |
|---|---|---|---|
| `backend` | JUnit 5 + Mockito + Testcontainers | 21 arquivos | via JaCoCo (`mvn verify`) |
| `frontend-cliente` | Vitest + Testing Library | 22 arquivos | 85% (linhas, funções, branches, statements) |

---

## Backend (Spring Boot / Java)

### Pré-requisitos

- Java 21
- Maven 3.9+
- Docker em execução (para Testcontainers e para o ambiente de teste via Docker Compose)

### Rodar os testes

```bash
cd backend

# Roda todos os testes unitários e de integração
mvn test

# Roda com relatório de cobertura JaCoCo
mvn verify
# Relatório: abra target/site/jacoco/index.html no navegador

# Ambiente de testes isolado via Docker Compose
# (banco mercado_test separado, dados descartados ao final)
docker compose -f docker/docker-compose.test.yml up --build --abort-on-container-exit
docker compose -f docker/docker-compose.test.yml down -v  # limpa após rodar
```

### O que é testado

| Pacote | Arquivo | O que cobre |
|---|---|---|
| `audit` | `AuditoriaServiceTest` | Registro e consulta de logs de auditoria |
| `audit` | `LogAuditoriaTest` | Entidade `LogAuditoria` (campos, construção) |
| `config` | `TokenProviderTest` | Geração e validação de tokens JWT |
| `controller` | `AdminControllerTest` | Endpoints do painel admin (Thymeleaf) |
| `controller` | `AuthControllerTest` | Login/logout via formulário web |
| `controller` | `AuthRestControllerTest` | Login via API REST (`/api/auth/**`) |
| `controller` | `ClienteRestControllerTest` | CRUD de perfil do cliente (`/api/clientes/**`) |
| `controller` | `PedidoRestControllerTest` | Criação e consulta de pedidos (`/api/pedidos/**`) |
| `controller` | `ProdutoControllerTest` | Endpoints admin de produto (Thymeleaf) |
| `controller` | `ProdutoRestControllerTest` | CRUD REST de produtos (`/api/produtos/**`) |
| `controller` | `SysAdminControllerTest` | Endpoints do painel sysadmin |
| `exception` | `GlobalExceptionHandlerTest` | Respostas de erro padronizadas |
| `service` | `AvaliacaoServiceTest` | Regras de negócio de avaliações |
| `service` | `CategoriaServiceTest` | Criação e listagem de categorias |
| `service` | `CupomServiceTest` | Validação e aplicação de cupons |
| `service` | `PedidoServiceTest` | Fluxo completo de pedido (carrinho → checkout) |
| `service` | `ProdutoServiceTest` | CRUD e regras de produto |
| `service` | `RecuperacaoSenhaServiceTest` | Fluxo de recuperação de senha |
| `service` | `UsuarioServiceTest` | Cadastro, edição e bloqueio de usuários |

### Análise de segurança (SAST)

```bash
# SpotBugs + FindSecBugs + OWASP Dependency Check
mvn verify -Psecurity

# Trivy — scan de vulnerabilidades no filesystem
docker compose -f docker/docker-compose.dev.yml --profile scan up trivy

# Dependências desatualizadas
mvn versions:display-dependency-updates -Pversions
```

> Veja `backend/docs/SECURITY.md` para detalhes sobre supressões e falsos positivos.

---

## Frontend (React / Vitest)

### Pré-requisitos

- Node.js 18+
- npm (já incluído com Node)

### Rodar os testes

```bash
cd frontend-cliente

npm install          # apenas na primeira vez

# Roda todos os testes uma vez (com cobertura)
npm test

# Modo watch — ideal durante desenvolvimento
npm run test:watch

# Interface visual no navegador
npm run test:ui
```

O relatório de cobertura HTML é gerado em `frontend-cliente/coverage/index.html`.

### Threshold mínimo de cobertura

O build falha automaticamente se qualquer métrica ficar abaixo de **85%**:

| Métrica | Mínimo |
|---|---|
| Linhas | 85% |
| Funções | 85% |
| Branches | 85% |
| Statements | 85% |

### O que é testado

| Arquivo | O que cobre |
|---|---|
| `apiLayer.test.js` | Funções de chamada à API (`api.js`, `productApi.js`, `cep.js`) |
| `components.test.jsx` | Componentes reutilizáveis (Button, Input, Loading, ErrorState, etc.) |
| `smallComponents.test.jsx` | Componentes menores (BackButton, QuantityControl, ScrollContainer, etc.) |
| `context.test.jsx` | `AuthContext` e `CartContext` — estado global da aplicação |
| `routes.test.jsx` | Definição de rotas e proteção de rotas privadas (`PrivateRoute`) |
| `mascaras.test.js` | Funções de máscara de cartão e endereço |
| `validarCartao.test.js` | Validação de número, validade e CVV de cartão |
| `validarEndereco.test.js` | Validação dos campos de endereço |
| `validarFormulario.test.js` | Validação genérica de formulários de cadastro |
| `pageHome.test.jsx` | Página inicial |
| `pageLoja.test.jsx` | Listagem de produtos da loja |
| `pageCategories.test.jsx` | Filtro e navegação por categorias |
| `pageDetails.test.jsx` | Página de detalhe do produto |
| `pageCartProduct.test.jsx` | Carrinho de compras |
| `pageCheckout.test.jsx` | Fluxo de checkout |
| `pageAddress.test.jsx` | Gerenciamento de endereços |
| `pageProfile.test.jsx` | Perfil do usuário |
| `pageLogin.test.jsx` | Página de login |
| `pageRegister.test.jsx` | Página de cadastro |
| `pageForgotPassword.test.jsx` | Recuperação de senha |
| `pagePromotion.test.jsx` | Página de promoções |

---

## Estrutura dos arquivos de teste

```
projeto-ep05/
├── backend/
│   ├── docker/
│   │   └── docker-compose.test.yml     ← ambiente Docker isolado para testes
│   └── src/
│       └── test/
│           └── java/br/ufpb/dsc/mercado/
│               ├── MercadoApplicationTests.java
│               ├── audit/
│               ├── config/              ← inclui TestcontainersConfiguration
│               ├── controller/
│               ├── exception/
│               └── service/
│
└── frontend-cliente/
    ├── vite.config.js                  ← configuração do Vitest (thresholds, jsdom)
    └── src/
        └── test/
            ├── setup.js                ← setup global (@testing-library/jest-dom)
            ├── apiLayer.test.js
            ├── components.test.jsx
            ├── context.test.jsx
            ├── routes.test.jsx
            ├── mascaras.test.js
            ├── validarCartao.test.js
            ├── validarEndereco.test.js
            ├── validarFormulario.test.js
            └── page*.test.jsx          ← um arquivo por página
```

---

## CI/CD

Os testes rodam automaticamente no GitHub Actions a cada `push` na branch `main`.

Pipeline: **Testes e SAST → Build e push → Deploy em produção**

Veja `.github/workflows/deploy.yml` para a configuração completa.
