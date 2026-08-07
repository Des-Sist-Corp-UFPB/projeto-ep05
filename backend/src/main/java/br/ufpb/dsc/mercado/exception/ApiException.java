package br.ufpb.dsc.mercado.exception;

import org.springframework.http.HttpStatus;

/**
 * Exceção base para erros de negócio da API.
 * Substitui o uso genérico de IllegalArgumentException + retornos manuais
 * de ResponseEntity nos controllers.
 *
 * Uso:
 *   throw new ApiException(HttpStatus.NOT_FOUND, "Usuário não encontrado");
 *   throw new ApiException(HttpStatus.CONFLICT, "E-mail já cadastrado");
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    // Fábricas para os casos mais comuns — evita repetir HttpStatus no código chamador
    public static ApiException naoEncontrado(String mensagem) {
        return new ApiException(HttpStatus.NOT_FOUND, mensagem);
    }

    public static ApiException conflito(String mensagem) {
        return new ApiException(HttpStatus.CONFLICT, mensagem);
    }

    public static ApiException naoAutorizado(String mensagem) {
        return new ApiException(HttpStatus.UNAUTHORIZED, mensagem);
    }

    public static ApiException proibido(String mensagem) {
        return new ApiException(HttpStatus.FORBIDDEN, mensagem);
    }

    public static ApiException requisicaoInvalida(String mensagem) {
        return new ApiException(HttpStatus.BAD_REQUEST, mensagem);
    }
}
