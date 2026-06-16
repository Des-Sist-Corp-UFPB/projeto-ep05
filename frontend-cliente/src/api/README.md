# Configuração da API

O arquivo `api/api.js` contém a URL base da API.

Para alterar a URL, edite a constante BASE_URL no arquivo `src/api/api.js`:

```js
const BASE_URL = "http://localhost:8080/api";  // desenvolvimento local
// const BASE_URL = "https://sua-api.com/api"; // produção
```

## Endpoints utilizados

| Frontend              | Endpoint API                      | Autenticação |
|-----------------------|-----------------------------------|--------------|
| Login                 | POST /api/auth/login              | Pública      |
| Cadastro              | POST /api/auth/cadastro           | Pública      |
| Listar produtos       | GET  /api/produtos                | Pública      |
| Produto por ID        | GET  /api/produtos/{id}           | Pública      |
| Categorias            | GET  /api/produtos/categorias     | Pública      |
| Perfil                | GET  /api/clientes/perfil         | JWT          |
| Atualizar perfil      | PUT  /api/clientes/perfil         | JWT          |
| Excluir conta         | DELETE /api/clientes/perfil       | JWT          |
| Salvar endereço       | POST /api/clientes/enderecos      | JWT          |
| Listar endereços      | GET  /api/clientes/enderecos      | JWT          |
| Criar pedido          | POST /api/pedidos                 | JWT          |
| Validar cupom         | GET  /api/pedidos/cupom/{codigo}  | JWT          |
