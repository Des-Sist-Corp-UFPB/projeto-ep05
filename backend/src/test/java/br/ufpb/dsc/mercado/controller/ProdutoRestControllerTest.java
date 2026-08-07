package br.ufpb.dsc.mercado.controller;

import br.ufpb.dsc.mercado.domain.Avaliacao;
import br.ufpb.dsc.mercado.domain.Categoria;
import br.ufpb.dsc.mercado.domain.Papel;
import br.ufpb.dsc.mercado.domain.Produto;
import br.ufpb.dsc.mercado.domain.Usuario;
import br.ufpb.dsc.mercado.dto.AvaliacaoDTO;
import br.ufpb.dsc.mercado.dto.AvaliacaoSalvarDTO;
import br.ufpb.dsc.mercado.dto.CategoriaDTO;
import br.ufpb.dsc.mercado.dto.ProdutoDTO;
import br.ufpb.dsc.mercado.service.AvaliacaoService;
import br.ufpb.dsc.mercado.service.CategoriaService;
import br.ufpb.dsc.mercado.service.ProdutoService;
import br.ufpb.dsc.mercado.service.UsuarioService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProdutoRestController — Testes Unitários")
class ProdutoRestControllerTest {

    @Mock ProdutoService produtoService;
    @Mock CategoriaService categoriaService;
    @Mock AvaliacaoService avaliacaoService;
    @Mock UsuarioService usuarioService;

    private ProdutoRestController controller;

    @BeforeEach
    void setUp() {
        controller = new ProdutoRestController(produtoService, categoriaService, avaliacaoService, usuarioService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void autenticar(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, null, List.of()));
    }

    private Produto produtoAtivo(Long id, String nome) {
        Produto p = new Produto(nome, "desc", BigDecimal.TEN, null, 10);
        p.setId(id);
        p.setAtivo(true);
        return p;
    }

    private ProdutoDTO produtoDTO(Long id, String nome) {
        return new ProdutoDTO(id, nome, "desc", BigDecimal.TEN, null, null, 10, true, List.of(), 0.0);
    }

    // ── listarAtivos ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("listarAtivos sem filtros deve chamar buscarAtivos")
    void listarAtivos_semFiltros_deveChamarBuscarAtivos() {
        Pageable pageable = Pageable.ofSize(12);
        Page<Produto> page = new PageImpl<>(List.of(produtoAtivo(1L, "Brownie")));
        when(produtoService.buscarAtivos(null, pageable)).thenReturn(page);
        when(produtoService.converterParaDTO(any())).thenReturn(produtoDTO(1L, "Brownie"));

        ResponseEntity<Page<ProdutoDTO>> resposta = controller.listarAtivos(null, null, pageable);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody().getContent()).hasSize(1);
        verify(produtoService).buscarAtivos(null, pageable);
        verify(produtoService, never()).buscarPorCategoriaEAtivos(any(), any());
    }

    @Test
    @DisplayName("listarAtivos com categoriaId deve chamar buscarPorCategoriaEAtivos")
    void listarAtivos_comCategoriaId_deveChamarBuscarPorCategoria() {
        Pageable pageable = Pageable.ofSize(12);
        Page<Produto> page = new PageImpl<>(List.of(produtoAtivo(1L, "Torta")));
        when(produtoService.buscarPorCategoriaEAtivos(5L, pageable)).thenReturn(page);
        when(produtoService.converterParaDTO(any())).thenReturn(produtoDTO(1L, "Torta"));

        ResponseEntity<Page<ProdutoDTO>> resposta = controller.listarAtivos(null, 5L, pageable);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(produtoService).buscarPorCategoriaEAtivos(5L, pageable);
        verify(produtoService, never()).buscarAtivos(any(), any());
    }

    @Test
    @DisplayName("listarAtivos com busca textual deve repassar o termo para buscarAtivos")
    void listarAtivos_comBusca_deveRepassarTermo() {
        Pageable pageable = Pageable.ofSize(12);
        when(produtoService.buscarAtivos("brownie", pageable)).thenReturn(Page.empty());

        controller.listarAtivos("brownie", null, pageable);

        verify(produtoService).buscarAtivos("brownie", pageable);
    }

    // ── buscarPorId ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("buscarPorId deve retornar 200 com o DTO quando o produto existe e está ativo")
    void buscarPorId_ativoExistente_deveRetornar200() {
        Produto p = produtoAtivo(1L, "Brownie");
        when(produtoService.buscarPorId(1L)).thenReturn(p);
        when(produtoService.converterParaDTO(p)).thenReturn(produtoDTO(1L, "Brownie"));

        ResponseEntity<ProdutoDTO> resposta = controller.buscarPorId(1L);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody().nome()).isEqualTo("Brownie");
    }

    @Test
    @DisplayName("buscarPorId deve retornar 404 quando o produto está inativo")
    void buscarPorId_inativo_deveRetornar404() {
        Produto p = produtoAtivo(1L, "Brownie");
        p.setAtivo(false);
        when(produtoService.buscarPorId(1L)).thenReturn(p);

        ResponseEntity<ProdutoDTO> resposta = controller.buscarPorId(1L);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(produtoService, never()).converterParaDTO(any());
    }

    // ── listarCategorias ─────────────────────────────────────────────────────

    @Test
    @DisplayName("listarCategorias deve mapear todas as categorias para DTO")
    void listarCategorias_deveMappearTodasParaDTO() {
        Categoria c = new Categoria("Bolos", "desc");
        c.setId(1L);
        when(categoriaService.listarTodas()).thenReturn(List.of(c));

        ResponseEntity<List<CategoriaDTO>> resposta = controller.listarCategorias();

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).hasSize(1);
        assertThat(resposta.getBody().get(0).nome()).isEqualTo("Bolos");
    }

    // ── maisVendidos ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("maisVendidos deve retornar a lista com a quantidade solicitada")
    void maisVendidos_deveRetornarQuantidadeSolicitada() {
        Produto p = produtoAtivo(1L, "Cookie");
        Page<Produto> page = new PageImpl<>(List.of(p));
        when(produtoService.buscarMaisVendidos(any(Pageable.class))).thenReturn(page);
        when(produtoService.converterParaDTO(p)).thenReturn(produtoDTO(1L, "Cookie"));

        ResponseEntity<List<ProdutoDTO>> resposta = controller.maisVendidos(8);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).hasSize(1);
    }

    // ── listarAvaliacoes ─────────────────────────────────────────────────────

    @Test
    @DisplayName("listarAvaliacoes deve retornar todas as avaliações do produto convertidas para DTO")
    void listarAvaliacoes_deveRetornarDTOs() {
        Produto produto = produtoAtivo(1L, "Brownie");
        Usuario cliente = new Usuario("Ana", "ana@teste.com", "hash", Papel.CLIENTE);
        cliente.setId(2L);
        Avaliacao av = new Avaliacao(produto, cliente, 5, "Delicioso!");
        av.setId(10L);
        av.setCriadoEm(Instant.now());

        AvaliacaoDTO dto = new AvaliacaoDTO(10L, "Ana", 5, "Delicioso!", Instant.now());
        when(avaliacaoService.listarPorProduto(1L)).thenReturn(List.of(av));
        when(avaliacaoService.converterParaDTO(av)).thenReturn(dto);

        ResponseEntity<List<AvaliacaoDTO>> resposta = controller.listarAvaliacoes(1L);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).hasSize(1);
        assertThat(resposta.getBody().get(0).nota()).isEqualTo(5);
    }

    // ── avaliarProduto ────────────────────────────────────────────────────────

    @Test
    @DisplayName("avaliarProduto sem autenticação deve retornar 401")
    void avaliarProduto_semAutenticacao_deveRetornar401() {
        // SecurityContext vazio = authentication é null → buscarPorEmail lança exceção
        autenticar("anonimo@anon.com");
        when(usuarioService.buscarPorEmail("anonimo@anon.com"))
                .thenThrow(new RuntimeException("não autenticado"));

        AvaliacaoSalvarDTO dto = new AvaliacaoSalvarDTO(1L, 5, "ok");
        ResponseEntity<?> resposta = controller.avaliarProduto(1L, dto);

        assertThat(resposta.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    @DisplayName("avaliarProduto com id do produto diferente no body deve retornar 400")
    void avaliarProduto_idDivergente_deveRetornar400() {
        autenticar("ana@teste.com");
        Usuario u = new Usuario("Ana", "ana@teste.com", "hash", Papel.CLIENTE);
        u.setId(2L);
        when(usuarioService.buscarPorEmail("ana@teste.com")).thenReturn(u);

        // Path id = 1, body produtoId = 99
        AvaliacaoSalvarDTO dto = new AvaliacaoSalvarDTO(99L, 5, "ok");
        ResponseEntity<?> resposta = controller.avaliarProduto(1L, dto);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody().toString()).contains("não coincide");
    }

    @Test
    @DisplayName("avaliarProduto com dados válidos deve salvar e retornar o DTO da avaliação")
    void avaliarProduto_dadosValidos_deveSalvarERetornarDTO() {
        autenticar("ana@teste.com");
        Usuario u = new Usuario("Ana", "ana@teste.com", "hash", Papel.CLIENTE);
        u.setId(2L);
        when(usuarioService.buscarPorEmail("ana@teste.com")).thenReturn(u);

        Produto produto = produtoAtivo(1L, "Brownie");
        Avaliacao av = new Avaliacao(produto, u, 4, "Bom");
        av.setId(10L);
        av.setCriadoEm(Instant.now());
        AvaliacaoDTO dto = new AvaliacaoDTO(10L, "Ana", 4, "Bom", av.getCriadoEm());
        AvaliacaoSalvarDTO salvarDTO = new AvaliacaoSalvarDTO(1L, 4, "Bom");

        when(avaliacaoService.criarAvaliacao(2L, salvarDTO)).thenReturn(av);
        when(avaliacaoService.converterParaDTO(av)).thenReturn(dto);

        ResponseEntity<?> resposta = controller.avaliarProduto(1L, salvarDTO);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((AvaliacaoDTO) resposta.getBody()).nota()).isEqualTo(4);
    }

    @Test
    @DisplayName("avaliarProduto com produto já avaliado deve retornar 400 com a mensagem do service")
    void avaliarProduto_jaAvaliado_deveRetornar400() {
        autenticar("ana@teste.com");
        Usuario u = new Usuario("Ana", "ana@teste.com", "hash", Papel.CLIENTE);
        u.setId(2L);
        when(usuarioService.buscarPorEmail("ana@teste.com")).thenReturn(u);

        AvaliacaoSalvarDTO salvarDTO = new AvaliacaoSalvarDTO(1L, 5, "Ótimo");
        when(avaliacaoService.criarAvaliacao(eq(2L), any()))
                .thenThrow(new IllegalArgumentException("Você já avaliou este produto"));

        ResponseEntity<?> resposta = controller.avaliarProduto(1L, salvarDTO);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).isEqualTo("Você já avaliou este produto");
    }
}
