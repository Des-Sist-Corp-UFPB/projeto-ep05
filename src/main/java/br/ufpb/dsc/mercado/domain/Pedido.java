package br.ufpb.dsc.mercado.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "pedido")
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Usuario cliente;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "endereco_entrega_id", nullable = false)
    private Endereco enderecoEntrega;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cartao_id")
    private Cartao cartao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cupom_id")
    private Cupom cupom;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusPedido status = StatusPedido.AGUARDANDO_PAGAMENTO;

    @NotNull
    @Column(name = "total_produtos", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalProdutos;

    @NotNull
    @Column(name = "total_desconto", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalDesconto = BigDecimal.ZERO;

    @NotNull
    @Column(name = "total_geral", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalGeral;

    @Column(name = "codigo_rastreamento", length = 100)
    private String codigoRastreamento;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PedidoItem> itens = new ArrayList<>();

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    @PrePersist
    protected void prePersist() {
        Instant agora = Instant.now();
        this.criadoEm = agora;
        this.atualizadoEm = agora;
    }

    @PreUpdate
    protected void preUpdate() {
        this.atualizadoEm = Instant.now();
    }

    public Pedido() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Usuario getCliente() {
        return cliente; // entidade JPA gerenciada - referência direta é intencional
    }

    public void setCliente(Usuario cliente) {
        this.cliente = cliente; // entidade JPA gerenciada - referência direta é intencional
    }

    public Endereco getEnderecoEntrega() {
        return enderecoEntrega; // entidade JPA gerenciada - referência direta é intencional
    }

    public void setEnderecoEntrega(Endereco enderecoEntrega) {
        this.enderecoEntrega = enderecoEntrega; // entidade JPA gerenciada - referência direta é intencional
    }

    public Cartao getCartao() {
        return cartao; // entidade JPA gerenciada - referência direta é intencional
    }

    public void setCartao(Cartao cartao) {
        this.cartao = cartao; // entidade JPA gerenciada - referência direta é intencional
    }

    public Cupom getCupom() {
        return cupom; // entidade JPA gerenciada - referência direta é intencional
    }

    public void setCupom(Cupom cupom) {
        this.cupom = cupom; // entidade JPA gerenciada - referência direta é intencional
    }

    public StatusPedido getStatus() {
        return status;
    }

    public void setStatus(StatusPedido status) {
        this.status = status;
    }

    public BigDecimal getTotalProdutos() {
        return totalProdutos;
    }

    public void setTotalProdutos(BigDecimal totalProdutos) {
        this.totalProdutos = totalProdutos;
    }

    public BigDecimal getTotalDesconto() {
        return totalDesconto;
    }

    public void setTotalDesconto(BigDecimal totalDesconto) {
        this.totalDesconto = totalDesconto;
    }

    public BigDecimal getTotalGeral() {
        return totalGeral;
    }

    public void setTotalGeral(BigDecimal totalGeral) {
        this.totalGeral = totalGeral;
    }

    public String getCodigoRastreamento() {
        return codigoRastreamento;
    }

    public void setCodigoRastreamento(String codigoRastreamento) {
        this.codigoRastreamento = codigoRastreamento;
    }

    public List<PedidoItem> getItens() {
        return Collections.unmodifiableList(itens);
    }

    public void setItens(List<PedidoItem> itens) {
        this.itens = itens == null ? new ArrayList<>() : new ArrayList<>(itens);
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(Instant criadoEm) {
        this.criadoEm = criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }

    public void setAtualizadoEm(Instant atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
    }
}
