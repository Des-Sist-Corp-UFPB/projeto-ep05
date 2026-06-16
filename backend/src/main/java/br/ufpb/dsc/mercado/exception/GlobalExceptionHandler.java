package br.ufpb.dsc.mercado.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Tratamento centralizado de erros da API REST.
 *
 * ANTES: controllers capturavam exceções manualmente e retornavam body como
 *        texto puro (ResponseEntity.badRequest().body("mensagem")), misturando
 *        JSON com String dependendo do caminho.
 *
 * AGORA: todas as exceções são capturadas aqui e retornam sempre o mesmo
 *        envelope JSON { status, mensagem, caminho, timestamp }.
 *        Controllers ficam limpos — apenas lançam a exceção adequada.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── Erros de validação de bean (@Valid / @NotBlank etc.) ─────────────────
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        String mensagem = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        return buildResponse(HttpStatus.BAD_REQUEST, mensagem, request);
    }

    // ── Erros de negócio tipados (naoEncontrado, conflito, etc.) ─────────────
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErroResponse> handleApiException(
            ApiException ex,
            HttpServletRequest request) {

        return buildResponse(ex.getStatus(), ex.getMessage(), request);
    }

    // ── Compatibilidade retroativa: IllegalArgumentException legada ───────────
    // Mantida para não quebrar código ainda não migrado. Remover gradualmente.
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErroResponse> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request) {

        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    // ── Qualquer outro erro inesperado ────────────────────────────────────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> handleGeneric(
            Exception ex,
            HttpServletRequest request) {

        // Não expõe detalhes internos ao cliente por segurança
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno do servidor", request);
    }

    // ── Utilitário ───────────────────────────────────────────────────────────
    private ResponseEntity<ErroResponse> buildResponse(
            HttpStatus status, String mensagem, HttpServletRequest request) {

        ErroResponse body = new ErroResponse(
                status.value(),
                mensagem,
                request.getRequestURI(),
                Instant.now().toString()
        );
        return ResponseEntity.status(status).body(body);
    }
}
