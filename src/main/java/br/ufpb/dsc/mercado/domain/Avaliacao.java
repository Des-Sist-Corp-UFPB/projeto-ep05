package br.ufpb.dsc.mercado.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

@Entity
@Table(name = "avaliacao", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"produto_id", "cliente_id"})
})
public class Avaliacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Usuario cliente;

    @NotNull(message = "A nota é obrigatória")
    @Min(value = 1, message = "A nota mínima é 1")
    @Max(value = 5, message = "A nota máxima é 5")
    @Column(nullable = false)
    private Integer nota;

    @Column(columnDefinition = "TEXT")
    private String comentario;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @PrePersist
    protected void prePersist() {
        this.criadoEm = Instant.now();
    }

    public Avaliacao() {
    }

    @SuppressWarnings({"EI_EXPOSE_REP2"})
    public Avaliacao(Produto produto, Usuario cliente, Integer nota, String comentario) {
        this.produto = produto;
        this.cliente = cliente;
        this.nota = nota;
        this.comentario = comentario;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @SuppressWarnings("EI_EXPOSE_REP")
    public Produto getProduto() {
        return produto; // entidade JPA gerenciada - referência direta é intencional
    }

    @SuppressWarnings("EI_EXPOSE_REP2")
    public void setProduto(Produto produto) {
        this.produto = produto; // entidade JPA gerenciada - referência direta é intencional
    }

    @SuppressWarnings("EI_EXPOSE_REP")
    public Usuario getCliente() {
        return cliente; // entidade JPA gerenciada - referência direta é intencional
    }

    @SuppressWarnings("EI_EXPOSE_REP2")
    public void setCliente(Usuario cliente) {
        this.cliente = cliente; // entidade JPA gerenciada - referência direta é intencional
    }

    public Integer getNota() {
        return nota;
    }

    public void setNota(Integer nota) {
        this.nota = nota;
    }

    public String getComentario() {
        return comentario;
    }

    public void setComentario(String comentario) {
        this.comentario = comentario;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(Instant criadoEm) {
        this.criadoEm = criadoEm;
    }
}
