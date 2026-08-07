package br.ufpb.dsc.mercado.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTOs para o fluxo de recuperação de senha.
 *
 * Fluxo:
 *   1. Cliente envia POST /api/auth/recuperar-senha  { email }
 *      → backend gera token e envia e-mail (ou loga em dev)
 *
 *   2. Cliente envia POST /api/auth/redefinir-senha  { token, novaSenha }
 *      → backend valida o token e salva a nova senha
 */
public class RecuperacaoSenhaDTO {

    /** Passo 1: solicitar o link de redefinição. */
    public record SolicitarRequest(
            @NotBlank(message = "E-mail é obrigatório")
            @Email(message = "E-mail inválido")
            String email
    ) {}

    /** Passo 2: redefinir a senha usando o token recebido. */
    public record RedefinirRequest(
            @NotBlank(message = "Token é obrigatório")
            String token,

            @NotBlank(message = "Nova senha é obrigatória")
            @Size(min = 6, message = "A senha deve ter pelo menos 6 caracteres")
            String novaSenha
    ) {}
}
