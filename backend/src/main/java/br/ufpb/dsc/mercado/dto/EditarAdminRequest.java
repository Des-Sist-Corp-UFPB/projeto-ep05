package br.ufpb.dsc.mercado.dto;

import br.ufpb.dsc.mercado.domain.Papel;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO de edição de admin — senha é opcional.
 * Quando senha é vazia ou nula, a senha atual é mantida.
 *
 * Para criação (senha obrigatória), use {@link CadastroRequest}.
 */
public record EditarAdminRequest(
    @NotBlank(message = "O nome é obrigatório")
    @Size(min = 2, max = 150)
    String nome,

    @NotBlank(message = "O e-mail é obrigatório")
    @Email(message = "E-mail inválido")
    String email,

    // senha é opcional na edição: vazia = mantém a atual; preenchida = mínimo 8 chars
    // A validação de tamanho mínimo é feita manualmente no controller/service
    // pois @Size(min=8) dispara mesmo para strings vazias (""), quebrando o caso de uso
    String senha,

    @NotNull(message = "O papel é obrigatório")
    Papel papel
) {}
