package br.ufpb.dsc.mercado.dto;

public record CartaoDTO(
    Long id,
    String nomeTitular,
    String bandeira,
    String quatroUltimosDigitos,
    String dataExpiracao
) {}
