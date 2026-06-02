package br.ufpb.dsc.mercado.dto;

import br.ufpb.dsc.mercado.domain.StatusPedido;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PedidoDTO(
    Long id,
    Long clienteId,
    String clienteNome,
    String clienteEmail,
    EnderecoDTO enderecoEntrega,
    CartaoDTO cartao,
    String codigoCupom,
    StatusPedido status,
    BigDecimal totalProdutos,
    BigDecimal totalDesconto,
    BigDecimal totalGeral,
    String codigoRastreamento,
    List<PedidoItemDTO> itens,
    Instant criadoEm,
    Instant atualizadoEm
) {
    public PedidoDTO {
        itens = itens == null ? java.util.List.of() : java.util.List.copyOf(itens);
    }
}
