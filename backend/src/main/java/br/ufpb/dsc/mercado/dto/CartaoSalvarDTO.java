package br.ufpb.dsc.mercado.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO para salvar um cartão.
 *
 * O token é gerado pelo SDK JS do Mercado Pago no frontend (Checkout.jsx).
 * O backend nunca recebe o número completo nem o CVV — apenas o token seguro,
 * os 4 últimos dígitos (para exibição), a bandeira, o nome do titular e a validade.
 */
public record CartaoSalvarDTO(
    @NotBlank(message = "O nome do titular é obrigatório")
    @Size(max = 150)
    String nomeTitular,

    @NotBlank(message = "A bandeira é obrigatória")
    String bandeira,

    @NotBlank(message = "Os quatro últimos dígitos são obrigatórios")
    @Pattern(regexp = "^\\d{4}$", message = "Deve conter exatamente 4 dígitos")
    String quatroUltimosDigitos,

    @NotBlank(message = "A data de expiração é obrigatória")
    @Pattern(regexp = "^(0[1-9]|1[0-2])\\/\\d{4}$", message = "Deve estar no formato MM/AAAA")
    String dataExpiracao,

    @NotBlank(message = "O token do cartão é obrigatório")
    String tokenMercadoPago
) {}
