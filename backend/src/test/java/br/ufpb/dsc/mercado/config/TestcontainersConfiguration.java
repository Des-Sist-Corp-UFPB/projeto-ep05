package br.ufpb.dsc.mercado.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Configuração de Testcontainers para testes de integração.
 *
 * <p>Sobe um container PostgreSQL automaticamente antes dos testes e o encerra
 * ao final — sem precisar ter banco local rodando nem subir docker-compose
 * manualmente.
 *
 * <p>Para usar em um teste, basta anotar a classe com:
 * {@code @Import(TestcontainersConfiguration.class)}
 *
 * <p>O {@code @ServiceConnection} conecta automaticamente o datasource do Spring
 * ao container, sobrescrevendo as configurações de URL/usuário/senha do
 * application-test.yml.
 *
 * <p><b>Modo Docker Compose (docker-compose.test.yml):</b><br>
 * O Compose injeta {@code SPRING_DATASOURCE_URL} e o {@code Dockerfile.test}
 * passa {@code -Dtestcontainers.enabled=false} para o Maven, suprimindo este
 * bean. O Spring usa diretamente a URL do banco externo.
 *
 * <p><b>Modo local com Docker (mvn test -P integration):</b><br>
 * Sem a flag acima, {@code matchIfMissing=true} garante que o bean é criado
 * e o Testcontainers sobe um PostgreSQL automaticamente.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    /**
     * Sobe PostgreSQL via Testcontainers.
     *
     * <p>Suprimido quando {@code testcontainers.enabled=false} — usado pelo
     * {@code Dockerfile.test} no modo Docker Compose, onde o banco já existe
     * como serviço externo e não há Docker-in-Docker disponível.
     */
    @Bean
    @ServiceConnection
    @ConditionalOnProperty(name = "testcontainers.enabled", havingValue = "true", matchIfMissing = true)
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                .withDatabaseName("mercado_test")
                .withUsername("mercado")
                .withPassword("mercado123");
    }
}
