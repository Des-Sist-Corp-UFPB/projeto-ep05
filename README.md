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

## Perfis de usuário

| Perfil    | Interface                | Acesso      |
|-----------|--------------------------|-------------|
| Cliente   | React (frontend-cliente) | `/`         |
| Admin     | Thymeleaf (backend)      | `/admin`    |
| SysAdmin  | Thymeleaf (backend)      | `/sysadmin` |
