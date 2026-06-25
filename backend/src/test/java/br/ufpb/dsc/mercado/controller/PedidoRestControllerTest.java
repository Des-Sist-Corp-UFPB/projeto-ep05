package br.ufpb.dsc.mercado.controller;

import br.ufpb.dsc.mercado.domain.*;
import br.ufpb.dsc.mercado.dto.CartItemRequest;
import br.ufpb.dsc.mercado.dto.CheckoutRequest;
import br.ufpb.dsc.mercado.dto.PedidoDTO;
import br.ufpb.dsc.mercado.service.CupomService;
import br.ufpb.dsc.mercado.service.PedidoService;
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
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PedidoRestController — Testes Unitários")
class PedidoRestControllerTest {

    @Mock PedidoService pedidoService;
    @Mock UsuarioService usuarioService;
    @Mock CupomService cupomService;

    private PedidoRestController controller;

    @BeforeEach
    void setUp() {
        controller = new PedidoRestController(pedidoService, usuarioService, cupomService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Usuario usuarioLogado(Long id, String email, Papel papel) {
        Usuario u = new Usuario("Ana", email, "hash", papel);
        u.setId(id);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, null, List.of()));
        when(usuarioService.buscarPorEmail(email)).thenReturn(u);
        return u;
    }

    private Pedido pedidoDoCliente(Long id, Usuario cliente) {
        Pedido p = new Pedido();
        p.setId(id);
        p.setCliente(cliente);
        return p;
    }

    private PedidoDTO dtoBasico(Long id) {
        return new PedidoDTO(id, 1L, "Ana", "ana@teste.com", null, null, null,
                StatusPedido.PAGO, BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN, null,
                List.of(), Instant.now(), Instant.now());
    }

    // ── checkout ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("checkout deve criar o pedido e retornar 200 com o DTO")
    void checkout_dadosValidos_deveRetornarPedidoCriado() {
        Usuario cliente = usuarioLogado(1L, "ana@teste.com", Papel.CLIENTE);
        CheckoutRequest request = new CheckoutRequest(10L, 20L, null, List.of(new CartItemRequest(100L, 1)));
        Pedido pedidoCriado = pedidoDoCliente(1L, cliente);
        PedidoDTO dto = dtoBasico(1L);

        when(pedidoService.criarPedido(1L, request)).thenReturn(pedidoCriado);
        when(pedidoService.converterParaDTO(pedidoCriado)).thenReturn(dto);

        ResponseEntity<?> resposta = controller.checkout(request);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).isEqualTo(dto);
    }

    @Test
    @DisplayName("checkout deve retornar 400 com a mensagem de erro quando o service lança IllegalArgumentException")
    void checkout_erroDeNegocio_deveRetornar400() {
        usuarioLogado(1L, "ana@teste.com", Papel.CLIENTE);
        CheckoutRequest request = new CheckoutRequest(10L, 20L, null, List.of(new CartItemRequest(100L, 1)));
        when(pedidoService.criarPedido(1L, request))
                .thenThrow(new IllegalArgumentException("Estoque insuficiente para o produto 'Brownie'"));

        ResponseEntity<?> resposta = controller.checkout(request);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).isEqualTo("Estoque insuficiente para o produto 'Brownie'");
    }

    // ── listarPedidosDoCliente ───────────────────────────────────────────────

    @Test
    @DisplayName("listarPedidosDoCliente deve retornar a página de pedidos do usuário logado, convertida para DTO")
    void listarPedidosDoCliente_deveRetornarPaginaDeDTOs() {
        Usuario cliente = usuarioLogado(1L, "ana@teste.com", Papel.CLIENTE);
        Pageable pageable = Pageable.ofSize(10);
        Pedido pedido = pedidoDoCliente(1L, cliente);
        Page<Pedido> page = new PageImpl<>(List.of(pedido));
        PedidoDTO dto = dtoBasico(1L);

        when(pedidoService.listarPorCliente(1L, pageable)).thenReturn(page);
        when(pedidoService.converterParaDTO(pedido)).thenReturn(dto);

        ResponseEntity<Page<PedidoDTO>> resposta = controller.listarPedidosDoCliente(pageable);

        assertThat(resposta.getBody().getContent()).containsExactly(dto);
    }

    // ── obterPedido ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("obterPedido deve retornar o pedido quando ele pertence ao cliente logado")
    void obterPedido_pertenceAoCliente_deveRetornar() {
        Usuario cliente = usuarioLogado(1L, "ana@teste.com", Papel.CLIENTE);
        Pedido pedido = pedidoDoCliente(5L, cliente);
        PedidoDTO dto = dtoBasico(5L);
        when(pedidoService.buscarPorId(5L)).thenReturn(pedido);
        when(pedidoService.converterParaDTO(pedido)).thenReturn(dto);

        ResponseEntity<?> resposta = controller.obterPedido(5L);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).isEqualTo(dto);
    }

    @Test
    @DisplayName("obterPedido deve retornar 403 quando o pedido pertence a outro cliente")
    void obterPedido_deOutroCliente_deveRetornar403() {
        Usuario logado = usuarioLogado(1L, "ana@teste.com", Papel.CLIENTE);
        Usuario dono = new Usuario("Bia", "bia@teste.com", "hash", Papel.CLIENTE);
        dono.setId(2L);
        Pedido pedido = pedidoDoCliente(5L, dono);
        when(pedidoService.buscarPorId(5L)).thenReturn(pedido);

        ResponseEntity<?> resposta = controller.obterPedido(5L);

        assertThat(resposta.getStatusCode().value()).isEqualTo(403);
        assertThat(resposta.getBody()).isEqualTo("Você não tem permissão para visualizar este pedido");
    }

    @Test
    @DisplayName("obterPedido deve permitir acesso quando o usuário logado é ADMIN, mesmo não sendo o dono")
    void obterPedido_usuarioAdmin_devePermitirAcesso() {
        usuarioLogado(9L, "admin@teste.com", Papel.ADMIN);
        Usuario dono = new Usuario("Bia", "bia@teste.com", "hash", Papel.CLIENTE);
        dono.setId(2L);
        Pedido pedido = pedidoDoCliente(5L, dono);
        PedidoDTO dto = dtoBasico(5L);
        when(pedidoService.buscarPorId(5L)).thenReturn(pedido);
        when(pedidoService.converterParaDTO(pedido)).thenReturn(dto);

        ResponseEntity<?> resposta = controller.obterPedido(5L);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("obterPedido deve retornar 404 quando o pedido não existe")
    void obterPedido_inexistente_deveRetornar404() {
        usuarioLogado(1L, "ana@teste.com", Papel.CLIENTE);
        when(pedidoService.buscarPorId(99L)).thenThrow(new IllegalArgumentException("Pedido não encontrado com ID: 99"));

        ResponseEntity<?> resposta = controller.obterPedido(99L);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── validarCupom ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("validarCupom deve retornar os dados do cupom quando válido")
    void validarCupom_valido_deveRetornarDados() {
        Cupom cupom = new Cupom("PROMO10", BigDecimal.TEN, TipoCupom.PORCENTAGEM, Instant.now().plus(1, ChronoUnit.DAYS));
        when(cupomService.validarCupom("PROMO10")).thenReturn(cupom);

        ResponseEntity<?> resposta = controller.validarCupom("PROMO10");

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        var body = (PedidoRestController.CupomDTO) resposta.getBody();
        assertThat(body.codigo()).isEqualTo("PROMO10");
        assertThat(body.tipo()).isEqualTo("PORCENTAGEM");
    }

    @Test
    @DisplayName("validarCupom deve retornar 400 com a mensagem de erro quando o cupom é inválido")
    void validarCupom_invalido_deveRetornar400() {
        when(cupomService.validarCupom("INVALIDO")).thenThrow(new IllegalArgumentException("Cupom inválido ou inativo"));

        ResponseEntity<?> resposta = controller.validarCupom("INVALIDO");

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).isEqualTo("Cupom inválido ou inativo");
    }
}
