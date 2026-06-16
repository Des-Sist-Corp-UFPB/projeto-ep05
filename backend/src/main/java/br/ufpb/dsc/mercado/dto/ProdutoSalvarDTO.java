package br.ufpb.dsc.mercado.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public record ProdutoSalvarDTO(
    @NotBlank(message = "O nome é obrigatório")
    @Size(min = 2, max = 120)
    String nome,

    @Size(max = 2000)
    String descricao,

    @NotNull(message = "O preço é obrigatório")
    @DecimalMin(value = "0.00")
    BigDecimal preco,

    Long categoriaId,

    @NotNull(message = "O estoque é obrigatório")
    @Min(0)
    Integer estoque,

    Boolean ativo,

    List<String> imagensUrls
) {
    public ProdutoSalvarDTO {
        imagensUrls = imagensUrls == null ? java.util.List.of() : java.util.List.copyOf(imagensUrls);
    }
}
