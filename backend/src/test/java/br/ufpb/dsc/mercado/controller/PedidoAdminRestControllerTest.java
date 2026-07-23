package br.ufpb.dsc.mercado.controller;

import br.ufpb.dsc.mercado.audit.AuditoriaService;
import br.ufpb.dsc.mercado.domain.Pedido;
import br.ufpb.dsc.mercado.domain.StatusPedido;
import br.ufpb.dsc.mercado.dto.PedidoDTO;
import br.ufpb.dsc.mercado.service.PedidoService;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PedidoAdminRestController — Testes Unitários")
class PedidoAdminRestControllerTest {

    @Mock PedidoService pedidoService;
    @Mock AuditoriaService auditoriaService;

    private PedidoAdminRestController controller;

    @BeforeEach
    void setUp() {
        controller = new PedidoAdminRestController(pedidoService, auditoriaService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Authentication autenticar(String email) {
        Authentication auth = new UsernamePasswordAuthenticationToken(email, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
        return auth;
    }

    private PedidoDTO dtoDeTeste() {
        return new PedidoDTO(
                1L, 10L, "Cliente Teste", "cliente@teste.com",
                null, null, null, StatusPedido.PAGO,
                BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN,
                null, null, List.of(),
                Instant.now(), Instant.now());
    }

    @Test
    @DisplayName("listarTodos deve retornar página de pedidos convertidos em DTO")
    void listarTodos_sucesso() {
        Pedido pedido = mock(Pedido.class);
        PedidoDTO dto = dtoDeTeste();
        Pageable pageable = Pageable.unpaged();
        Page<Pedido> pagina = new PageImpl<>(List.of(pedido));

        when(pedidoService.listarTodos(pageable)).thenReturn(pagina);
        when(pedidoService.converterParaDTO(pedido)).thenReturn(dto);

        ResponseEntity<Page<PedidoDTO>> resposta = controller.listarTodos(pageable);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).isNotNull();
        assertThat(resposta.getBody().getContent()).containsExactly(dto);
        verify(pedidoService).listarTodos(pageable);
        verify(pedidoService).converterParaDTO(pedido);
    }

    @Test
    @DisplayName("listarTodos deve retornar página vazia quando não há pedidos")
    void listarTodos_vazio() {
        Pageable pageable = Pageable.unpaged();
        when(pedidoService.listarTodos(pageable)).thenReturn(Page.empty());

        ResponseEntity<Page<PedidoDTO>> resposta = controller.listarTodos(pageable);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).isNotNull();
        assertThat(resposta.getBody().getContent()).isEmpty();
    }

    @Test
    @DisplayName("obterPedido deve retornar o DTO do pedido encontrado")
    void obterPedido_sucesso() {
        Pedido pedido = mock(Pedido.class);
        PedidoDTO dto = dtoDeTeste();

        when(pedidoService.buscarPorId(1L)).thenReturn(pedido);
        when(pedidoService.converterParaDTO(pedido)).thenReturn(dto);

        ResponseEntity<PedidoDTO> resposta = controller.obterPedido(1L);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).isSameAs(dto);
    }

    @Test
    @DisplayName("atualizarStatus deve atualizar o status e registrar auditoria com o usuário autenticado")
    void atualizarStatus_comUsuarioAutenticado() {
        Pedido pedido = mock(Pedido.class);
        PedidoDTO dto = dtoDeTeste();
        Authentication auth = autenticar("admin@teste.com");

        when(pedidoService.atualizarStatus(1L, StatusPedido.ENVIADO)).thenReturn(pedido);
        when(pedidoService.converterParaDTO(pedido)).thenReturn(dto);

        ResponseEntity<PedidoDTO> resposta = controller.atualizarStatus(1L, StatusPedido.ENVIADO, auth);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).isSameAs(dto);
        verify(auditoriaService).registrarAdmin(
                eq("admin@teste.com"), eq("PEDIDO"),
                contains("Atualizou status do pedido #1"), eq(1L));
    }

    @Test
    @DisplayName("atualizarStatus deve usar 'admin' como ator quando não há autenticação")
    void atualizarStatus_semAutenticacao() {
        Pedido pedido = mock(Pedido.class);
        PedidoDTO dto = dtoDeTeste();

        when(pedidoService.atualizarStatus(2L, StatusPedido.CANCELADO)).thenReturn(pedido);
        when(pedidoService.converterParaDTO(pedido)).thenReturn(dto);

        ResponseEntity<PedidoDTO> resposta = controller.atualizarStatus(2L, StatusPedido.CANCELADO, null);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(auditoriaService).registrarAdmin(eq("admin"), eq("PEDIDO"), anyString(), eq(2L));
    }

    @Test
    @DisplayName("atualizarRastreamento deve atualizar o código e registrar auditoria com o usuário autenticado")
    void atualizarRastreamento_comUsuarioAutenticado() {
        Pedido pedido = mock(Pedido.class);
        PedidoDTO dto = dtoDeTeste();
        Authentication auth = autenticar("admin@teste.com");

        when(pedidoService.atualizarCodigoRastreamento(1L, "BR123456789")).thenReturn(pedido);
        when(pedidoService.converterParaDTO(pedido)).thenReturn(dto);

        ResponseEntity<PedidoDTO> resposta = controller.atualizarRastreamento(1L, "BR123456789", auth);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).isSameAs(dto);
        verify(auditoriaService).registrarAdmin(
                eq("admin@teste.com"), eq("PEDIDO"),
                contains("BR123456789"), eq(1L));
    }

    @Test
    @DisplayName("atualizarRastreamento deve usar 'admin' como ator quando não há autenticação")
    void atualizarRastreamento_semAutenticacao() {
        Pedido pedido = mock(Pedido.class);
        PedidoDTO dto = dtoDeTeste();

        when(pedidoService.atualizarCodigoRastreamento(3L, "XX000")).thenReturn(pedido);
        when(pedidoService.converterParaDTO(pedido)).thenReturn(dto);

        ResponseEntity<PedidoDTO> resposta = controller.atualizarRastreamento(3L, "XX000", null);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(auditoriaService).registrarAdmin(eq("admin"), eq("PEDIDO"), anyString(), eq(3L));
    }
}
