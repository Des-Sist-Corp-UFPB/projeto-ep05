package br.ufpb.dsc.mercado.dto;

import java.time.Instant;

public record AvaliacaoDTO(
    Long id,
    String clienteNome,
    Integer nota,
    String comentario,
    Instant criadoEm
) {}
