package br.ufpb.dsc.mercado.dto;

import java.math.BigDecimal;

public record PedidoItemDTO(
    Long produtoId,
    String produtoNome,
    Integer quantidade,
    BigDecimal precoUnitario
) {}
