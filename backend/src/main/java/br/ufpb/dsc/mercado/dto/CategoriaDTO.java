package br.ufpb.dsc.mercado.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoriaDTO(
    Long id,

    @NotBlank(message = "O nome é obrigatório")
    @Size(min = 2, max = 100)
    String nome,

    @Size(max = 255)
    String descricao
) {}
