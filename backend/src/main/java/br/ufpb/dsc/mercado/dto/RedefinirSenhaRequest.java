package br.ufpb.dsc.mercado.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RedefinirSenhaRequest(
    @NotBlank(message = "O token é obrigatório")
    String token,

    @NotBlank(message = "A nova senha é obrigatória")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$",
        message = "A senha deve ter no mínimo 8 caracteres, uma maiúscula, uma minúscula e um número"
    )
    String novaSenha
) {}