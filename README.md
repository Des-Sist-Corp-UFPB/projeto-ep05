# Sweet Delights — Monorepo

Este repositório contém dois projetos que juntos formam a plataforma **Sweet Delights**.

```
projeto-ep05/
├── backend/           # API + painéis admin/sysadmin (Spring Boot + Thymeleaf)
├── frontend-cliente/  # Loja do cliente (React + Vite)
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
cd backend
# configurar variáveis de ambiente
cp .env.example .env

# subir com Docker
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

---

## Perfis de usuário

| Perfil    | Interface         | Acesso              |
|-----------|-------------------|---------------------|
| Cliente   | React (frontend-cliente) | `/` |
| Admin     | Thymeleaf (backend) | `/admin` |
| SysAdmin  | Thymeleaf (backend) | `/sysadmin` |
