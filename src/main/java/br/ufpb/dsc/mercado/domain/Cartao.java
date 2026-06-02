package br.ufpb.dsc.mercado.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

@Entity
@Table(name = "cartao")
public class Cartao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Usuario cliente;

    @NotBlank(message = "O nome do titular é obrigatório")
    @Size(max = 150)
    @Column(name = "nome_titular", nullable = false, length = 150)
    private String nomeTitular;

    @NotBlank(message = "A bandeira é obrigatória")
    @Size(max = 50)
    @Column(nullable = false, length = 50)
    private String bandeira;

    @NotBlank(message = "Os quatro últimos dígitos são obrigatórios")
    @Pattern(regexp = "^\\d{4}$", message = "Deve conter exatamente 4 dígitos")
    @Column(name = "quatro_ultimos_digitos", nullable = false, length = 4)
    private String quatroUltimosDigitos;

    @NotBlank
    @Column(name = "token_pagamento", nullable = false)
    private String tokenPagamento;

    @NotBlank(message = "A data de expiração é obrigatória")
    @Pattern(regexp = "^(0[1-9]|1[0-2])\\/\\d{4}$", message = "Deve estar no formato MM/AAAA")
    @Column(name = "data_expiracao", nullable = false, length = 7)
    private String dataExpiracao;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @PrePersist
    protected void prePersist() {
        this.criadoEm = Instant.now();
    }

    public Cartao() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Usuario getCliente() {
        return cliente;
    }

    public void setCliente(Usuario cliente) {
        this.cliente = cliente;
    }

    public String getNomeTitular() {
        return nomeTitular;
    }

    public void setNomeTitular(String nomeTitular) {
        this.nomeTitular = nomeTitular;
    }

    public String getBandeira() {
        return bandeira;
    }

    public void setBandeira(String bandeira) {
        this.bandeira = bandeira;
    }

    public String getQuatroUltimosDigitos() {
        return quatroUltimosDigitos;
    }

    public void setQuatroUltimosDigitos(String quatroUltimosDigitos) {
        this.quatroUltimosDigitos = quatroUltimosDigitos;
    }

    public String getTokenPagamento() {
        return tokenPagamento;
    }

    public void setTokenPagamento(String tokenPagamento) {
        this.tokenPagamento = tokenPagamento;
    }

    public String getDataExpiracao() {
        return dataExpiracao;
    }

    public void setDataExpiracao(String dataExpiracao) {
        this.dataExpiracao = dataExpiracao;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(Instant criadoEm) {
        this.criadoEm = criadoEm;
    }
}
