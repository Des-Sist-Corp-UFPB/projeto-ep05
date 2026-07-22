# docker/ — orquestração do projeto

Este diretório concentra tudo relacionado a como subir o projeto via Docker.
Os `Dockerfile`s de cada serviço continuam dentro da própria pasta do serviço
(`backend/docker/`, `frontend-cliente/docker/`, `nginx/`); aqui ficam só as
"receitas" de orquestração (compose) e os artefatos exclusivos do modo
single-container.

## Estrutura

```
docker/
├── README.md          este arquivo
├── compose/            um docker-compose.*.yml por modo de uso
│   ├── dev.yml
│   ├── prod.yml
│   ├── single.yml
│   └── test.yml
└── single/              Dockerfile + configs usados só pelo modo single
    ├── Dockerfile
    ├── nginx.conf
    ├── nginx-main.conf
    └── supervisord.conf
```

## Qual arquivo usar

| Arquivo | Quando usar |
|---|---|
| `compose/dev.yml` | Ambiente local completo (postgres + app + frontend + nginx + adminer), com hot-reload. É o que um dev roda no dia a dia. |
| `compose/prod.yml` | 4 containers separados (postgres, app, frontend, nginx) — é o que roda em produção hoje via CI/CD (`.github/workflows/deploy.yml`). |
| `compose/single.yml` | Tudo (frontend + backend + nginx) numa única imagem/container, via `docker/single/Dockerfile` + supervisord. Útil para deploy simplificado tipo PaaS, onde só se pode rodar um container de app. |
| `compose/test.yml` | Sobe um Postgres descartável + a aplicação em modo `test`, para rodar a suíte de integração isolada (nunca mistura com dados de dev/prod). |

Todos os comandos abaixo são executados a partir da **raiz do projeto**:

```bash
# desenvolvimento
docker compose -f docker/compose/dev.yml --env-file backend/.env.dev --env-file frontend-cliente/.env up --build

# produção
docker compose -f docker/compose/prod.yml --env-file .env up -d --build

# single-container
docker compose -f docker/compose/single.yml --env-file .env up -d --build

# testes de integração
docker compose -f docker/compose/test.yml up --build --abort-on-container-exit
```
