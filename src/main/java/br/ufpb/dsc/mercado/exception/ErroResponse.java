package br.ufpb.dsc.mercado.exception;

import java.time.Instant;

public record ErroResponse(

    int status,
    String erro,
    String caminho,
    Instant timestamp
) {
    public ErroResponse(int status, String erro, String caminho) {
        this(status, erro, caminho, Instant.now());

}
}
