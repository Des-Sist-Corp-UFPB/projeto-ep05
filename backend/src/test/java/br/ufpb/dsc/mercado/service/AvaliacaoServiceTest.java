package br.ufpb.dsc.mercado.service;

import br.ufpb.dsc.mercado.domain.Avaliacao;
import br.ufpb.dsc.mercado.domain.Produto;
import br.ufpb.dsc.mercado.domain.Usuario;
import br.ufpb.dsc.mercado.dto.AvaliacaoDTO;
import br.ufpb.dsc.mercado.dto.AvaliacaoSalvarDTO;
import br.ufpb.dsc.mercado.exception.ApiException;
import br.ufpb.dsc.mercado.repository.AvaliacaoRepository;
import br.ufpb.dsc.mercado.repository.ProdutoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AvaliacaoService — Testes Unitários")
class AvaliacaoServiceTest {

    @Mock AvaliacaoRepository avaliacaoRepository;
    @Mock ProdutoRepository produtoRepository;
    @Mock UsuarioService usuarioService;

    private AvaliacaoService service;

    @BeforeEach
    void setUp() {
        service = new AvaliacaoService(avaliacaoRepository, produtoRepository, usuarioService);
    }

    private Produto produtoComId(Long id) {
        Produto p = new Produto("Brownie", "desc", java.math.BigDecimal.TEN, null, 10);
        p.setId(id);
        return p;
    }

    private Usuario usuarioComId(Long id, String nome) {
        Usuario u = new Usuario(nome, "cliente@teste.com", "senha123", br.ufpb.dsc.mercado.domain.Papel.CLIENTE);
        u.setId(id);
        return u;
    }

    @Test
    @DisplayName("listarPorProduto deve delegar ao repositório ordenado por data desc")
    void listarPorProduto_deveDelegarAoRepositorio() {
        Avaliacao avaliacao = new Avaliacao(produtoComId(1L), usuarioComId(2L, "Ana"), 5, "Ótimo!");
        when(avaliacaoRepository.findByProdutoIdOrderByCriadoEmDesc(1L)).thenReturn(List.of(avaliacao));

        List<Avaliacao> resultado = service.listarPorProduto(1L);

        assertThat(resultado).hasSize(1);
        verify(avaliacaoRepository).findByProdutoIdOrderByCriadoEmDesc(1L);
    }

    @Test
    @DisplayName("criarAvaliacao deve salvar quando cliente ainda não avaliou o produto")
    void criarAvaliacao_dadosValidos_deveSalvar() {
        AvaliacaoSalvarDTO dto = new AvaliacaoSalvarDTO(1L, 5, "Excelente!");
        Produto produto = produtoComId(1L);
        Usuario cliente = usuarioComId(2L, "Ana");

        when(avaliacaoRepository.existsByProdutoIdAndClienteId(1L, 2L)).thenReturn(false);
        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto));
        when(usuarioService.buscarPorId(2L)).thenReturn(cliente);
        when(avaliacaoRepository.save(any(Avaliacao.class))).thenAnswer(inv -> inv.getArgument(0));

        Avaliacao resultado = service.criarAvaliacao(2L, dto);

        assertThat(resultado.getNota()).isEqualTo(5);
        assertThat(resultado.getComentario()).isEqualTo("Excelente!");
        assertThat(resultado.getProduto()).isEqualTo(produto);
        assertThat(resultado.getCliente()).isEqualTo(cliente);
    }

    @Test
    @DisplayName("criarAvaliacao deve lançar exceção quando o cliente já avaliou o produto")
    void criarAvaliacao_jaAvaliado_deveLancarExcecao() {
        AvaliacaoSalvarDTO dto = new AvaliacaoSalvarDTO(1L, 5, "Excelente!");
        when(avaliacaoRepository.existsByProdutoIdAndClienteId(1L, 2L)).thenReturn(true);

        assertThatThrownBy(() -> service.criarAvaliacao(2L, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Você já avaliou este produto");

        verify(produtoRepository, never()).findById(any());
        verify(avaliacaoRepository, never()).save(any());
    }

    @Test
    @DisplayName("criarAvaliacao deve lançar exceção quando o produto não existe")
    void criarAvaliacao_produtoInexistente_deveLancarExcecao() {
        AvaliacaoSalvarDTO dto = new AvaliacaoSalvarDTO(99L, 5, null);
        when(avaliacaoRepository.existsByProdutoIdAndClienteId(99L, 2L)).thenReturn(false);
        when(produtoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.criarAvaliacao(2L, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Produto não encontrado com ID: 99");

        verify(avaliacaoRepository, never()).save(any());
    }

    @Test
    @DisplayName("criarAvaliacao deve propagar exceção quando o cliente não existe")
    void criarAvaliacao_clienteInexistente_devePropagarExcecao() {
        AvaliacaoSalvarDTO dto = new AvaliacaoSalvarDTO(1L, 5, null);
        when(avaliacaoRepository.existsByProdutoIdAndClienteId(1L, 2L)).thenReturn(false);
        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produtoComId(1L)));
        when(usuarioService.buscarPorId(2L)).thenThrow(ApiException.naoEncontrado("Usuário não encontrado com ID: 2"));

        assertThatThrownBy(() -> service.criarAvaliacao(2L, dto))
                .isInstanceOf(ApiException.class);

        verify(avaliacaoRepository, never()).save(any());
    }

    @Test
    @DisplayName("converterParaDTO deve mapear todos os campos corretamente")
    void converterParaDTO_deveMapearCampos() {
        Instant agora = Instant.now();
        Avaliacao avaliacao = new Avaliacao(produtoComId(1L), usuarioComId(2L, "Ana Souza"), 4, "Muito bom");
        avaliacao.setId(10L);
        avaliacao.setCriadoEm(agora);

        AvaliacaoDTO dto = service.converterParaDTO(avaliacao);

        assertThat(dto.id()).isEqualTo(10L);
        assertThat(dto.clienteNome()).isEqualTo("Ana Souza");
        assertThat(dto.nota()).isEqualTo(4);
        assertThat(dto.comentario()).isEqualTo("Muito bom");
        assertThat(dto.criadoEm()).isEqualTo(agora);
    }
}
