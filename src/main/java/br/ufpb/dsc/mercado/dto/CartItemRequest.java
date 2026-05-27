package br.ufpb.dsc.mercado.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CartItemRequest(
    @NotNull(message = "O ID do produto é obrigatório")
    Long produtoId,

    @NotNull(message = "A quantidade é obrigatória")
    @Min(value = 1, message = "A quantidade deve ser de pelo menos 1")
    Integer quantidade
) {}
