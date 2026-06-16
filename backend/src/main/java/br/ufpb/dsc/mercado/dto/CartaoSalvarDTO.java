package br.ufpb.dsc.mercado.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CartaoSalvarDTO(
    @NotBlank(message = "O nome do titular é obrigatório")
    @Size(max = 150)
    String nomeTitular,

    @NotBlank(message = "O número do cartão é obrigatório")
    @Pattern(regexp = "^\\d{16}$", message = "Deve conter exatamente 16 dígitos numéricos")
    String numeroCartao,

    @NotBlank(message = "A bandeira é obrigatória")
    String bandeira,

    @NotBlank(message = "A data de expiração é obrigatória")
    @Pattern(regexp = "^(0[1-9]|1[0-2])\\/\\d{4}$", message = "Deve estar no formato MM/AAAA")
    String dataExpiracao,

    @NotBlank(message = "O CVV é obrigatório")
    @Pattern(regexp = "^\\d{3,4}$", message = "CVV inválido")
    String cvv
) {}
