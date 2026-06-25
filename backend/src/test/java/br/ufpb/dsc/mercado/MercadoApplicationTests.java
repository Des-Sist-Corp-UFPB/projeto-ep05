package br.ufpb.dsc.mercado;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Teste de carregamento do contexto Spring Boot.
 * Usa o banco configurado em application-test.yml.
 */
@SpringBootTest
@ActiveProfiles("test")
class MercadoApplicationTests {

    @Test
    void contextLoads() {
        // Se chegar aqui sem lançar exceção, o contexto carregou com sucesso
    }
}
