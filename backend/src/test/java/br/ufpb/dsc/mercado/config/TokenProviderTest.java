package br.ufpb.dsc.mercado.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;

/**
 * Testes unitários puros do TokenProvider.
 *
 * O TokenProvider não depende de nenhum bean Spring — usa apenas HMAC-SHA256
 * da JDK. Por isso instanciamos diretamente e injetamos os campos @Value via
 * ReflectionTestUtils, sem precisar de @SpringBootTest nem Testcontainers.
 */
@DisplayName("TokenProvider — Testes Unitários")
class TokenProviderTest {

    private TokenProvider tokenProvider;

    private static final String SECRET = "segredo-de-teste-com-pelo-menos-32-chars-ok";
    private static final long EXPIRACAO_PADRAO = 3600L; // 1 hora

    @BeforeEach
    void setUp() {
        tokenProvider = new TokenProvider();
        ReflectionTestUtils.setField(tokenProvider, "jwtSecret", SECRET);
        ReflectionTestUtils.setField(tokenProvider, "jwtExpirationSeconds", EXPIRACAO_PADRAO);
    }

    // ── gerarToken ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("gerarToken deve retornar uma string no formato header.payload.signature")
    void gerarToken_deveRetornarTokenNoFormatoJwt() {
        String token = tokenProvider.gerarToken("ana@teste.com", "CLIENTE");

        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("tokens gerados para o mesmo email devem ser consistentes (mesma estrutura)")
    void gerarToken_paraMesmoEmail_deveConterEmailNoPayload() {
        String token = tokenProvider.gerarToken("ana@teste.com", "ADMIN");

        String email = tokenProvider.extrairEmail(token);
        assertThat(email).isEqualTo("ana@teste.com");
    }

    @Test
    @DisplayName("gerarToken deve embutir a role corretamente no payload")
    void gerarToken_roleDeveEstarNoPayload() {
        String token = tokenProvider.gerarToken("admin@teste.com", "SYSADMIN");

        // O payload é a segunda parte do JWT, em Base64URL
        String[] parts = token.split("\\.");
        String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
        assertThat(payload).contains("SYSADMIN");
    }

    // ── validarToken ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("validarToken deve retornar true para um token recém-gerado")
    void validarToken_tokenValido_deveRetornarTrue() {
        String token = tokenProvider.gerarToken("ana@teste.com", "CLIENTE");

        assertThat(tokenProvider.validarToken(token)).isTrue();
    }

    @Test
    @DisplayName("validarToken deve retornar false para um token expirado")
    void validarToken_tokenExpirado_deveRetornarFalse() {
        // Gera token com expiração no passado
        ReflectionTestUtils.setField(tokenProvider, "jwtExpirationSeconds", -1L);
        String tokenExpirado = tokenProvider.gerarToken("ana@teste.com", "CLIENTE");

        assertThat(tokenProvider.validarToken(tokenExpirado)).isFalse();
    }

    @Test
    @DisplayName("validarToken deve retornar false para uma string aleatória (não é JWT)")
    void validarToken_stringAleatoria_deveRetornarFalse() {
        assertThat(tokenProvider.validarToken("isso-nao-e-um-jwt")).isFalse();
    }

    @Test
    @DisplayName("validarToken deve retornar false quando a assinatura foi adulterada")
    void validarToken_assinaturaAdulterada_deveRetornarFalse() {
        String token = tokenProvider.gerarToken("ana@teste.com", "CLIENTE");
        String tokenAdulterado = token.substring(0, token.lastIndexOf('.') + 1) + "assinatura-invalida";

        assertThat(tokenProvider.validarToken(tokenAdulterado)).isFalse();
    }

    @Test
    @DisplayName("validarToken deve retornar false para token com segredo diferente")
    void validarToken_segredoDiferente_deveRetornarFalse() {
        // Gera token com o segredo padrão
        String token = tokenProvider.gerarToken("ana@teste.com", "CLIENTE");

        // Troca o segredo — a validação deve falhar
        ReflectionTestUtils.setField(tokenProvider, "jwtSecret", "outro-segredo-completamente-diferente-x");
        assertThat(tokenProvider.validarToken(token)).isFalse();
    }

    // ── extrairEmail ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("extrairEmail deve retornar o email correto do payload do token")
    void extrairEmail_deveRetornarEmailCorreto() {
        String token = tokenProvider.gerarToken("usuario@empresa.com.br", "ADMIN");

        assertThat(tokenProvider.extrairEmail(token)).isEqualTo("usuario@empresa.com.br");
    }

    @Test
    @DisplayName("extrairEmail deve lançar RuntimeException para token malformado")
    void extrairEmail_tokenMalformado_deveLancarExcecao() {
        assertThatThrownBy(() -> tokenProvider.extrairEmail("nao.e.um.jwt.valido.mesmo"))
                .isInstanceOf(RuntimeException.class);
    }
}
