package br.ufpb.dsc.mercado.dto;

import java.math.BigDecimal;
import java.util.List;

public record ProdutoDTO(
    Long id,
    String nome,
    String descricao,
    BigDecimal preco,
    Long categoriaId,
    String categoriaNome,
    Integer estoque,
    Boolean ativo,
    List<String> imagensUrls,
    Double notaMedia
) {
    public ProdutoDTO {
        imagensUrls = imagensUrls == null ? java.util.List.of() : java.util.List.copyOf(imagensUrls);
    }
}
