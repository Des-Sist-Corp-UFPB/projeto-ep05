# =============================================================================
# Dockerfile do Frontend React (Sweet Delights - Cliente)
# Multi-stage: Node para build, Nginx para servir os estáticos
# =============================================================================

# ---- Estágio 1: Build -------------------------------------------------------
FROM node:22-alpine AS builder

WORKDIR /app

# Copia manifesto primeiro para cache de dependências
COPY package.json package-lock.json ./
RUN npm ci --silent

# Copia o restante do código e gera o build de produção
COPY . .
RUN npm run build

# ---- Estágio 2: Runtime com Nginx -------------------------------------------
FROM nginx:1.27-alpine AS runtime

# Remove config padrão do Nginx
RUN rm /etc/nginx/conf.d/default.conf

# Copia nossa config customizada (suporte a React Router, proxy para a API)
COPY docker/nginx.conf /etc/nginx/conf.d/app.conf

# Copia os arquivos estáticos gerados pelo Vite
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD wget -qO- http://localhost/index.html || exit 1

CMD ["nginx", "-g", "daemon off;"]
