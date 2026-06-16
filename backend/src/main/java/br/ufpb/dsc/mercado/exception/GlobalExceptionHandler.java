package br.ufpb.dsc.mercado.exception;

import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {


    // Erros de validação (@Valid) — ex: campo obrigatório vazio
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> handleValidation(
        MethodArgumentNotValidException ex, 
        HttpServletRequest request){


    String mensagem = ex.getBindingResult().getFieldErrors().stream()
        .map(FieldError::getDefaultMessage)
        .collect(Collectors.joining(", "));

    return ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(new ErroResponse(400, mensagem, request.getRequestURI()));
        }


    // Regras de negócio — ex: produto não encontrado, email já cadastrado
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErroResponse> handleIllegalArgument(
            IllegalArgumentException ex, 
            HttpServletRequest request){

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ErroResponse(400, ex.getMessage(), request.getRequestURI()));
    }


    // Produto não encontrado — retorna 404 em vez de 400
    @ExceptionHandler(ProdutoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> handleProdutoNaoEncontrado(
            ProdutoNaoEncontradoException ex, 
            HttpServletRequest request){

        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(new ErroResponse(404, ex.getMessage(), request.getRequestURI()));
    }

    // Qualquer outro erro inesperado
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> handleGeneric(
            Exception ex,
            HttpServletRequest request){

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErroResponse(500, "Erro interno do servidor", request.getRequestURI()));
    }

}