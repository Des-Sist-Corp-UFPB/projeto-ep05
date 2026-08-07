package br.ufpb.dsc.mercado.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EnderecoDTO(
    Long id,

    @NotBlank(message = "O logradouro é obrigatório")
    @Size(max = 150)
    String logradouro,

    @NotBlank(message = "O número é obrigatório")
    @Size(max = 20)
    String numero,

    @Size(max = 100)
    String complemento,

    @NotBlank(message = "O bairro é obrigatório")
    @Size(max = 100)
    String bairro,

    @NotBlank(message = "A cidade é obrigatória")
    @Size(max = 100)
    String cidade,

    @NotBlank(message = "O estado é obrigatório")
    @Size(min = 2, max = 2)
    String estado,

    @NotBlank(message = "O CEP é obrigatório")
    @Size(max = 10)
    String cep,

    @NotNull
    Boolean principal
) {}
