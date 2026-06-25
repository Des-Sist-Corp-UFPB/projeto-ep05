# Relatório de Avaliação — EQ05 (DSC)

| | |
|---|---|
| **Data** | 2026-06-25 |
| **Repositório** | https://github.com/des-sist-corp-ufpb/projeto-ep05 |
| **Aplicação** | https://eq05.dsc.rodrigor.com |
| **Período de atividade** | 2026-06-20 → 2026-06-20 |
| **Total de commits** (sem merges) | 1 |
| **Integrantes** | Arthur Cezar Silva De Araujo (@UpSnow) |

---

## 1. Tecnologias

- Thymeleaf
- Flyway (4 migrations)
- Spring Security
- JWT
- Testcontainers

---

## 2. Análise Funcional

### Endpoints REST (62 mapeados)

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
| `GET` | `/sysadmin/admins` | `SysAdminController.java` |
| `GET` | `/sysadmin/admins/novo` | `SysAdminController.java` |
| `GET` | `/sysadmin/admins/{id}/editar` | `SysAdminController.java` |
| `GET` | `/sysadmin/configuracoes` | `SysAdminController.java` |
| `GET` | `/sysadmin/dashboard` | `SysAdminController.java` |
| `GET` | `/sysadmin/logs` | `SysAdminController.java` |
| `POST` | `/sysadmin/admins` | `SysAdminController.java` |
| `POST` | `/sysadmin/admins/{id}/bloquear` | `SysAdminController.java` |
| `PUT` | `/sysadmin/admins/{id}` | `SysAdminController.java` |

### Entidades / Tabelas (22 encontradas)

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

### Migrations (4 arquivos)

- `V1__criar_tabela_produto.sql`
- `V2__criar_tabelas_sistema.sql`
- `V3__adicionar_campos_usuario.sql`
- `V4__criar_tabela_recuperacao_senha.sql`

---

## 3. Análise Arquitetural

| Aspecto | Status | Observação |
|---------|--------|-----------|
| Arquitetura em camadas | ✅ | controller=✅  service=✅  repository=✅ |
| Testes automatizados | ❌ | 0 arquivo(s) de teste |
| Migrations versionadas | ✅ | 4 migration(s) |
| Logging | ❌ | não detectado |
| Autenticação / Segurança | ✅ | Spring Security / JWT / decorator detectado |
| DTOs / Separação de dados | ✅ | classes *DTO / *Request / *Response detectadas |
| Tratamento global de exceções | ✅ | @ControllerAdvice / @ExceptionHandler detectado |
| Documentação de API (OpenAPI) | ❌ | não detectado |
| Variáveis de ambiente | ✅ | .env / @Value / os.environ detectado |
| Dockerfile / docker-compose | ❌ | não encontrado |

---

## 4. Contribuição por Usuário

### Resumo

| Usuário | Commits | % commits | Linhas adicionadas | Linhas no código atual | % código atual |
|---------|---------|-----------|-------------------|----------------------|----------------|
| Arthur Cezar Silva De Araujo (@UpSnow) | 1 | 100% | 19.043 | 11.431 | 100% |

### Contribuição por Camada

| Camada | Total linhas | Arthur Cezar Silva De Araujo (@UpSnow) |
|--------|-------------|---------|
| Controller | 4.810 | 100% |
| Frontend | 2.133 | 100% |
| Repository | 178 | 100% |
| Service | 1.279 | 100% |

---

## 5. Contribuição por Funcionalidade

Baseado em `git blame` nos arquivos de controller e service.

| Arquivo | Total linhas | Arthur Cezar Silva De Araujo (@UpSnow) |
|---------|-------------|---------|
| `cadastro.html` | 420 | 100% |
| `layout.html` | 412 | 100% |
| `AdminController.java` | 314 | 100% |
| `UsuarioService.java` | 273 | 100% |
| `login.html` | 270 | 100% |
| `PedidoService.java` | 237 | 100% |
| `SysAdminController.java` | 230 | 100% |
| `dashboard.html` | 199 | 100% |
| `form.html` | 191 | 100% |
| `ProdutoServiceTest.java` | 188 | 100% |
| `SysAdminControllerTest.java` | 183 | 100% |
| `ProdutoControllerTest.java` | 176 | 100% |
| `ProdutoService.java` | 173 | 100% |
| `ClienteRestController.java` | 155 | 100% |
| `ProdutoController.java` | 153 | 100% |
| `form_admin.html` | 142 | 100% |
| `ProdutoRestController.java` | 132 | 100% |
| `lista.html` | 122 | 100% |
| `V2__criar_tabelas_sistema.sql` | 116 | 100% |
| `tabela_admins.html` | 108 | 100% |
| `AuthRestController.java` | 107 | 100% |
| `tabela.html` | 107 | 100% |
| `modal_pedido.html` | 105 | 100% |
| `PedidoRestController.java` | 92 | 100% |
| `AuthController.java` | 90 | 100% |
| `CupomService.java` | 89 | 100% |
| `form_categoria.html` | 85 | 100% |
| `tabela_cupons.html` | 73 | 100% |
| `admins.html` | 70 | 100% |
| `linha.html` | 69 | 100% |
| `RecuperacaoSenhaService.java` | 67 | 100% |
| `form_cupom.html` | 67 | 100% |
| `CategoriaService.java` | 63 | 100% |
| `tabela_pedidos.html` | 62 | 100% |
| `AvaliacaoService.java` | 61 | 100% |
| `tabela_categorias.html` | 61 | 100% |
| `tabela_clientes.html` | 59 | 100% |
| `configuracoes.html` | 58 | 100% |
| `clientes.html` | 55 | 100% |
| `logs.html` | 46 | 100% |
| `MercadoApplicationTests.java` | 45 | 100% |
| `CustomUserDetailsService.java` | 43 | 100% |
| `MercadoApplication.java` | 40 | 100% |
| `linha_cupom.html` | 39 | 100% |
| `cupons.html` | 36 | 100% |
| `categorias.html` | 36 | 100% |
| `linha_admin.html` | 34 | 100% |
| `linha_pedido.html` | 28 | 100% |
| `pedidos.html` | 26 | 100% |
| `V1__criar_tabela_produto.sql` | 25 | 100% |
| `linha_categoria.html` | 25 | 100% |
| `PingController.java` | 17 | 100% |
| `V4__criar_tabela_recuperacao_senha.sql` | 10 | 100% |
| `V3__adicionar_campos_usuario.sql` | 5 | 100% |

---

*Relatório gerado automaticamente em 2026-06-25.*
*Os dados de contribuição são baseados em `git log --numstat` (linhas adicionadas) e `git blame` (linhas no código atual), excluindo commits de merge.*