package br.ufpb.dsc.mercado.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Armazena parâmetros globais da plataforma como pares chave/valor
 * (ex.: nome da loja, e-mail de contato), editáveis pelo SysAdmin
 * na tela de Configurações do Sistema.
 */
@Entity
@Table(name = "configuracao_sistema")
public class ConfiguracaoSistema {

    @Id
    @Column(name = "chave", length = 100, nullable = false)
    private String chave;

    @Column(name = "valor", length = 500)
    private String valor;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    @PrePersist
    @PreUpdate
    protected void aoSalvar() {
        this.atualizadoEm = Instant.now();
    }

    public ConfiguracaoSistema() {
    }

    public ConfiguracaoSistema(String chave, String valor) {
        this.chave = chave;
        this.valor = valor;
    }

    public String getChave() {
        return chave;
    }

    public void setChave(String chave) {
        this.chave = chave;
    }

    public String getValor() {
        return valor;
    }

    public void setValor(String valor) {
        this.valor = valor;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }

    public void setAtualizadoEm(Instant atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
    }
}
