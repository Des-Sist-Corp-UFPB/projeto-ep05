package br.ufpb.dsc.mercado.dto;

import br.ufpb.dsc.mercado.domain.Papel;

public record LoginResponse(
    String token,
    String email,
    String nome,
    Papel papel
) {}
