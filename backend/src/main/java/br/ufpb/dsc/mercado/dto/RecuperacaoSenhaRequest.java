package br.ufpb.dsc.mercado.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RecuperacaoSenhaRequest(
    @NotBlank(message = "O e-mail é obrigatório")
    @Email(message = "E-mail inválido")
    String email
) {}