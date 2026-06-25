# Relatório de Avaliação — EQ05 (DSC)

| | |
|---|---|
| **Data** | 2026-06-25 |
| **Repositório** | https://github.com/des-sist-corp-ufpb/projeto-ep05 |
| **Aplicação** | https://eq05.dsc.rodrigor.com |
| **Período de atividade** | 2026-06-20 → 2026-06-25 |
| **Total de commits** (sem merges, branch main) | 7 |
| **Integrantes** | Arthur Cezar Silva De Araujo (@UpSnow), Jau Italo Batista Dos Santos (@JauItalo) |

---

## 1. Tecnologias

- Thymeleaf
- Flyway (5 migrations)
- Spring Security
- JWT
- Testcontainers

---

## 2. Análise Funcional

### Endpoints REST (63 mapeados)

| Método | Path | Arquivo |
|--------|------|---------|
| `DELETE` | `/admin/categorias/{id}` | `AdminController.java` |
| `DELETE` | `/admin/cupons/{id}` | `AdminController.java` |
| `GET` | `/admin/, ` | `AdminController.java` |
| `GET` | `/admin/categorias` | `AdminController.java` |
| `GET` | `/admin/categorias/nova` | `AdminController.java` |
| `GET` | `/admin/categorias/{id}/editar` | `AdminController.java` |
| `GET` | `/admin/clientes` | `AdminController.java` |
| `GET` | `/admin/cupons` | `AdminController.java` |
| `GET` | `/admin/cupons/novo` | `AdminController.java` |
| `GET` | `/admin/dashboard` | `AdminController.java` |
| `GET` | `/admin/pedidos` | `AdminController.java` |
| `GET` | `/admin/pedidos/{id}` | `AdminController.java` |
| `POST` | `/admin/categorias` | `AdminController.java` |
| `POST` | `/admin/clientes/{id}/bloquear` | `AdminController.java` |
| `POST` | `/admin/cupons` | `AdminController.java` |
| `POST` | `/admin/cupons/{id}/alternar` | `AdminController.java` |
| `POST` | `/admin/pedidos/{id}/rastreamento` | `AdminController.java` |
| `POST` | `/admin/pedidos/{id}/status` | `AdminController.java` |
| `PUT` | `/admin/categorias/{id}` | `AdminController.java` |
| `GET` | `/admin/cadastro` | `AuthController.java` |
| `GET` | `/admin/login` | `AuthController.java` |
| `POST` | `/admin/cadastro` | `AuthController.java` |
| `POST` | `/api/auth/cadastro` | `AuthRestController.java` |
| `POST` | `/api/auth/login` | `AuthRestController.java` |
| `POST` | `/api/auth/recuperar-senha` | `AuthRestController.java` |
| `POST` | `/api/auth/redefinir-senha` | `AuthRestController.java` |
| `DELETE` | `/api/clientes/cartoes/{id}` | `ClienteRestController.java` |
| `DELETE` | `/api/clientes/enderecos/{id}` | `ClienteRestController.java` |
| `DELETE` | `/api/clientes/perfil` | `ClienteRestController.java` |
| `GET` | `/api/clientes/cartoes` | `ClienteRestController.java` |
| `GET` | `/api/clientes/enderecos` | `ClienteRestController.java` |
| `GET` | `/api/clientes/perfil` | `ClienteRestController.java` |
| `POST` | `/api/clientes/cartoes` | `ClienteRestController.java` |
| `POST` | `/api/clientes/enderecos` | `ClienteRestController.java` |
| `PUT` | `/api/clientes/perfil` | `ClienteRestController.java` |
| `GET` | `/api/pedidos` | `PedidoRestController.java` |
| `GET` | `/api/pedidos/cupom/{codigo}` | `PedidoRestController.java` |
| `GET` | `/api/pedidos/{id}` | `PedidoRestController.java` |
| `POST` | `/api/pedidos` | `PedidoRestController.java` |
| `GET` | `/ping` | `PingController.java` |
| `DELETE` | `/produtos/{id}` | `ProdutoController.java` |
| `GET` | `/produtos` | `ProdutoController.java` |
| `GET` | `/produtos/fragmento-tabela` | `ProdutoController.java` |
| `GET` | `/produtos/novo` | `ProdutoController.java` |
| `GET` | `/produtos/{id}/editar` | `ProdutoController.java` |
| `POST` | `/produtos` | `ProdutoController.java` |
| `PUT` | `/produtos/{id}` | `ProdutoController.java` |
| `GET` | `/api/produtos` | `ProdutoRestController.java` |
| `GET` | `/api/produtos/categorias` | `ProdutoRestController.java` |
| `GET` | `/api/produtos/mais-vendidos` | `ProdutoRestController.java` |
| `GET` | `/api/produtos/{id}` | `ProdutoRestController.java` |
| `GET` | `/api/produtos/{id}/avaliacoes` | `ProdutoRestController.java` |
| `POST` | `/api/produtos/{id}/avaliacoes` | `ProdutoRestController.java` |
| `DELETE` | `/sysadmin/admins/{id}` | `SysAdminController.java` |
| `GET` | `/sysadmin/admins` | `SysAdminController.java` |
| `GET` | `/sysadmin/admins/novo` | `SysAdminController.java` |
| `GET` | `/sysadmin/admins/{id}/editar` | `SysAdminController.java` |
| `GET` | `/sysadmin/configuracoes` | `SysAdminController.java` |
| `GET` | `/sysadmin/dashboard` | `SysAdminController.java` |
| `GET` | `/sysadmin/logs` | `SysAdminController.java` |
| `POST` | `/sysadmin/admins` | `SysAdminController.java` |
| `POST` | `/sysadmin/admins/{id}/bloquear` | `SysAdminController.java` |
| `PUT` | `/sysadmin/admins/{id}` | `SysAdminController.java` |

### Entidades / Tabelas (26 encontradas)

- `categoria`
- `produto`
- `pedido_item`
- `avaliacao`
- `cartao`
- `cupom`
- `endereco`
- `pedido`
- `recuperacao_senha`
- `usuario`
- `produto_imagem`
- `log_auditoria`
- `recuperacao_senha (via V4__criar_tabela_recuperacao_senha.sql)`
- `produto (via V1__criar_tabela_produto.sql)`
- `categoria (via V2__criar_tabelas_sistema.sql)`
- `usuario (via V2__criar_tabelas_sistema.sql)`
- `produto_imagem (via V2__criar_tabelas_sistema.sql)`
- `endereco (via V2__criar_tabelas_sistema.sql)`
- `cartao (via V2__criar_tabelas_sistema.sql)`
- `cupom (via V2__criar_tabelas_sistema.sql)`
- `avaliacao (via V2__criar_tabelas_sistema.sql)`
- `pedido (via V2__criar_tabelas_sistema.sql)`
- `pedido_item (via V2__criar_tabelas_sistema.sql)`
- `log_auditoria (via V5__criar_tabela_log_auditoria.sql)`
- `recuperacao_senha (via R__garantir_tabelas_v4_v5.sql)`
- `log_auditoria (via R__garantir_tabelas_v4_v5.sql)`

### Migrations (6 arquivos)

- `R__garantir_tabelas_v4_v5.sql`
- `V1__criar_tabela_produto.sql`
- `V2__criar_tabelas_sistema.sql`
- `V3__adicionar_campos_usuario.sql`
- `V4__criar_tabela_recuperacao_senha.sql`
- `V5__criar_tabela_log_auditoria.sql`

---

## 3. Análise Arquitetural

| Aspecto | Status | Observação |
|---------|--------|-----------|
| Arquitetura em camadas | ✅ | controller=✅  service=✅  repository=✅ |
| Testes automatizados | ❌ | 0 arquivo(s) de teste |
| Migrations versionadas | ✅ | 6 migration(s) |
| Logging | ✅ | @Slf4j / LoggerFactory / logging.getLogger detectado |
| Autenticação / Segurança | ✅ | Spring Security / JWT / decorator detectado |
| DTOs / Separação de dados | ✅ | classes *DTO / *Request / *Response detectadas |
| Tratamento global de exceções | ✅ | @ControllerAdvice / @ExceptionHandler detectado |
| Documentação de API (OpenAPI) | ❌ | não detectado |
| Variáveis de ambiente | ✅ | .env / @Value / os.environ detectado |
| Dockerfile / docker-compose | ❌ | não encontrado |

---

## 4. Contribuição por Usuário

### Resumo

| Usuário | Commits (main) | Commits (GitHub API) | Linhas adicionadas | Linhas no código atual | % código atual |
|---------|---------------|---------------------|-------------------|----------------------|----------------|
| Arthur Cezar Silva De Araujo (@UpSnow) | 6 | **70** ⚠️ | 44.815 | 15.873 | 100% |
| Jau Italo Batista Dos Santos (@JauItalo) | 0 | **41** ⚠️ | 0 | 0 | 0% |
| *(sem login GitHub)* | 1 | 14% | — | — | — |

> **⚠️ Divergência entre commits locais e GitHub API:**
> - **@UpSnow**: 6 commit(s) na branch `main` vs **70** registrados na API GitHub (commits em branches não mergeadas ou absorvidos via squash-merge sem preservação de autoria).
> - **@JauItalo**: 0 commit(s) na branch `main` vs **41** registrados na API GitHub (commits em branches não mergeadas ou absorvidos via squash-merge sem preservação de autoria).
>

### Contribuição por Camada

| Camada | Total linhas | Arthur Cezar Silva De Araujo (@UpSnow) | Jau Italo Batista Dos Santos (@JauItalo) |
|--------|-------------|---------|---------|
| Controller | 6.322 | 100% | 0% |
| Frontend | 2.133 | 100% | 0% |
| Repository | 259 | 100% | 0% |
| Service | 2.988 | 100% | 0% |
| Test | 948 | 100% | 0% |

---

## 5. Contribuição por Funcionalidade

Baseado em `git blame` nos arquivos de controller e service.

| Arquivo | Total linhas | Arthur Cezar Silva De Araujo (@UpSnow) | Jau Italo Batista Dos Santos (@JauItalo) |
|---------|-------------|---------|---------|
| `PedidoServiceTest.java` | 530 | 100% | 0% |
| `layout.html` | 420 | 100% | 0% |
| `cadastro.html` | 420 | 100% | 0% |
| `UsuarioService.java` | 296 | 100% | 0% |
| `AdminController.java` | 293 | 100% | 0% |
| `ProdutoRestControllerTest.java` | 279 | 100% | 0% |
| `login.html` | 270 | 100% | 0% |
| `SysAdminControllerTest.java` | 251 | 100% | 0% |
| `SysAdminController.java` | 250 | 100% | 0% |
| `PedidoService.java` | 237 | 100% | 0% |
| `AdminControllerTest.java` | 237 | 100% | 0% |
| `CupomServiceTest.java` | 234 | 100% | 0% |
| `PedidoRestControllerTest.java` | 213 | 100% | 0% |
| `ClienteRestControllerTest.java` | 208 | 100% | 0% |
| `dashboard.html` | 199 | 100% | 0% |
| `UsuarioServiceTest.java` | 195 | 100% | 0% |
| `CategoriaServiceTest.java` | 194 | 100% | 0% |
| `AuthRestControllerTest.java` | 193 | 100% | 0% |
| `form.html` | 191 | 100% | 0% |
| `ProdutoServiceTest.java` | 188 | 100% | 0% |
| `ProdutoController.java` | 185 | 100% | 0% |
| `ProdutoService.java` | 173 | 100% | 0% |
| `ProdutoControllerTest.java` | 168 | 100% | 0% |
| `AuditoriaServiceTest.java` | 161 | 100% | 0% |
| `ClienteRestController.java` | 155 | 100% | 0% |
| `AvaliacaoServiceTest.java` | 143 | 100% | 0% |
| `form_admin.html` | 142 | 100% | 0% |
| `ProdutoRestController.java` | 132 | 100% | 0% |
| `RecuperacaoSenhaServiceTest.java` | 130 | 100% | 0% |
| `AuditoriaService.java` | 125 | 100% | 0% |
| `AuthControllerTest.java` | 125 | 100% | 0% |
| `lista.html` | 122 | 100% | 0% |
| `V2__criar_tabelas_sistema.sql` | 116 | 100% | 0% |
| `tabela_admins.html` | 116 | 100% | 0% |
| `AuthRestController.java` | 107 | 100% | 0% |
| `tabela.html` | 107 | 100% | 0% |
| `modal_pedido.html` | 105 | 100% | 0% |
| `PedidoRestController.java` | 92 | 100% | 0% |
| `AuthController.java` | 90 | 100% | 0% |
| `CupomService.java` | 89 | 100% | 0% |
| `form_categoria.html` | 85 | 100% | 0% |
| `tabela_logs.html` | 79 | 100% | 0% |
| `tabela_cupons.html` | 73 | 100% | 0% |
| `admins.html` | 70 | 100% | 0% |
| `linha.html` | 69 | 100% | 0% |
| `tabela_clientes.html` | 68 | 100% | 0% |
| `RecuperacaoSenhaService.java` | 67 | 100% | 0% |
| `form_cupom.html` | 67 | 100% | 0% |
| `CategoriaService.java` | 63 | 100% | 0% |
| `tabela_pedidos.html` | 62 | 100% | 0% |
| `AvaliacaoService.java` | 61 | 100% | 0% |
| `tabela_categorias.html` | 61 | 100% | 0% |
| `logs.html` | 61 | 100% | 0% |
| `configuracoes.html` | 58 | 100% | 0% |
| `clientes.html` | 55 | 100% | 0% |
| `CustomUserDetailsService.java` | 43 | 100% | 0% |
| `MercadoApplication.java` | 40 | 100% | 0% |
| `linha_cupom.html` | 39 | 100% | 0% |
| `cupons.html` | 36 | 100% | 0% |
| `categorias.html` | 36 | 100% | 0% |
| `linha_admin.html` | 34 | 100% | 0% |
| `R__garantir_tabelas_v4_v5.sql` | 31 | 100% | 0% |
| `linha_pedido.html` | 28 | 100% | 0% |
| `pedidos.html` | 26 | 100% | 0% |
| `V1__criar_tabela_produto.sql` | 25 | 100% | 0% |
| `linha_categoria.html` | 25 | 100% | 0% |
| `MercadoApplicationTests.java` | 19 | 100% | 0% |
| `PingController.java` | 17 | 100% | 0% |
| `V5__criar_tabela_log_auditoria.sql` | 16 | 100% | 0% |
| `V4__criar_tabela_recuperacao_senha.sql` | 10 | 100% | 0% |
| `V3__adicionar_campos_usuario.sql` | 5 | 100% | 0% |

---

*Relatório gerado automaticamente em 2026-06-25.*
*Os dados de contribuição são baseados em `git log --numstat` (linhas adicionadas) e `git blame` (linhas no código atual), excluindo commits de merge.*