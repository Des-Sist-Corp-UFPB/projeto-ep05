# =============================================================================
# Dockerfile ÚNICO — Frontend (React) + Backend (Spring Boot) + Nginx
# =============================================================================
# Gera UMA imagem só, que sobe UMA única porta (80 por padrão) contendo:
#   - Nginx        → serve o React (SPA) e roteia por caminho para o backend
#   - Spring Boot   → roda internamente em 127.0.0.1:8080 (não exposto ao host)
#
# Mesma lógica de rotas do nginx/nginx.prod.conf original, só que agora o
# "mercado-app-prod:8080" vira "127.0.0.1:8080" (mesmo container).
#
# Roteamento:
#   /api/*, /admin/*, /sysadmin/*, /produtos/*, /logout, /actuator/*,
#   /webjars/*, /images/*, /css/*, /js/*, /ping  → Spring Boot (backend)
#   /*  (tudo o resto)                            → React (frontend, SPA)
#
# Build (a partir da raiz do monorepo projeto-ep05):
#   docker build -f docker/single/Dockerfile -t mercado-app:single \
#     --build-arg VITE_MP_PUBLIC_KEY=$VITE_MP_PUBLIC_KEY .
#
# Run:
#   docker run -p 80:80 \
#     -e SPRING_DATASOURCE_URL=jdbc:postgresql://<host>:5432/mercado_prod \
#     -e SPRING_DATASOURCE_USERNAME=mercado \
#     -e SPRING_DATASOURCE_PASSWORD=... \
#     -e APP_JWT_SECRET=... \
#     -e APP_SEED_ADMIN_PASSWORD=... \
#     -e MERCADOPAGO_ACCESS_TOKEN=... \
#     mercado-app:single
# =============================================================================

# ---- Estágio 1: Build do Frontend (React/Vite) ------------------------------
FROM node:22-alpine AS frontend-build
WORKDIR /app
COPY frontend-cliente/package.json frontend-cliente/package-lock.json ./
RUN npm ci --silent
ARG VITE_MP_PUBLIC_KEY
ENV VITE_MP_PUBLIC_KEY=$VITE_MP_PUBLIC_KEY
COPY frontend-cliente/ ./
RUN npm run build

# ---- Estágio 2: Build do Backend (Spring Boot) -------------------------------
FROM maven:3.9.9-eclipse-temurin-21 AS backend-build
WORKDIR /build
COPY backend/pom.xml .
RUN mvn dependency:go-offline -B -q
COPY backend/src ./src
RUN mvn clean package -DskipTests -B -q

# ---- Estágio 3: Runtime — Nginx + JRE + Supervisor ---------------------------
FROM eclipse-temurin:21-jre-jammy AS runtime

# Instala nginx e supervisor (gerenciador dos dois processos no mesmo container)
RUN apt-get update && \
    apt-get install -y --no-install-recommends nginx supervisor gettext-base wget && \
    rm -rf /var/lib/apt/lists/* && \
    rm -f /etc/nginx/sites-enabled/default

# Usuário não-root para o backend (nginx e supervisord precisam de root para
# bind na porta 80 e gerenciar processos — comportamento padrão dessas ferramentas)
RUN groupadd --gid 1001 mercado && \
    useradd --uid 1001 --gid mercado --shell /bin/false mercado

WORKDIR /app

# JAR do backend
COPY --from=backend-build /build/target/*.jar /app/app.jar
RUN chown mercado:mercado /app/app.jar

# Build estático do React → raiz servida pelo Nginx
COPY --from=frontend-build /app/dist /usr/share/nginx/html

# Config do Nginx (única porta, roteamento por caminho)
COPY docker/single/nginx.conf /etc/nginx/conf.d/app.conf
COPY docker/single/nginx-main.conf /etc/nginx/nginx.conf

# Config do Supervisor (sobe nginx + java no mesmo container)
COPY docker/single/supervisord.conf /etc/supervisor/conf.d/supervisord.conf

# Diretórios de log exigidos pelo supervisor/nginx
RUN mkdir -p /var/log/supervisor && \
    touch /var/log/nginx/access.log /var/log/nginx/error.log

ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -qO- http://127.0.0.1/ping || exit 1

# supervisord roda como root (necessário p/ nginx bindar na porta 80);
# o processo Java é executado como usuário "mercado" (ver supervisord.conf)
ENTRYPOINT ["/usr/bin/supervisord", "-n", "-c", "/etc/supervisor/conf.d/supervisord.conf"]
