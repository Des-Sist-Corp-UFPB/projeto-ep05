package br.ufpb.dsc.mercado.service;

import br.ufpb.dsc.mercado.domain.*;
import br.ufpb.dsc.mercado.dto.CartItemRequest;
import br.ufpb.dsc.mercado.dto.CheckoutRequest;
import br.ufpb.dsc.mercado.dto.PedidoDTO;
import br.ufpb.dsc.mercado.repository.PedidoRepository;
import br.ufpb.dsc.mercado.repository.ProdutoRepository;
import br.ufpb.dsc.mercado.service.MercadoPagoService;
import com.mercadopago.resources.payment.Payment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PedidoService — Testes Unitários")
class PedidoServiceTest {

    @Mock PedidoRepository pedidoRepository;
    @Mock ProdutoRepository produtoRepository;
    @Mock UsuarioService usuarioService;
    @Mock CupomService cupomService;
    @Mock MercadoPagoService mercadoPagoService;

    private PedidoService service;

    @BeforeEach
    void setUp() {
        service = new PedidoService(pedidoRepository, produtoRepository, usuarioService, cupomService, mercadoPagoService);

        // Mock padrao do Payment: testes que chegam a cobrar recebem um Payment aprovado.
        // Tudo lenient para nao falhar nos testes que nao chegam a chamar cobrarComToken.
        Payment mockPayment = Mockito.mock(Payment.class);
        Mockito.lenient().when(mockPayment.getId()).thenReturn(999L);
        Mockito.lenient().when(mockPayment.getStatus()).thenReturn("approved");
        Mockito.lenient()
               .when(mercadoPagoService.cobrarComToken(any(), any(), any(), any(), anyInt()))
               .thenReturn(mockPayment);
    }

    // ── Helpers de fixture ──────────────────────────────────────────────────

    private Usuario usuarioComId(Long id) {
        Usuario u = new Usuario("Ana", "ana@teste.com", "hash", Papel.CLIENTE);
        u.setId(id);
        return u;
    }

    private Endereco enderecoDoCliente(Long id, Usuario cliente) {
        Endereco e = new Endereco();
        e.setId(id);
        e.setCliente(cliente);
        e.setLogradouro("Rua A");
        e.setNumero("100");
        e.setBairro("Centro");
        e.setCidade("João Pessoa");
        e.setEstado("PB");
        e.setCep("58000000");
        e.setPrincipal(true);
        return e;
    }

    private Cartao cartaoDoCliente(Long id, Usuario cliente) {
        Cartao c = new Cartao();
        c.setId(id);
        c.setCliente(cliente);
        c.setNomeTitular("Ana Silva");
        c.setBandeira("VISA");
        c.setQuatroUltimosDigitos("1111");
        c.setTokenPagamento("tok_123");
        c.setDataExpiracao("12/2030");
        return c;
    }

    private Produto produtoAtivo(Long id, String nome, BigDecimal preco, Integer estoque) {
        Produto p = new Produto(nome, "desc", preco, null, estoque);
        p.setId(id);
        p.setAtivo(true);
        return p;
    }

    private CheckoutRequest checkoutRequest(Long enderecoId, Long cartaoId, String cupom, List<CartItemRequest> itens) {
        return new CheckoutRequest(enderecoId, cartaoId, cupom, itens);
    }

    // ── Consultas simples ────────────────────────────────────────────────────

    @Test
    @DisplayName("calcularFaturamentoTotal deve delegar ao repositório")
    void calcularFaturamentoTotal_deveDelegar() {
        when(pedidoRepository.somarFaturamentoTotal()).thenReturn(BigDecimal.valueOf(500));

        assertThat(service.calcularFaturamentoTotal()).isEqualByComparingTo(BigDecimal.valueOf(500));
    }

    @Test
    @DisplayName("listarTodos deve delegar ao repositório ordenado por data desc")
    void listarTodos_deveDelegar() {
        Pageable pageable = Pageable.ofSize(10);
        Page<Pedido> page = new PageImpl<>(List.of(new Pedido()));
        when(pedidoRepository.findAllByOrderByCriadoEmDesc(pageable)).thenReturn(page);

        assertThat(service.listarTodos(pageable).getContent()).hasSize(1);
    }

    @Test
    @DisplayName("listarPorCliente deve delegar ao repositório filtrando por clienteId")
    void listarPorCliente_deveDelegar() {
        Pageable pageable = Pageable.ofSize(10);
        Page<Pedido> page = new PageImpl<>(List.of(new Pedido()));
        when(pedidoRepository.findByClienteIdOrderByCriadoEmDesc(1L, pageable)).thenReturn(page);

        assertThat(service.listarPorCliente(1L, pageable).getContent()).hasSize(1);
    }

    @Test
    @DisplayName("buscarPorId deve retornar o pedido quando existe")
    void buscarPorId_existente_deveRetornar() {
        Pedido pedido = new Pedido();
        pedido.setId(1L);
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

        assertThat(service.buscarPorId(1L).getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("buscarPorId deve lançar exceção quando não existe")
    void buscarPorId_inexistente_deveLancarExcecao() {
        when(pedidoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Pedido não encontrado com ID: 99");
    }

    // ── criarPedido ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("criarPedido deve calcular total, decrementar estoque e salvar sem cupom")
    void criarPedido_semCupom_deveCalcularTotalEDecrementarEstoque() {
        Usuario cliente = usuarioComId(1L);
        Endereco endereco = enderecoDoCliente(10L, cliente);
        Cartao cartao = cartaoDoCliente(20L, cliente);
        Produto produto = produtoAtivo(100L, "Brownie", BigDecimal.valueOf(10), 5);

        CheckoutRequest request = checkoutRequest(10L, 20L, null, List.of(new CartItemRequest(100L, 2)));

        when(usuarioService.buscarPorId(1L)).thenReturn(cliente);
        when(usuarioService.buscarEnderecoPorId(10L)).thenReturn(endereco);
        when(usuarioService.buscarCartaoPorId(20L)).thenReturn(cartao);
        when(produtoRepository.findById(100L)).thenReturn(Optional.of(produto));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));

        Pedido resultado = service.criarPedido(1L, request);

        assertThat(resultado.getStatus()).isEqualTo(StatusPedido.PAGO);
        assertThat(resultado.getTotalProdutos()).isEqualByComparingTo(BigDecimal.valueOf(20));
        assertThat(resultado.getTotalDesconto()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(resultado.getTotalGeral()).isEqualByComparingTo(BigDecimal.valueOf(20));
        assertThat(resultado.getItens()).hasSize(1);
        assertThat(produto.getEstoque()).isEqualTo(3); // 5 - 2
        verify(produtoRepository).save(produto);
    }

    @Test
    @DisplayName("criarPedido deve lançar exceção quando o endereço não pertence ao cliente")
    void criarPedido_enderecoDeOutroCliente_deveLancarExcecao() {
        Usuario cliente = usuarioComId(1L);
        Usuario outroCliente = usuarioComId(2L);
        Endereco endereco = enderecoDoCliente(10L, outroCliente);

        CheckoutRequest request = checkoutRequest(10L, 20L, null, List.of(new CartItemRequest(100L, 1)));

        when(usuarioService.buscarPorId(1L)).thenReturn(cliente);
        when(usuarioService.buscarEnderecoPorId(10L)).thenReturn(endereco);

        assertThatThrownBy(() -> service.criarPedido(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("O endereço de entrega fornecido não pertence a este cliente");

        verify(pedidoRepository, never()).save(any());
    }

    @Test
    @DisplayName("criarPedido deve lançar exceção quando o cartão não pertence ao cliente")
    void criarPedido_cartaoDeOutroCliente_deveLancarExcecao() {
        Usuario cliente = usuarioComId(1L);
        Usuario outroCliente = usuarioComId(2L);
        Endereco endereco = enderecoDoCliente(10L, cliente);
        Cartao cartao = cartaoDoCliente(20L, outroCliente);

        CheckoutRequest request = checkoutRequest(10L, 20L, null, List.of(new CartItemRequest(100L, 1)));

        when(usuarioService.buscarPorId(1L)).thenReturn(cliente);
        when(usuarioService.buscarEnderecoPorId(10L)).thenReturn(endereco);
        when(usuarioService.buscarCartaoPorId(20L)).thenReturn(cartao);

        assertThatThrownBy(() -> service.criarPedido(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("O cartão de pagamento fornecido não pertence a este cliente");

        verify(pedidoRepository, never()).save(any());
    }

    @Test
    @DisplayName("criarPedido deve lançar exceção quando o produto não existe")
    void criarPedido_produtoInexistente_deveLancarExcecao() {
        Usuario cliente = usuarioComId(1L);
        Endereco endereco = enderecoDoCliente(10L, cliente);
        Cartao cartao = cartaoDoCliente(20L, cliente);

        CheckoutRequest request = checkoutRequest(10L, 20L, null, List.of(new CartItemRequest(999L, 1)));

        when(usuarioService.buscarPorId(1L)).thenReturn(cliente);
        when(usuarioService.buscarEnderecoPorId(10L)).thenReturn(endereco);
        when(usuarioService.buscarCartaoPorId(20L)).thenReturn(cartao);
        when(produtoRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.criarPedido(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Produto não encontrado com ID: 999");
    }

    @Test
    @DisplayName("criarPedido deve lançar exceção quando o produto está inativo")
    void criarPedido_produtoInativo_deveLancarExcecao() {
        Usuario cliente = usuarioComId(1L);
        Endereco endereco = enderecoDoCliente(10L, cliente);
        Cartao cartao = cartaoDoCliente(20L, cliente);
        Produto produto = produtoAtivo(100L, "Brownie", BigDecimal.TEN, 5);
        produto.setAtivo(false);

        CheckoutRequest request = checkoutRequest(10L, 20L, null, List.of(new CartItemRequest(100L, 1)));

        when(usuarioService.buscarPorId(1L)).thenReturn(cliente);
        when(usuarioService.buscarEnderecoPorId(10L)).thenReturn(endereco);
        when(usuarioService.buscarCartaoPorId(20L)).thenReturn(cartao);
        when(produtoRepository.findById(100L)).thenReturn(Optional.of(produto));

        assertThatThrownBy(() -> service.criarPedido(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("não está ativo para compra");
    }

    @Test
    @DisplayName("criarPedido deve lançar exceção quando o estoque é insuficiente")
    void criarPedido_estoqueInsuficiente_deveLancarExcecao() {
        Usuario cliente = usuarioComId(1L);
        Endereco endereco = enderecoDoCliente(10L, cliente);
        Cartao cartao = cartaoDoCliente(20L, cliente);
        Produto produto = produtoAtivo(100L, "Brownie", BigDecimal.TEN, 1);

        CheckoutRequest request = checkoutRequest(10L, 20L, null, List.of(new CartItemRequest(100L, 5)));

        when(usuarioService.buscarPorId(1L)).thenReturn(cliente);
        when(usuarioService.buscarEnderecoPorId(10L)).thenReturn(endereco);
        when(usuarioService.buscarCartaoPorId(20L)).thenReturn(cartao);
        when(produtoRepository.findById(100L)).thenReturn(Optional.of(produto));

        assertThatThrownBy(() -> service.criarPedido(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Estoque insuficiente");

        verify(produtoRepository, never()).save(any());
    }

    @Test
    @DisplayName("criarPedido deve aplicar desconto percentual do cupom corretamente")
    void criarPedido_cupomPorcentagem_deveAplicarDesconto() {
        Usuario cliente = usuarioComId(1L);
        Endereco endereco = enderecoDoCliente(10L, cliente);
        Cartao cartao = cartaoDoCliente(20L, cliente);
        Produto produto = produtoAtivo(100L, "Brownie", BigDecimal.valueOf(100), 10);

        Cupom cupom = new Cupom("PROMO10", BigDecimal.valueOf(10), TipoCupom.PORCENTAGEM, Instant.now().plus(1, ChronoUnit.DAYS));

        CheckoutRequest request = checkoutRequest(10L, 20L, "PROMO10", List.of(new CartItemRequest(100L, 1)));

        when(usuarioService.buscarPorId(1L)).thenReturn(cliente);
        when(usuarioService.buscarEnderecoPorId(10L)).thenReturn(endereco);
        when(usuarioService.buscarCartaoPorId(20L)).thenReturn(cartao);
        when(produtoRepository.findById(100L)).thenReturn(Optional.of(produto));
        when(cupomService.validarCupom("PROMO10")).thenReturn(cupom);
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));

        Pedido resultado = service.criarPedido(1L, request);

        // 100 * 10% = 10 de desconto
        assertThat(resultado.getTotalProdutos()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(resultado.getTotalDesconto()).isEqualByComparingTo(BigDecimal.valueOf(10).setScale(2));
        assertThat(resultado.getTotalGeral()).isEqualByComparingTo(BigDecimal.valueOf(90).setScale(2));
        assertThat(resultado.getCupom()).isEqualTo(cupom);
    }

    @Test
    @DisplayName("criarPedido deve aplicar desconto de valor fixo do cupom corretamente")
    void criarPedido_cupomValorFixo_deveAplicarDesconto() {
        Usuario cliente = usuarioComId(1L);
        Endereco endereco = enderecoDoCliente(10L, cliente);
        Cartao cartao = cartaoDoCliente(20L, cliente);
        Produto produto = produtoAtivo(100L, "Brownie", BigDecimal.valueOf(50), 10);

        Cupom cupom = new Cupom("DESC15", BigDecimal.valueOf(15), TipoCupom.VALOR_FIXO, Instant.now().plus(1, ChronoUnit.DAYS));

        CheckoutRequest request = checkoutRequest(10L, 20L, "DESC15", List.of(new CartItemRequest(100L, 1)));

        when(usuarioService.buscarPorId(1L)).thenReturn(cliente);
        when(usuarioService.buscarEnderecoPorId(10L)).thenReturn(endereco);
        when(usuarioService.buscarCartaoPorId(20L)).thenReturn(cartao);
        when(produtoRepository.findById(100L)).thenReturn(Optional.of(produto));
        when(cupomService.validarCupom("DESC15")).thenReturn(cupom);
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));

        Pedido resultado = service.criarPedido(1L, request);

        assertThat(resultado.getTotalDesconto()).isEqualByComparingTo(BigDecimal.valueOf(15));
        assertThat(resultado.getTotalGeral()).isEqualByComparingTo(BigDecimal.valueOf(35));
    }

    @Test
    @DisplayName("criarPedido deve limitar o desconto ao valor total dos produtos")
    void criarPedido_descontoMaiorQueTotal_deveSerLimitado() {
        Usuario cliente = usuarioComId(1L);
        Endereco endereco = enderecoDoCliente(10L, cliente);
        Cartao cartao = cartaoDoCliente(20L, cliente);
        Produto produto = produtoAtivo(100L, "Brownie", BigDecimal.valueOf(10), 10);

        Cupom cupom = new Cupom("MEGA100", BigDecimal.valueOf(100), TipoCupom.VALOR_FIXO, Instant.now().plus(1, ChronoUnit.DAYS));

        CheckoutRequest request = checkoutRequest(10L, 20L, "MEGA100", List.of(new CartItemRequest(100L, 1)));

        when(usuarioService.buscarPorId(1L)).thenReturn(cliente);
        when(usuarioService.buscarEnderecoPorId(10L)).thenReturn(endereco);
        when(usuarioService.buscarCartaoPorId(20L)).thenReturn(cartao);
        when(produtoRepository.findById(100L)).thenReturn(Optional.of(produto));
        when(cupomService.validarCupom("MEGA100")).thenReturn(cupom);
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));

        Pedido resultado = service.criarPedido(1L, request);

        // desconto não pode ultrapassar os 10 de total de produtos
        assertThat(resultado.getTotalDesconto()).isEqualByComparingTo(BigDecimal.valueOf(10));
        assertThat(resultado.getTotalGeral()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("criarPedido deve lançar exceção quando o cupom informado é inválido")
    void criarPedido_cupomInvalido_deveLancarExcecao() {
        Usuario cliente = usuarioComId(1L);
        Endereco endereco = enderecoDoCliente(10L, cliente);
        Cartao cartao = cartaoDoCliente(20L, cliente);
        Produto produto = produtoAtivo(100L, "Brownie", BigDecimal.TEN, 10);

        CheckoutRequest request = checkoutRequest(10L, 20L, "INVALIDO", List.of(new CartItemRequest(100L, 1)));

        when(usuarioService.buscarPorId(1L)).thenReturn(cliente);
        when(usuarioService.buscarEnderecoPorId(10L)).thenReturn(endereco);
        when(usuarioService.buscarCartaoPorId(20L)).thenReturn(cartao);
        when(produtoRepository.findById(100L)).thenReturn(Optional.of(produto));
        when(cupomService.validarCupom("INVALIDO")).thenThrow(new IllegalArgumentException("Cupom inválido ou inativo"));

        assertThatThrownBy(() -> service.criarPedido(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Erro no cupom");

        verify(pedidoRepository, never()).save(any());
    }


    @Test
    @DisplayName("criarPedido deve aprovar localmente quando o Mercado Pago recusa por credenciais não autorizadas (ambiente de teste)")
    void criarPedido_pagamentoUnauthorized_deveAprovarLocalmente() {
        Usuario cliente = usuarioComId(1L);
        Endereco endereco = enderecoDoCliente(10L, cliente);
        Cartao cartao = cartaoDoCliente(20L, cliente);
        Produto produto = produtoAtivo(100L, "Brownie", BigDecimal.valueOf(10), 5);

        CheckoutRequest request = checkoutRequest(10L, 20L, null, List.of(new CartItemRequest(100L, 2)));

        when(usuarioService.buscarPorId(1L)).thenReturn(cliente);
        when(usuarioService.buscarEnderecoPorId(10L)).thenReturn(endereco);
        when(usuarioService.buscarCartaoPorId(20L)).thenReturn(cartao);
        when(produtoRepository.findById(100L)).thenReturn(Optional.of(produto));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mercadoPagoService.cobrarComToken(any(), any(), any(), any(), anyInt()))
                .thenThrow(new IllegalArgumentException("Unauthorized use of live credentials"));

        Pedido resultado = service.criarPedido(1L, request);

        assertThat(resultado.getStatus()).isEqualTo(StatusPedido.PAGO);
        assertThat(produto.getEstoque()).isEqualTo(3); // não restaura estoque
    }

    @Test
    @DisplayName("criarPedido deve cancelar e devolver estoque quando o pagamento é genuinamente recusado")
    void criarPedido_pagamentoRecusado_deveCancelarEDevolverEstoque() {
        Usuario cliente = usuarioComId(1L);
        Endereco endereco = enderecoDoCliente(10L, cliente);
        Cartao cartao = cartaoDoCliente(20L, cliente);
        Produto produto = produtoAtivo(100L, "Brownie", BigDecimal.valueOf(10), 5);

        CheckoutRequest request = checkoutRequest(10L, 20L, null, List.of(new CartItemRequest(100L, 2)));

        when(usuarioService.buscarPorId(1L)).thenReturn(cliente);
        when(usuarioService.buscarEnderecoPorId(10L)).thenReturn(endereco);
        when(usuarioService.buscarCartaoPorId(20L)).thenReturn(cartao);
        when(produtoRepository.findById(100L)).thenReturn(Optional.of(produto));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mercadoPagoService.cobrarComToken(any(), any(), any(), any(), anyInt()))
                .thenThrow(new IllegalArgumentException("cc_rejected_insufficient_amount"));

        assertThatThrownBy(() -> service.criarPedido(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Pagamento recusado");

        assertThat(produto.getEstoque()).isEqualTo(5); // 5 -2 decrementado, +2 devolvido no catch
        verify(produtoRepository, times(2)).save(produto); // decremento + devolução
    }

    // ── listarTodos / listarPorCliente / contarPorStatus (overloads com status) ─

    @Test
    @DisplayName("listarTodos(status, pageable) sem status deve usar findAllByOrderByCriadoEmDesc")
    void listarTodosComStatus_statusNulo_deveUsarMetodoSimples() {
        Pageable pageable = Pageable.ofSize(10);
        Page<Pedido> page = new PageImpl<>(List.of(new Pedido()));
        when(pedidoRepository.findAllByOrderByCriadoEmDesc(pageable)).thenReturn(page);

        assertThat(service.listarTodos((StatusPedido) null, pageable).getContent()).hasSize(1);
        verify(pedidoRepository, never()).findByStatusOrderByCriadoEmDesc(any(), any());
    }

    @Test
    @DisplayName("listarTodos(status, pageable) com status deve filtrar por status")
    void listarTodosComStatus_statusInformado_deveFiltrar() {
        Pageable pageable = Pageable.ofSize(10);
        Page<Pedido> page = new PageImpl<>(List.of(new Pedido()));
        when(pedidoRepository.findByStatusOrderByCriadoEmDesc(StatusPedido.PAGO, pageable)).thenReturn(page);

        assertThat(service.listarTodos(StatusPedido.PAGO, pageable).getContent()).hasSize(1);
    }

    @Test
    @DisplayName("listarPorCliente(clienteId, status, pageable) sem status deve usar método simples")
    void listarPorClienteComStatus_statusNulo_deveUsarMetodoSimples() {
        Pageable pageable = Pageable.ofSize(10);
        Page<Pedido> page = new PageImpl<>(List.of(new Pedido()));
        when(pedidoRepository.findByClienteIdOrderByCriadoEmDesc(1L, pageable)).thenReturn(page);

        assertThat(service.listarPorCliente(1L, null, pageable).getContent()).hasSize(1);
        verify(pedidoRepository, never()).findByClienteIdAndStatusOrderByCriadoEmDesc(any(), any(), any());
    }

    @Test
    @DisplayName("listarPorCliente(clienteId, status, pageable) com status deve filtrar por cliente e status")
    void listarPorClienteComStatus_statusInformado_deveFiltrar() {
        Pageable pageable = Pageable.ofSize(10);
        Page<Pedido> page = new PageImpl<>(List.of(new Pedido()));
        when(pedidoRepository.findByClienteIdAndStatusOrderByCriadoEmDesc(1L, StatusPedido.PAGO, pageable))
                .thenReturn(page);

        assertThat(service.listarPorCliente(1L, StatusPedido.PAGO, pageable).getContent()).hasSize(1);
    }

    @Test
    @DisplayName("contarPorStatus deve delegar ao repositório")
    void contarPorStatus_deveDelegar() {
        when(pedidoRepository.countByStatus(StatusPedido.PAGO)).thenReturn(7L);

        assertThat(service.contarPorStatus(StatusPedido.PAGO)).isEqualTo(7L);
    }

    // ── atualizarStatus ──────────────────────────────────────────────────────

    @Test
    @DisplayName("atualizarStatus deve alterar o status quando o pedido não está finalizado")
    void atualizarStatus_pedidoAtivo_deveAlterarStatus() {
        Pedido pedido = new Pedido();
        pedido.setId(1L);
        pedido.setStatus(StatusPedido.PAGO);
        pedido.setItens(List.of());

        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));

        Pedido resultado = service.atualizarStatus(1L, StatusPedido.ENVIADO);

        assertThat(resultado.getStatus()).isEqualTo(StatusPedido.ENVIADO);
    }

    @Test
    @DisplayName("atualizarStatus deve lançar exceção quando o pedido já está cancelado")
    void atualizarStatus_jaCancelado_deveLancarExcecao() {
        Pedido pedido = new Pedido();
        pedido.setId(1L);
        pedido.setStatus(StatusPedido.CANCELADO);

        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> service.atualizarStatus(1L, StatusPedido.ENVIADO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("já CANCELADO");

        verify(pedidoRepository, never()).save(any());
    }

    @Test
    @DisplayName("atualizarStatus deve lançar exceção quando o pedido já foi entregue")
    void atualizarStatus_jaEntregue_deveLancarExcecao() {
        Pedido pedido = new Pedido();
        pedido.setId(1L);
        pedido.setStatus(StatusPedido.ENTREGUE);

        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> service.atualizarStatus(1L, StatusPedido.CANCELADO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("já ENTREGUE");
    }

    @Test
    @DisplayName("atualizarStatus para CANCELADO deve devolver os produtos ao estoque")
    void atualizarStatus_paraCancelado_deveDevolverEstoque() {
        Produto produto = produtoAtivo(100L, "Brownie", BigDecimal.TEN, 3);
        Pedido pedido = new Pedido();
        pedido.setId(1L);
        pedido.setStatus(StatusPedido.PAGO);
        pedido.setItens(List.of(new PedidoItem(pedido, produto, 2, BigDecimal.TEN)));

        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));

        service.atualizarStatus(1L, StatusPedido.CANCELADO);

        assertThat(produto.getEstoque()).isEqualTo(5); // 3 + 2 devolvidos
        verify(produtoRepository).save(produto);
    }

    @Test
    @DisplayName("atualizarStatus para CANCELADO com motivo informado deve usá-lo")
    void atualizarStatus_paraCanceladoComMotivo_deveUsarMotivoInformado() {
        Pedido pedido = new Pedido();
        pedido.setId(1L);
        pedido.setStatus(StatusPedido.PAGO);
        pedido.setItens(List.of());

        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));

        Pedido resultado = service.atualizarStatus(1L, StatusPedido.CANCELADO, "Mudei de ideia");

        assertThat(resultado.getMotivoCancelamento()).isEqualTo("Mudei de ideia");
    }

    @Test
    @DisplayName("atualizarStatus para CANCELADO com motivo em branco deve usar mensagem padrão")
    void atualizarStatus_paraCanceladoComMotivoEmBranco_deveUsarMensagemPadrao() {
        Pedido pedido = new Pedido();
        pedido.setId(1L);
        pedido.setStatus(StatusPedido.PAGO);
        pedido.setItens(List.of());

        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));

        Pedido resultado = service.atualizarStatus(1L, StatusPedido.CANCELADO, "   ");

        assertThat(resultado.getMotivoCancelamento()).isEqualTo("Cancelado pelo cliente");
    }

    // ── atualizarCodigoRastreamento ──────────────────────────────────────────

    @Test
    @DisplayName("atualizarCodigoRastreamento deve mudar status de PAGO para ENVIADO")
    void atualizarCodigoRastreamento_statusPago_deveMudarParaEnviado() {
        Pedido pedido = new Pedido();
        pedido.setId(1L);
        pedido.setStatus(StatusPedido.PAGO);

        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));

        Pedido resultado = service.atualizarCodigoRastreamento(1L, "BR123456789");

        assertThat(resultado.getCodigoRastreamento()).isEqualTo("BR123456789");
        assertThat(resultado.getStatus()).isEqualTo(StatusPedido.ENVIADO);
    }

    @Test
    @DisplayName("atualizarCodigoRastreamento não deve alterar status quando o pedido não está PAGO")
    void atualizarCodigoRastreamento_statusDiferenteDePago_naoDeveAlterarStatus() {
        Pedido pedido = new Pedido();
        pedido.setId(1L);
        pedido.setStatus(StatusPedido.ENVIADO);

        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));

        Pedido resultado = service.atualizarCodigoRastreamento(1L, "BR000");

        assertThat(resultado.getStatus()).isEqualTo(StatusPedido.ENVIADO);
    }

    // ── converterParaDTO ─────────────────────────────────────────────────────

    @Test
    @DisplayName("converterParaDTO deve mapear todos os campos, incluindo cartão e cupom")
    void converterParaDTO_comCartaoECupom_deveMapearTudo() {
        Usuario cliente = usuarioComId(1L);
        Endereco endereco = enderecoDoCliente(10L, cliente);
        Cartao cartao = cartaoDoCliente(20L, cliente);
        Produto produto = produtoAtivo(100L, "Brownie", BigDecimal.TEN, 10);
        Cupom cupom = new Cupom("PROMO10", BigDecimal.TEN, TipoCupom.PORCENTAGEM, Instant.now().plus(1, ChronoUnit.DAYS));

        Pedido pedido = new Pedido();
        pedido.setId(1L);
        pedido.setCliente(cliente);
        pedido.setEnderecoEntrega(endereco);
        pedido.setCartao(cartao);
        pedido.setCupom(cupom);
        pedido.setStatus(StatusPedido.PAGO);
        pedido.setTotalProdutos(BigDecimal.TEN);
        pedido.setTotalDesconto(BigDecimal.ONE);
        pedido.setTotalGeral(BigDecimal.valueOf(9));
        pedido.setItens(List.of(new PedidoItem(pedido, produto, 1, BigDecimal.TEN)));

        PedidoDTO dto = service.converterParaDTO(pedido);

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.clienteId()).isEqualTo(1L);
        assertThat(dto.clienteNome()).isEqualTo("Ana");
        assertThat(dto.enderecoEntrega().cep()).isEqualTo("58000000");
        assertThat(dto.cartao()).isNotNull();
        assertThat(dto.cartao().quatroUltimosDigitos()).isEqualTo("1111");
        assertThat(dto.codigoCupom()).isEqualTo("PROMO10");
        assertThat(dto.itens()).hasSize(1);
        assertThat(dto.itens().get(0).produtoNome()).isEqualTo("Brownie");
    }

    @Test
    @DisplayName("converterParaDTO deve retornar cartão e cupom nulos quando o pedido não os possui")
    void converterParaDTO_semCartaoSemCupom_deveRetornarNulos() {
        Usuario cliente = usuarioComId(1L);
        Endereco endereco = enderecoDoCliente(10L, cliente);

        Pedido pedido = new Pedido();
        pedido.setId(1L);
        pedido.setCliente(cliente);
        pedido.setEnderecoEntrega(endereco);
        pedido.setStatus(StatusPedido.AGUARDANDO_PAGAMENTO);
        pedido.setTotalProdutos(BigDecimal.TEN);
        pedido.setTotalDesconto(BigDecimal.ZERO);
        pedido.setTotalGeral(BigDecimal.TEN);
        pedido.setItens(List.of());

        PedidoDTO dto = service.converterParaDTO(pedido);

        assertThat(dto.cartao()).isNull();
        assertThat(dto.codigoCupom()).isNull();
        assertThat(dto.itens()).isEmpty();
    }
}
