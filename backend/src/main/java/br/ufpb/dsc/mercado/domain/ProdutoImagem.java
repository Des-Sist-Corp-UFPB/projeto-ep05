package br.ufpb.dsc.mercado.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

@Entity
@Table(name = "produto_imagem")
public class ProdutoImagem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @NotBlank
    @Column(nullable = false, length = 500)
    private String url;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @PrePersist
    protected void prePersist() {
        this.criadoEm = Instant.now();
    }

    public ProdutoImagem() {
    }

    @SuppressWarnings("EI_EXPOSE_REP2")
    public ProdutoImagem(Produto produto, String url) {
        this.produto = produto;
        this.url = url;
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

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(Instant criadoEm) {
        this.criadoEm = criadoEm;
    }
}
