# =============================================================================
# Dockerfile de PRODUÇÃO — Multi-stage build
# =============================================================================
# Estágio 1 (builder): compila o projeto com Maven e gera o JAR.
# Estágio 2 (runtime): imagem mínima com apenas o JRE, sem ferramentas de build.
#
# Vantagens do multi-stage:
#   - Imagem final muito menor (sem Maven, sem fontes Java, sem classes temp.)
#   - Superfície de ataque reduzida (menos binários = menos vulnerabilidades)
#   - Separação entre ambiente de build e runtime
#
# Como construir:
#   docker build -f docker/Dockerfile -t mercado:latest .
# =============================================================================

# ---- Estágio 1: Build -------------------------------------------------------
FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /build

# Copiar pom.xml primeiro para cache de dependências
COPY pom.xml .
RUN mvn dependency:go-offline -B -q

# Copiar código-fonte e compilar
COPY src ./src
RUN mvn clean package -DskipTests -B -q

# ---- Estágio 1b: Agente Java do OpenTelemetry (observabilidade) ------------
FROM alpine:3.20 AS otel-agent
RUN apk add --no-cache curl && \
    curl -L -o /opentelemetry-javaagent.jar \
    https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar

# ---- Estágio 2: Runtime -----------------------------------------------------
FROM eclipse-temurin:21-jre-jammy AS runtime

# Criar usuário não-root por segurança
# Executar aplicações como root em container é má prática de segurança
RUN groupadd --gid 1001 mercado && \
    useradd --uid 1001 --gid mercado --shell /bin/false mercado

WORKDIR /app

# Copiar apenas o JAR do estágio anterior
COPY --from=builder /build/target/*.jar app.jar

# Agente Java do OpenTelemetry (observabilidade — traces, métricas e logs)
COPY --from=otel-agent /opentelemetry-javaagent.jar otel-javaagent.jar

# Definir owner dos arquivos
RUN chown mercado:mercado app.jar otel-javaagent.jar

# Trocar para usuário não-root
USER mercado

# OTEL_SERVICE_NAME e o endpoint são fixos do projeto; o TOKEN da turma NUNCA
# fica aqui — vem por variável de ambiente injetada no `docker run`/compose
# (ver docker/compose/prod.yml e docs/opentelemetry.md).
ENV JAVA_TOOL_OPTIONS="-javaagent:/app/otel-javaagent.jar"
ENV OTEL_SERVICE_NAME="dsc-eq05"
ENV OTEL_EXPORTER_OTLP_ENDPOINT="https://otel.dsc.rodrigor.com"
ENV OTEL_EXPORTER_OTLP_PROTOCOL="http/protobuf"
ENV OTEL_TRACES_EXPORTER="otlp"
ENV OTEL_METRICS_EXPORTER="otlp"
ENV OTEL_LOGS_EXPORTER="otlp"

EXPOSE 8080

# Health check: o Actuator expõe /actuator/health
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1

# Opções JVM para produção:
#   -XX:+UseContainerSupport: respeita limites de memória do container
#   -XX:MaxRAMPercentage=75: usa até 75% da RAM disponível para o heap
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Dspring.profiles.active=prod", \
    "-jar", "app.jar"]
