package br.ufpb.dsc.mercado.service;

import br.ufpb.dsc.mercado.domain.Produto;
import br.ufpb.dsc.mercado.dto.ProdutoForm;
import br.ufpb.dsc.mercado.exception.ProdutoNaoEncontradoException;
import br.ufpb.dsc.mercado.repository.CategoriaRepository;
import br.ufpb.dsc.mercado.repository.ProdutoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para {@link ProdutoService}.
 *
 * <p>Usa Mockito para simular ProdutoRepository e CategoriaRepository,
 * sem acesso real ao banco de dados.
 *
 * <p>Os construtores usados correspondem à versão atual das classes:
 * <ul>
 *   <li>Produto(nome, descricao, preco, categoria, estoque)</li>
 *   <li>ProdutoForm(nome, descricao, preco, categoriaId, estoque, ativo, imagensUrls)</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProdutoService — Testes Unitários")
class ProdutoServiceTest {

    @Mock
    private ProdutoRepository produtoRepository;

    @Mock
    private CategoriaRepository categoriaRepository;

    @InjectMocks
    private ProdutoService produtoService;

    private Produto produtoExistente;
    private ProdutoForm formValido;

    @BeforeEach
    void setUp() {
        // Construtor: (nome, descricao, preco, categoria, estoque)
        // categoria = null → produto sem categoria
        produtoExistente = new Produto("Arroz Integral", "Arroz integral tipo 1",
                new BigDecimal("8.99"), null, 100);
        produtoExistente.setId(1L);

        // ProdutoForm: (nome, descricao, preco, categoriaId, estoque, ativo, imagensUrls)
        formValido = new ProdutoForm(
                "Feijão Preto",
                "Feijão preto premium",
                new BigDecimal("7.50"),
                null,   // categoriaId — sem categoria
                20,     // estoque
                true,   // ativo
                null    // imagensUrls
        );
    }

    // =========================================================================
    // TESTES: buscarPorId
    // =========================================================================

    @Test
    @DisplayName("buscarPorId: deve retornar produto quando ID existe")
    void buscarPorId_quandoIdExiste_deveRetornarProduto() {
        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produtoExistente));

        Produto resultado = produtoService.buscarPorId(1L);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getNome()).isEqualTo("Arroz Integral");

        verify(produtoRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("buscarPorId: deve lançar exceção quando ID não existe")
    void buscarPorId_quandoIdNaoExiste_deveLancarExcecao() {
        when(produtoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> produtoService.buscarPorId(99L))
                .isInstanceOf(ProdutoNaoEncontradoException.class)
                .hasMessageContaining("99");

        verify(produtoRepository, times(1)).findById(99L);
    }

    // =========================================================================
    // TESTES: criar
    // =========================================================================

    @Test
    @DisplayName("criar: deve salvar e retornar o novo produto")
    void criar_comFormValido_deveSalvarERetornarProduto() {
        Produto produtoSalvo = new Produto(
                formValido.nome(), formValido.descricao(),
                formValido.preco(), null, formValido.estoque());
        produtoSalvo.setId(2L);

        when(produtoRepository.save(any(Produto.class))).thenReturn(produtoSalvo);
        // categoriaId = null → não chama categoriaRepository

        Produto resultado = produtoService.criar(formValido);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getId()).isEqualTo(2L);
        assertThat(resultado.getNome()).isEqualTo("Feijão Preto");
        assertThat(resultado.getPreco()).isEqualByComparingTo("7.50");

        verify(produtoRepository, times(1)).save(any(Produto.class));
    }

    // =========================================================================
    // TESTES: atualizar
    // =========================================================================

    @Test
    @DisplayName("atualizar: deve modificar os dados do produto existente")
    void atualizar_quandoProdutoExiste_deveAtualizarDados() {
        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produtoExistente));
        when(produtoRepository.save(any(Produto.class))).thenReturn(produtoExistente);

        ProdutoForm formAtualizado = new ProdutoForm(
                "Arroz Branco", "Arroz branco tipo 1",
                new BigDecimal("5.99"), null, 30, true, null);

        Produto resultado = produtoService.atualizar(1L, formAtualizado);

        assertThat(resultado.getNome()).isEqualTo("Arroz Branco");
        assertThat(resultado.getPreco()).isEqualByComparingTo("5.99");

        verify(produtoRepository).findById(1L);
        verify(produtoRepository).save(any(Produto.class));
    }

    @Test
    @DisplayName("atualizar: deve lançar exceção quando produto não existe")
    void atualizar_quandoProdutoNaoExiste_deveLancarExcecao() {
        when(produtoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> produtoService.atualizar(99L, formValido))
                .isInstanceOf(ProdutoNaoEncontradoException.class);

        verify(produtoRepository, never()).save(any());
    }

    // =========================================================================
    // TESTES: excluir
    // =========================================================================

    @Test
    @DisplayName("excluir: deve deletar produto quando ID existe")
    void excluir_quandoProdutoExiste_deveDeletar() {
        when(produtoRepository.existsById(1L)).thenReturn(true);
        doNothing().when(produtoRepository).deleteById(1L);

        assertThatCode(() -> produtoService.excluir(1L))
                .doesNotThrowAnyException();

        verify(produtoRepository).existsById(1L);
        verify(produtoRepository).deleteById(1L);
    }

    @Test
    @DisplayName("excluir: deve lançar exceção quando produto não existe")
    void excluir_quandoProdutoNaoExiste_deveLancarExcecao() {
        when(produtoRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> produtoService.excluir(99L))
                .isInstanceOf(ProdutoNaoEncontradoException.class)
                .hasMessageContaining("99");

        verify(produtoRepository, never()).deleteById(any());
    }
}
