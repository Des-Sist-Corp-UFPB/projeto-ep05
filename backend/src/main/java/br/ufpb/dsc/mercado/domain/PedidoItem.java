package br.ufpb.dsc.mercado.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Entity
@Table(name = "pedido_item")
public class PedidoItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @NotNull
    @Min(value = 1, message = "A quantidade deve ser de pelo menos 1")
    @Column(nullable = false)
    private Integer quantidade;

    @NotNull
    @Column(name = "preco_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoUnitario;

    public PedidoItem() {
    }

    @SuppressWarnings({"EI_EXPOSE_REP2"})
    public PedidoItem(Pedido pedido, Produto produto, Integer quantidade, BigDecimal precoUnitario) {
        this.pedido = pedido;
        this.produto = produto;
        this.quantidade = quantidade;
        this.precoUnitario = precoUnitario;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @SuppressWarnings("EI_EXPOSE_REP")
    public Pedido getPedido() {
        return pedido; // entidade JPA gerenciada - referência direta é intencional
    }

    @SuppressWarnings("EI_EXPOSE_REP2")
    public void setPedido(Pedido pedido) {
        this.pedido = pedido; // entidade JPA gerenciada - referência direta é intencional
    }

    @SuppressWarnings("EI_EXPOSE_REP")
    public Produto getProduto() {
        return produto; // entidade JPA gerenciada - referência direta é intencional
    }

    @SuppressWarnings("EI_EXPOSE_REP2")
    public void setProduto(Produto produto) {
        this.produto = produto; // entidade JPA gerenciada - referência direta é intencional
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(Integer quantidade) {
        this.quantidade = quantidade;
    }

    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }

    public void setPrecoUnitario(BigDecimal precoUnitario) {
        this.precoUnitario = precoUnitario;
    }
}
