package br.ufpb.dsc.mercado.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

@Entity
@Table(name = "endereco")
public class Endereco {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Usuario cliente;

    @NotBlank(message = "O logradouro é obrigatório")
    @Size(max = 150)
    @Column(nullable = false, length = 150)
    private String logradouro;

    @NotBlank(message = "O número é obrigatório")
    @Size(max = 20)
    @Column(nullable = false, length = 20)
    private String numero;

    @Size(max = 100)
    @Column(length = 100)
    private String complemento;

    @NotBlank(message = "O bairro é obrigatório")
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String bairro;

    @NotBlank(message = "A cidade é obrigatória")
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String cidade;

    @NotBlank(message = "O estado é obrigatório")
    @Size(min = 2, max = 2)
    @Column(nullable = false, length = 2)
    private String estado;

    @NotBlank(message = "O CEP é obrigatório")
    @Size(max = 10)
    @Column(nullable = false, length = 10)
    private String cep;

    @NotNull
    @Column(nullable = false)
    private Boolean principal = false;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @PrePersist
    protected void prePersist() {
        this.criadoEm = Instant.now();
    }

    public Endereco() {
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

    public String getLogradouro() {
        return logradouro;
    }

    public void setLogradouro(String logradouro) {
        this.logradouro = logradouro;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public String getComplemento() {
        return complemento;
    }

    public void setComplemento(String complemento) {
        this.complemento = complemento;
    }

    public String getBairro() {
        return bairro;
    }

    public void setBairro(String bairro) {
        this.bairro = bairro;
    }

    public String getCidade() {
        return cidade;
    }

    public void setCidade(String cidade) {
        this.cidade = cidade;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getCep() {
        return cep;
    }

    public void setCep(String cep) {
        this.cep = cep;
    }

    public Boolean getPrincipal() {
        return principal;
    }

    public void setPrincipal(Boolean principal) {
        this.principal = principal;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(Instant criadoEm) {
        this.criadoEm = criadoEm;
    }
}
