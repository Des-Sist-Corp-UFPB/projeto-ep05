package br.ufpb.dsc.mercado.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record ProdutoForm(
        @NotBlank(message = "O nome é obrigatório")
        @Size(min = 2, max = 120, message = "O nome deve ter entre 2 e 120 caracteres")
        String nome,

        @Size(max = 2000, message = "A descrição pode ter no máximo 2000 caracteres")
        String descricao,

        @NotNull(message = "O preço é obrigatório")
        @DecimalMin(value = "0.00", message = "O preço não pode ser negativo")
        @Digits(integer = 8, fraction = 2, message = "Preço deve ter no máximo 8 dígitos inteiros e 2 decimais")
        BigDecimal preco,

        Long categoriaId,

        @NotNull(message = "O estoque é obrigatório")
        @Min(value = 0, message = "O estoque não pode ser negativo")
        Integer estoque,

        Boolean ativo,

        String imagensUrls // Campo de texto para as URLs das imagens (separadas por vírgula ou nova linha no form)
) {
}
