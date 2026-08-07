package br.ufpb.dsc.mercado.service;

import br.ufpb.dsc.mercado.domain.Avaliacao;
import br.ufpb.dsc.mercado.domain.Categoria;
import br.ufpb.dsc.mercado.domain.Produto;
import br.ufpb.dsc.mercado.domain.ProdutoImagem;
import br.ufpb.dsc.mercado.dto.ProdutoDTO;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
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

    // =========================================================================
    // TESTES: listar
    // =========================================================================

    @Test
    @DisplayName("listar: deve retornar página de produtos do repositório")
    void listar_deveRetornarPaginaDeProdutos() {
        Pageable pageable = Pageable.unpaged();
        Page<Produto> pagina = new PageImpl<>(List.of(produtoExistente));
        when(produtoRepository.findAll(pageable)).thenReturn(pagina);

        Page<Produto> resultado = produtoService.listar(pageable);

        assertThat(resultado.getContent()).hasSize(1);
        verify(produtoRepository).findAll(pageable);
    }

    // =========================================================================
    // TESTES: buscar
    // =========================================================================

    @Test
    @DisplayName("buscar: sem texto de busca deve listar todos os produtos")
    void buscar_semTexto_deveListarTodos() {
        Pageable pageable = Pageable.unpaged();
        Page<Produto> pagina = new PageImpl<>(List.of(produtoExistente));
        when(produtoRepository.findAll(pageable)).thenReturn(pagina);

        Page<Produto> resultado = produtoService.buscar("   ", pageable);

        assertThat(resultado.getContent()).hasSize(1);
        verify(produtoRepository).findAll(pageable);
        verify(produtoRepository, never()).findByNomeContainingIgnoreCase(any(), any());
    }

    @Test
    @DisplayName("buscar: com texto deve filtrar pelo nome")
    void buscar_comTexto_deveFiltrarPorNome() {
        Pageable pageable = Pageable.unpaged();
        Page<Produto> pagina = new PageImpl<>(List.of(produtoExistente));
        when(produtoRepository.findByNomeContainingIgnoreCase("arroz", pageable)).thenReturn(pagina);

        Page<Produto> resultado = produtoService.buscar("  arroz  ", pageable);

        assertThat(resultado.getContent()).hasSize(1);
        verify(produtoRepository).findByNomeContainingIgnoreCase("arroz", pageable);
    }

    // =========================================================================
    // TESTES: buscarAtivos
    // =========================================================================

    @Test
    @DisplayName("buscarAtivos: sem texto deve listar apenas produtos ativos")
    void buscarAtivos_semTexto_deveListarAtivos() {
        Pageable pageable = Pageable.unpaged();
        Page<Produto> pagina = new PageImpl<>(List.of(produtoExistente));
        when(produtoRepository.findByAtivoTrue(pageable)).thenReturn(pagina);

        Page<Produto> resultado = produtoService.buscarAtivos(null, pageable);

        assertThat(resultado.getContent()).hasSize(1);
        verify(produtoRepository).findByAtivoTrue(pageable);
    }

    @Test
    @DisplayName("buscarAtivos: com texto deve filtrar ativos pelo nome")
    void buscarAtivos_comTexto_deveFiltrarAtivosPorNome() {
        Pageable pageable = Pageable.unpaged();
        Page<Produto> pagina = new PageImpl<>(List.of(produtoExistente));
        when(produtoRepository.findByAtivoTrueAndNomeContainingIgnoreCase("feijão", pageable)).thenReturn(pagina);

        Page<Produto> resultado = produtoService.buscarAtivos("feijão", pageable);

        assertThat(resultado.getContent()).hasSize(1);
        verify(produtoRepository).findByAtivoTrueAndNomeContainingIgnoreCase("feijão", pageable);
    }

    // =========================================================================
    // TESTES: buscarPorCategoriaEAtivos / buscarPorCategoria / buscarMaisVendidos
    // =========================================================================

    @Test
    @DisplayName("buscarPorCategoriaEAtivos: deve delegar ao repositório")
    void buscarPorCategoriaEAtivos_deveDelegarAoRepositorio() {
        Pageable pageable = Pageable.unpaged();
        Page<Produto> pagina = new PageImpl<>(List.of(produtoExistente));
        when(produtoRepository.findByCategoriaIdAndAtivoTrue(5L, pageable)).thenReturn(pagina);

        Page<Produto> resultado = produtoService.buscarPorCategoriaEAtivos(5L, pageable);

        assertThat(resultado.getContent()).hasSize(1);
        verify(produtoRepository).findByCategoriaIdAndAtivoTrue(5L, pageable);
    }

    @Test
    @DisplayName("buscarPorCategoria: deve delegar ao repositório")
    void buscarPorCategoria_deveDelegarAoRepositorio() {
        Pageable pageable = Pageable.unpaged();
        Page<Produto> pagina = new PageImpl<>(List.of(produtoExistente));
        when(produtoRepository.findByCategoriaId(5L, pageable)).thenReturn(pagina);

        Page<Produto> resultado = produtoService.buscarPorCategoria(5L, pageable);

        assertThat(resultado.getContent()).hasSize(1);
        verify(produtoRepository).findByCategoriaId(5L, pageable);
    }

    @Test
    @DisplayName("buscarMaisVendidos: deve delegar ao repositório")
    void buscarMaisVendidos_deveDelegarAoRepositorio() {
        Pageable pageable = Pageable.unpaged();
        Page<Produto> pagina = new PageImpl<>(List.of(produtoExistente));
        when(produtoRepository.findMaisVendidos(pageable)).thenReturn(pagina);

        Page<Produto> resultado = produtoService.buscarMaisVendidos(pageable);

        assertThat(resultado.getContent()).hasSize(1);
        verify(produtoRepository).findMaisVendidos(pageable);
    }

    // =========================================================================
    // TESTES: criar — categoria válida / inválida / imagens
    // =========================================================================

    @Test
    @DisplayName("criar: com categoria válida deve associar a categoria ao produto")
    void criar_comCategoriaValida_deveAssociarCategoria() {
        Categoria categoria = new Categoria("Grãos", "Categoria de grãos");
        categoria.setId(10L);

        ProdutoForm form = new ProdutoForm(
                "Lentilha", "Lentilha tipo 1", new BigDecimal("9.90"),
                10L, 15, true, "http://img1.com/a.png, http://img2.com/b.png");

        when(categoriaRepository.findById(10L)).thenReturn(Optional.of(categoria));
        when(produtoRepository.save(any(Produto.class))).thenAnswer(inv -> inv.getArgument(0));

        Produto resultado = produtoService.criar(form);

        assertThat(resultado.getCategoria()).isEqualTo(categoria);
        assertThat(resultado.getImagens()).hasSize(2);
        verify(categoriaRepository).findById(10L);
        verify(produtoRepository).save(any(Produto.class));
    }

    @Test
    @DisplayName("criar: com categoria inexistente deve lançar exceção")
    void criar_comCategoriaInexistente_deveLancarExcecao() {
        ProdutoForm form = new ProdutoForm(
                "Lentilha", "Lentilha tipo 1", new BigDecimal("9.90"),
                99L, 15, true, null);

        when(categoriaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> produtoService.criar(form))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("99");

        verify(produtoRepository, never()).save(any());
    }

    @Test
    @DisplayName("criar: sem campo ativo definido deve manter valor padrão do produto")
    void criar_semAtivoDefinido_naoDeveAlterarAtivo() {
        ProdutoForm form = new ProdutoForm(
                "Milho", "Milho verde", new BigDecimal("3.50"),
                null, 40, null, null);

        when(produtoRepository.save(any(Produto.class))).thenAnswer(inv -> inv.getArgument(0));

        Produto resultado = produtoService.criar(form);

        assertThat(resultado.getAtivo()).isTrue();
        assertThat(resultado.getImagens()).isEmpty();
    }

    // =========================================================================
    // TESTES: atualizar — categoria válida / inválida / imagens
    // =========================================================================

    @Test
    @DisplayName("atualizar: com categoria válida deve associar a nova categoria")
    void atualizar_comCategoriaValida_deveAssociarNovaCategoria() {
        Categoria categoria = new Categoria("Grãos", "Categoria de grãos");
        categoria.setId(10L);

        ProdutoForm form = new ProdutoForm(
                "Arroz Branco", "Arroz branco tipo 1", new BigDecimal("5.99"),
                10L, 30, false, "http://img.com/x.png");

        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produtoExistente));
        when(categoriaRepository.findById(10L)).thenReturn(Optional.of(categoria));
        when(produtoRepository.save(any(Produto.class))).thenReturn(produtoExistente);

        Produto resultado = produtoService.atualizar(1L, form);

        assertThat(resultado.getCategoria()).isEqualTo(categoria);
        assertThat(resultado.getAtivo()).isFalse();
        assertThat(resultado.getImagens()).hasSize(1);
        verify(categoriaRepository).findById(10L);
    }

    @Test
    @DisplayName("atualizar: com categoria inexistente deve lançar exceção")
    void atualizar_comCategoriaInexistente_deveLancarExcecao() {
        ProdutoForm form = new ProdutoForm(
                "Arroz Branco", "Arroz branco tipo 1", new BigDecimal("5.99"),
                99L, 30, true, null);

        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produtoExistente));
        when(categoriaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> produtoService.atualizar(1L, form))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("99");

        verify(produtoRepository, never()).save(any());
    }

    // =========================================================================
    // TESTES: alternarAtivo
    // =========================================================================

    @Test
    @DisplayName("alternarAtivo: deve inverter o status ativo do produto")
    void alternarAtivo_deveInverterStatusAtivo() {
        produtoExistente.setAtivo(true);
        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produtoExistente));
        when(produtoRepository.save(any(Produto.class))).thenReturn(produtoExistente);

        produtoService.alternarAtivo(1L);

        assertThat(produtoExistente.getAtivo()).isFalse();
        verify(produtoRepository).save(produtoExistente);
    }

    @Test
    @DisplayName("alternarAtivo: deve lançar exceção quando produto não existe")
    void alternarAtivo_quandoProdutoNaoExiste_deveLancarExcecao() {
        when(produtoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> produtoService.alternarAtivo(99L))
                .isInstanceOf(ProdutoNaoEncontradoException.class);

        verify(produtoRepository, never()).save(any());
    }

    // =========================================================================
    // TESTES: converterParaDTO
    // =========================================================================

    @Test
    @DisplayName("converterParaDTO: deve converter produto sem avaliações com nota média 0.0")
    void converterParaDTO_semAvaliacoes_deveRetornarNotaMediaZero() {
        ProdutoDTO dto = produtoService.converterParaDTO(produtoExistente);

        assertThat(dto.id()).isEqualTo(produtoExistente.getId());
        assertThat(dto.nome()).isEqualTo("Arroz Integral");
        assertThat(dto.categoriaId()).isNull();
        assertThat(dto.categoriaNome()).isNull();
        assertThat(dto.notaMedia()).isEqualTo(0.0);
        assertThat(dto.imagensUrls()).isEmpty();
    }

    @Test
    @DisplayName("converterParaDTO: deve calcular nota média e incluir dados da categoria e imagens")
    void converterParaDTO_comAvaliacoesECategoria_deveCalcularCorretamente() {
        Categoria categoria = new Categoria("Grãos", "Categoria de grãos");
        categoria.setId(7L);

        Produto produto = new Produto("Quinoa", "Quinoa orgânica", new BigDecimal("15.00"), categoria, 50);
        produto.setId(3L);
        produto.addImagem(new ProdutoImagem(produto, "http://img.com/quinoa.png"));

        Avaliacao avaliacao1 = new Avaliacao(produto, null, 4, "Muito bom");
        Avaliacao avaliacao2 = new Avaliacao(produto, null, 5, "Excelente");
        produto.setAvaliacoes(List.of(avaliacao1, avaliacao2));

        ProdutoDTO dto = produtoService.converterParaDTO(produto);

        assertThat(dto.categoriaId()).isEqualTo(7L);
        assertThat(dto.categoriaNome()).isEqualTo("Grãos");
        assertThat(dto.notaMedia()).isEqualTo(4.5);
        assertThat(dto.imagensUrls()).containsExactly("http://img.com/quinoa.png");
    }
}
