package br.ufpb.dsc.mercado.audit;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Registro de auditoria persistido no banco.
 *
 * Cada ação relevante de ADMIN ou CLIENTE gera uma entrada aqui,
 * visível na tela /sysadmin/logs com filtros de papel e data.
 */
@Entity
@Table(name = "log_auditoria", indexes = {
        @Index(name = "idx_log_papel", columnList = "papel_ator"),
        @Index(name = "idx_log_criado", columnList = "criado_em")
})
public class LogAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Papel do usuário que realizou a ação: SYSADMIN, ADMIN, CLIENTE, SYSTEM */
    @Column(name = "papel_ator", nullable = false, length = 20)
    private String papelAtor;

    /** E-mail (ou identificador) do ator. "SYSTEM" para eventos automáticos. */
    @Column(name = "ator", nullable = false, length = 150)
    private String ator;

    /** Categoria da ação: USER_MGMT, PRODUTO, PEDIDO, AUTH, SYSTEM */
    @Column(nullable = false, length = 50)
    private String categoria;

    /** Descrição legível da ação realizada. */
    @Column(nullable = false, length = 500)
    private String descricao;

    /** ID do recurso afetado (opcional). Ex: id do pedido, produto, usuário. */
    @Column(name = "recurso_id")
    private Long recursoId;

    /** Resultado: SUCCESS ou FAILURE */
    @Column(nullable = false, length = 20)
    private String resultado;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @PrePersist
    protected void prePersist() {
        this.criadoEm = Instant.now();
    }

    public LogAuditoria() {}

    private LogAuditoria(Builder b) {
        this.papelAtor = b.papelAtor;
        this.ator = b.ator;
        this.categoria = b.categoria;
        this.descricao = b.descricao;
        this.recursoId = b.recursoId;
        this.resultado = b.resultado;
    }

    // ── Builder ───────────────────────────────────────────────────────────────

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String papelAtor;
        private String ator;
        private String categoria;
        private String descricao;
        private Long recursoId;
        private String resultado = "SUCCESS";

        public Builder papelAtor(String v)  { this.papelAtor = v; return this; }
        public Builder ator(String v)        { this.ator = v; return this; }
        public Builder categoria(String v)   { this.categoria = v; return this; }
        public Builder descricao(String v)   { this.descricao = v; return this; }
        public Builder recursoId(Long v)     { this.recursoId = v; return this; }
        public Builder falha()               { this.resultado = "FAILURE"; return this; }
        public LogAuditoria build()          { return new LogAuditoria(this); }
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public Long getId()           { return id; }
    public String getPapelAtor()  { return papelAtor; }
    public String getAtor()       { return ator; }
    public String getCategoria()  { return categoria; }
    public String getDescricao()  { return descricao; }
    public Long getRecursoId()    { return recursoId; }
    public String getResultado()  { return resultado; }
    public Instant getCriadoEm() { return criadoEm; }
}
