package br.ufpb.dsc.mercado.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GlobalExceptionHandler -- Testes Unitarios")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest();
        request.setRequestURI("/api/test");
    }

    @Test
    @DisplayName("ApiException naoEncontrado deve retornar 404")
    void handleApiException_naoEncontrado_deveRetornar404() {
        ApiException ex = ApiException.naoEncontrado("Recurso nao encontrado");
        ResponseEntity<ErroResponse> resposta = handler.handleApiException(ex, request);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resposta.getBody().mensagem()).isEqualTo("Recurso nao encontrado");
    }

    @Test
    @DisplayName("ApiException conflito deve retornar 409")
    void handleApiException_conflito_deveRetornar409() {
        ApiException ex = ApiException.conflito("E-mail ja cadastrado");
        ResponseEntity<ErroResponse> resposta = handler.handleApiException(ex, request);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("ApiException naoAutorizado deve retornar 401")
    void handleApiException_naoAutorizado_deveRetornar401() {
        ApiException ex = ApiException.naoAutorizado("Token invalido");
        ResponseEntity<ErroResponse> resposta = handler.handleApiException(ex, request);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("ApiException proibido deve retornar 403")
    void handleApiException_proibido_deveRetornar403() {
        ApiException ex = ApiException.proibido("Acesso negado");
        ResponseEntity<ErroResponse> resposta = handler.handleApiException(ex, request);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("ApiException requisicaoInvalida deve retornar 400")
    void handleApiException_requisicaoInvalida_deveRetornar400() {
        ApiException ex = ApiException.requisicaoInvalida("Dados invalidos");
        ResponseEntity<ErroResponse> resposta = handler.handleApiException(ex, request);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("IllegalArgumentException deve retornar 400 com a mensagem")
    void handleIllegalArgument_deveRetornar400() {
        IllegalArgumentException ex = new IllegalArgumentException("Argumento invalido");
        ResponseEntity<ErroResponse> resposta = handler.handleIllegalArgument(ex, request);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody().mensagem()).isEqualTo("Argumento invalido");
    }

    @Test
    @DisplayName("Exception generica deve retornar 500 sem expor detalhes internos")
    void handleGeneric_deveRetornar500SemDetalhes() {
        Exception ex = new RuntimeException("erro interno secreto");
        ResponseEntity<ErroResponse> resposta = handler.handleGeneric(ex, request);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(resposta.getBody().mensagem()).doesNotContain("erro interno secreto");
    }

    @Test
    @DisplayName("MethodArgumentNotValidException deve retornar 400 com mensagens de campo")
    void handleValidation_deveRetornar400ComMensagensDeCampo() throws Exception {
        br.ufpb.dsc.mercado.dto.LoginRequest target =
                new br.ufpb.dsc.mercado.dto.LoginRequest("", "");
        BindingResult bindingResult = new BeanPropertyBindingResult(target, "loginRequest");
        bindingResult.rejectValue("email", "NotBlank", "O e-mail e obrigatorio");
        bindingResult.rejectValue("senha",  "NotBlank", "A senha e obrigatoria");

        java.lang.reflect.Method method = String.class.getMethod("toString");
        org.springframework.core.MethodParameter param =
                new org.springframework.core.MethodParameter(method, -1);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(param, bindingResult);

        ResponseEntity<ErroResponse> resposta = handler.handleValidation(ex, request);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody().mensagem())
                .contains("O e-mail e obrigatorio")
                .contains("A senha e obrigatoria");
    }
}
