package br.ufpb.dsc.mercado.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CheckoutRequest(
    @NotNull(message = "O endereço de entrega é obrigatório")
    Long enderecoId,

    @NotNull(message = "O cartão de pagamento é obrigatório")
    Long cartaoId,

    String codigoCupom,

    @NotEmpty(message = "O carrinho de compras não pode estar vazio")
    List<CartItemRequest> itens
) {
    public CheckoutRequest {
        itens = itens == null ? java.util.List.of() : java.util.List.copyOf(itens);
    }
}
