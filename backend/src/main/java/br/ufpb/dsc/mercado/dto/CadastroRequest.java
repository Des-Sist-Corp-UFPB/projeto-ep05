package br.ufpb.dsc.mercado.dto;

import br.ufpb.dsc.mercado.domain.Papel;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CadastroRequest(
    @NotBlank(message = "O nome é obrigatório")
    @Size(min = 2, max = 150)
    String nome,

    @NotBlank(message = "O e-mail é obrigatório")
    @Email(message = "E-mail inválido")
    String email,

    @NotBlank(message = "A senha é obrigatória")
    @Size(min = 6, max = 100)
    String senha,

    @NotNull(message = "O papel é obrigatório")
    Papel papel
) {}
