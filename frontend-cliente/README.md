# Sweet Delights — Frontend Cliente

Interface React do e-commerce Sweet Delights. Consome a API REST do backend Spring Boot e fornece ao usuário final as telas de catálogo, carrinho, checkout, perfil e recuperação de senha.

---

## Pré-requisitos

- Node.js 18+
- npm (incluído com Node)
- Backend rodando em `http://localhost:8080` (ou configure `VITE_API_URL`)

---

## Como rodar localmente

```bash
cd frontend-cliente

npm install        # instala dependências (apenas na primeira vez)
npm run dev        # inicia em http://localhost:5173
```

### Configurar a URL da API

Crie um arquivo `.env.local` na raiz de `frontend-cliente/`:

```env
VITE_API_URL=http://localhost:8080
```

Por padrão, `src/api/api.js` usa `http://localhost:8080` se a variável não estiver definida.

---

## Build de produção

```bash
npm run build      # gera dist/
npm run preview    # visualiza o build localmente
```

A imagem Docker de produção está em `frontend-cliente/docker/Dockerfile` e é orquestrada pelo `docker/docker-compose.prod.yml` na raiz do monorepo.

---

## Testes

```bash
npm test              # roda todos os testes (com cobertura)
npm run test:watch    # modo watch — ideal durante desenvolvimento
npm run test:ui       # interface visual no navegador
```

Cobertura mínima exigida: **85%** em linhas, funções, branches e statements. Veja o `TESTING.md` na raiz do projeto para a documentação completa dos 22 arquivos de teste.

---

## Estrutura principal

```
frontend-cliente/
├── src/
│   ├── api/          ← funções de chamada à API REST
│   ├── components/   ← componentes reutilizáveis
│   ├── context/      ← AuthContext, CartContext
│   ├── pages/        ← uma pasta por página
│   ├── Routes/       ← definição de rotas (PrivateRoute)
│   └── utils/        ← validações e máscaras
├── docker/
│   ├── Dockerfile    ← imagem de produção (nginx)
│   └── nginx.conf
└── vite.config.js    ← configuração do Vite + Vitest
```
