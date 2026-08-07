package br.ufpb.dsc.mercado.mcp;

import br.ufpb.dsc.mercado.audit.AuditoriaService;
import br.ufpb.dsc.mercado.domain.Endereco;
import br.ufpb.dsc.mercado.domain.Papel;
import br.ufpb.dsc.mercado.domain.Pedido;
import br.ufpb.dsc.mercado.domain.Produto;
import br.ufpb.dsc.mercado.domain.Usuario;
import br.ufpb.dsc.mercado.dto.CartItemRequest;
import br.ufpb.dsc.mercado.dto.CheckoutRequest;
import br.ufpb.dsc.mercado.dto.PedidoDTO;
import br.ufpb.dsc.mercado.dto.ProdutoDTO;
import br.ufpb.dsc.mercado.service.PedidoService;
import br.ufpb.dsc.mercado.service.ProdutoService;
import br.ufpb.dsc.mercado.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Testes unitários para {@link PedidosTools} — a camada fina de tools do
 * servidor MCP (pedidos-mcp). Cada teste confere que a tool apenas delega
 * para os services já existentes, aplicando as checagens de propriedade e
 * auditoria descritas na documentação (MCP-IDEIA.md / README.md).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PedidosTools (MCP) — Testes Unitários")
class PedidosToolsTest {

    @Mock private ProdutoService produtoService;
    @Mock private PedidoService pedidoService;
    @Mock private UsuarioService usuarioService;
    @Mock private AuditoriaService auditoriaService;

    private PedidosTools tools;

    @BeforeEach
    void setUp() {
        tools = new PedidosTools(produtoService, pedidoService, usuarioService, auditoriaService);
    }

    private Usuario clienteComId(Long id, String email) {
        Usuario u = new Usuario("Ana", email, "hash", Papel.CLIENTE);
        u.setId(id);
        return u;
    }

    private Pedido pedidoDoCliente(Long id, Usuario cliente) {
        Pedido pedido = new Pedido();
        pedido.setId(id);
        pedido.setCliente(cliente);
        Endereco endereco = new Endereco();
        endereco.setId(1L);
        endereco.setCliente(cliente);
        endereco.setLogradouro("Rua A");
        endereco.setNumero("10");
        endereco.setBairro("Centro");
        endereco.setCidade("João Pessoa");
        endereco.setEstado("PB");
        endereco.setCep("58000000");
        endereco.setPrincipal(true);
        pedido.setEnderecoEntrega(endereco);
        pedido.setItens(List.of());
        pedido.setTotalProdutos(BigDecimal.TEN);
        pedido.setTotalDesconto(BigDecimal.ZERO);
        pedido.setTotalGeral(BigDecimal.TEN);
        return pedido;
    }

    // ── catalogo ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("catalogo deve delegar para ProdutoService.buscarAtivos e converter para DTO")
    void catalogo_delegaParaProdutoService() {
        Produto produto = new Produto("Bolo de Chocolate", "desc", BigDecimal.valueOf(50), null, 10);
        produto.setId(1L);
        Page<Produto> page = new PageImpl<>(List.of(produto));
        ProdutoDTO dto = new ProdutoDTO(1L, "Bolo de Chocolate", "desc", BigDecimal.valueOf(50),
                null, "Bolos", 10, true, List.of(), null);

        when(produtoService.buscarAtivos(eq("bolo"), any(Pageable.class))).thenReturn(page);
        when(produtoService.converterParaDTO(produto)).thenReturn(dto);

        List<ProdutoDTO> resultado = tools.catalogo("bolo");

        assertThat(resultado).containsExactly(dto);
        verify(produtoService).buscarAtivos(eq("bolo"), eq(PageRequest.of(0, 20)));
    }

    // ── rastrearPedido ───────────────────────────────────────────────────────

    @Test
    @DisplayName("rastrearPedido deve retornar o pedido quando o e-mail informado é do dono")
    void rastrearPedido_donoCorreto_deveRetornar() {
        Usuario cliente = clienteComId(1L, "ana@teste.com");
        Pedido pedido = pedidoDoCliente(10L, cliente);
        PedidoDTO dto = new PedidoDTO(10L, 1L, "Ana", "ana@teste.com", null, null, null,
                null, BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN, null, null, List.of(), null, null);

        when(pedidoService.buscarPorId(10L)).thenReturn(pedido);
        when(pedidoService.converterParaDTO(pedido)).thenReturn(dto);

        PedidoDTO resultado = tools.rastrearPedido("ana@teste.com", 10L);

        assertThat(resultado).isEqualTo(dto);
    }

    @Test
    @DisplayName("rastrearPedido deve recusar quando o e-mail informado não é o dono do pedido")
    void rastrearPedido_donoErrado_deveRecusar() {
        Usuario cliente = clienteComId(1L, "ana@teste.com");
        Pedido pedido = pedidoDoCliente(10L, cliente);

        when(pedidoService.buscarPorId(10L)).thenReturn(pedido);

        assertThatThrownBy(() -> tools.rastrearPedido("outra@teste.com", 10L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("não pertence");
    }

    // ── montarPedido ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("montarPedido deve buscar o cliente pelo e-mail, criar o pedido e registrar auditoria")
    void montarPedido_deveCriarERegistrarAuditoria() {
        Usuario cliente = clienteComId(1L, "ana@teste.com");
        Pedido pedidoCriado = pedidoDoCliente(20L, cliente);
        PedidoDTO dto = new PedidoDTO(20L, 1L, "Ana", "ana@teste.com", null, null, null,
                null, BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN, null, null, List.of(), null, null);
        List<CartItemRequest> itens = List.of(new CartItemRequest(1L, 2));

        when(usuarioService.buscarPorEmail("ana@teste.com")).thenReturn(cliente);
        when(pedidoService.criarPedido(eq(1L), any(CheckoutRequest.class))).thenReturn(pedidoCriado);
        when(pedidoService.converterParaDTO(pedidoCriado)).thenReturn(dto);

        PedidoDTO resultado = tools.montarPedido("ana@teste.com", itens, 5L, 7L, "PROMO10");

        assertThat(resultado).isEqualTo(dto);

        verify(pedidoService).criarPedido(eq(1L), eq(new CheckoutRequest(5L, 7L, "PROMO10", itens)));
        verify(auditoriaService).registrarCliente(
                eq("ana@teste.com"), eq("PEDIDO"), any(String.class), eq(20L));
    }

    @Test
    @DisplayName("montarPedido não deve criar pedido nem auditar quando o cliente não existe")
    void montarPedido_clienteInexistente_naoDeveCriarPedido() {
        when(usuarioService.buscarPorEmail("fantasma@teste.com"))
                .thenThrow(new IllegalArgumentException("Cliente não encontrado"));

        assertThatThrownBy(() -> tools.montarPedido(
                "fantasma@teste.com", List.of(new CartItemRequest(1L, 1)), 1L, 1L, null))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(auditoriaService);
    }
}