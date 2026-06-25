package br.ufpb.dsc.mercado.config;

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
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                .withDatabaseName("mercado_test")
                .withUsername("mercado")
                .withPassword("mercado123");
    }
}
