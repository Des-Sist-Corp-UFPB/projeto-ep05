package br.ufpb.dsc.mercado.exception;

/**
 * Envelope de erro padronizado para todas as respostas de erro da API.
 *
 * ADICIONADO: campo `timestamp` para rastreabilidade nos logs do cliente.
 *
 * Exemplo de resposta:
 * {
 *   "status": 404,
 *   "mensagem": "Usuário não encontrado",
 *   "caminho": "/api/clientes/perfil",
 *   "timestamp": "2024-01-15T10:30:00Z"
 * }
 */
public record ErroResponse(
        int status,
        String mensagem,
        String caminho,
        String timestamp
) {}
