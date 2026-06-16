package br.ufpb.dsc.mercado.dto;

import br.ufpb.dsc.mercado.domain.Papel;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO de cadastro genérico — usado pelo endpoint REST /api/auth/cadastro (frontend React)
 * e pelo SysAdminController para criar admins via formulário web.
 *
 * A validação de senha aqui é intencional simples (@Size min=8) pois:
 * - O frontend React aplica suas próprias regras avançadas antes de enviar
 * - O SysAdmin precisa criar admins com senhas operacionais sem restrições de caractere especial
 *
 * Regras fortes de senha (maiúscula, número, especial) ficam no CadastroClienteRequest,
 * que é exclusivo do formulário web de auto-cadastro de clientes.
 */
public record CadastroRequest(
    @NotBlank(message = "O nome é obrigatório")
    @Size(min = 2, max = 150)
    String nome,

    @NotBlank(message = "O e-mail é obrigatório")
    @Email(message = "E-mail inválido")
    String email,

    @NotBlank(message = "A senha é obrigatória")
    @Size(min = 8, max = 100, message = "A senha deve ter no mínimo 8 caracteres")
    String senha,

    @NotNull(message = "O papel é obrigatório")
    Papel papel
) {}
