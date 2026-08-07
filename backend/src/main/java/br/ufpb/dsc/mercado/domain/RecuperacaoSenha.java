package br.ufpb.dsc.mercado.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "recuperacao_senha")
public class RecuperacaoSenha {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, unique = true, length = 64)
    private String token;

    @Column(name = "expira_em", nullable = false)
    private Instant expiraEm;

    @Column(nullable = false)
    private boolean usado = false;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm = Instant.now();

    public RecuperacaoSenha() {}

    public RecuperacaoSenha(Usuario usuario, String token, Instant expiraEm) {
        this.usuario = usuario;
        this.token = token;
        this.expiraEm = expiraEm;
    }

    public boolean isValido() {
        return !usado && Instant.now().isBefore(expiraEm);
    }

    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public String getToken() { return token; }
    public Instant getExpiraEm() { return expiraEm; }
    public boolean isUsado() { return usado; }
    public void setUsado(boolean usado) { this.usado = usado; }
    public Instant getCriadoEm() { return criadoEm; }
}