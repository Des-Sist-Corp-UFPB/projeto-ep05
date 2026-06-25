package br.ufpb.dsc.mercado.controller;

import br.ufpb.dsc.mercado.audit.AuditoriaService;
import br.ufpb.dsc.mercado.domain.*;
import br.ufpb.dsc.mercado.dto.CategoriaDTO;
import br.ufpb.dsc.mercado.repository.PedidoRepository;
import br.ufpb.dsc.mercado.service.*;
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
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminController — Testes Unitários")
class AdminControllerTest {

    @Mock CategoriaService categoriaService;
    @Mock CupomService cupomService;
    @Mock UsuarioService usuarioService;
    @Mock PedidoService pedidoService;
    @Mock PedidoRepository pedidoRepository;
    @Mock AuditoriaService auditoriaService;
    @Mock Model model;
    @Mock BindingResult bindingResult;
    @Mock Authentication auth;

    private AdminController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminController(
                categoriaService, cupomService, usuarioService,
                pedidoService, pedidoRepository, auditoriaService);
        lenient().when(auth.getName()).thenReturn("admin@teste.com");
    }

    // ── Navegação ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("raiz deve redirecionar para /admin/dashboard")
    void raiz_deveRedirecionarParaDashboard() {
        assertThat(controller.raiz()).isEqualTo("redirect:/admin/dashboard");
    }

    @Test
    @DisplayName("dashboard deve popular faturamento, pedidos e clientes no model")
    void dashboard_devePopularModel() {
        when(pedidoService.calcularFaturamentoTotal()).thenReturn(BigDecimal.valueOf(1500));
        when(pedidoRepository.count()).thenReturn(10L);
        when(usuarioService.listarTodosPorPapel(Papel.CLIENTE)).thenReturn(List.of(new Usuario()));

        String view = controller.dashboard(model);

        assertThat(view).isEqualTo("admin/dashboard");
        verify(model).addAttribute("faturamentoTotal", BigDecimal.valueOf(1500));
        verify(model).addAttribute("totalPedidos", 10L);
        verify(model).addAttribute("totalClientes", 1);
    }

    // ── Categorias ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("listarCategorias sem HTMX deve retornar view completa")
    void listarCategorias_semHtmx_deveRetornarViewCompleta() {
        Page<Categoria> page = Page.empty();
        when(categoriaService.listar(any())).thenReturn(page);

        String view = controller.listarCategorias(0, null, model);

        assertThat(view).isEqualTo("admin/categorias");
    }

    @Test
    @DisplayName("listarCategorias com HTMX deve retornar fragmento")
    void listarCategorias_comHtmx_deveRetornarFragmento() {
        when(categoriaService.listar(any())).thenReturn(Page.empty());

        String view = controller.listarCategorias(0, "true", model);

        assertThat(view).isEqualTo("admin/fragments/tabela_categorias :: tabela");
    }

    @Test
    @DisplayName("criarCategoria com erros de validacao deve retornar formulario")
    void criarCategoria_comErros_deveRetornarFormulario() {
        when(bindingResult.hasErrors()).thenReturn(true);
        CategoriaDTO dto = new CategoriaDTO(null, "", null);

        String view = controller.criarCategoria(dto, bindingResult, auth, model);

        assertThat(view).isEqualTo("admin/fragments/form_categoria :: modal");
        verify(categoriaService, never()).criar(any());
    }

    @Test
    @DisplayName("criarCategoria com sucesso deve retornar linha e registrar auditoria")
    void criarCategoria_comSucesso_deveRetornarLinhaERegistrarAuditoria() {
        when(bindingResult.hasErrors()).thenReturn(false);
        Categoria nova = new Categoria("Brownies", "desc");
        nova.setId(1L);
        when(categoriaService.criar(any())).thenReturn(nova);

        CategoriaDTO dto = new CategoriaDTO(null, "Brownies", "desc");
        String view = controller.criarCategoria(dto, bindingResult, auth, model);

        assertThat(view).isEqualTo("admin/fragments/linha_categoria :: linha");
        verify(auditoriaService).registrarAdmin(eq("admin@teste.com"), eq("PRODUTO"), anyString(), eq(1L));
    }

    @Test
    @DisplayName("criarCategoria com nome duplicado deve rejeitar campo e retornar formulario")
    void criarCategoria_nomeDuplicado_deveRejeitarCampo() {
        when(bindingResult.hasErrors()).thenReturn(false);
        when(categoriaService.criar(any())).thenThrow(new IllegalArgumentException("Ja existe"));

        CategoriaDTO dto = new CategoriaDTO(null, "Bolos", null);
        String view = controller.criarCategoria(dto, bindingResult, auth, model);

        assertThat(view).isEqualTo("admin/fragments/form_categoria :: modal");
        verify(bindingResult).rejectValue(eq("nome"), anyString(), contains("Ja existe"));
    }

    @Test
    @DisplayName("excluirCategoria com sucesso deve retornar 200 e registrar auditoria")
    void excluirCategoria_comSucesso_deveRetornar200() {
        doNothing().when(categoriaService).excluir(1L);

        ResponseEntity<Void> resp = controller.excluirCategoria(1L, auth);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(auditoriaService).registrarAdmin(eq("admin@teste.com"), eq("PRODUTO"), anyString(), eq(1L));
    }

    @Test
    @DisplayName("excluirCategoria inexistente deve retornar 404")
    void excluirCategoria_inexistente_deveRetornar404() {
        doThrow(new IllegalArgumentException("nao encontrada")).when(categoriaService).excluir(99L);

        ResponseEntity<Void> resp = controller.excluirCategoria(99L, auth);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── Cupons ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("alternarCupom com sucesso deve retornar 200 e registrar auditoria")
    void alternarCupom_comSucesso_deveRetornar200() {
        doNothing().when(cupomService).alternarAtivo(1L);

        ResponseEntity<?> resp = controller.alternarCupom(1L, auth);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(auditoriaService).registrarAdmin(eq("admin@teste.com"), eq("PRODUTO"), anyString(), eq(1L));
    }

    @Test
    @DisplayName("excluirCupom com sucesso deve retornar 200 e registrar auditoria")
    void excluirCupom_comSucesso_deveRetornar200() {
        doNothing().when(cupomService).excluir(2L);

        ResponseEntity<Void> resp = controller.excluirCupom(2L, auth);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(auditoriaService).registrarAdmin(eq("admin@teste.com"), eq("PRODUTO"), anyString(), eq(2L));
    }

    // ── Clientes ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("bloquearCliente com sucesso deve retornar 200 e registrar auditoria")
    void bloquearCliente_comSucesso_deveRetornar200() {
        Usuario alvo = new Usuario();
        alvo.setEmail("cliente@teste.com");
        when(usuarioService.buscarPorId(1L)).thenReturn(alvo);
        doNothing().when(usuarioService).alternarStatus(1L);

        ResponseEntity<?> resp = controller.bloquearCliente(1L, auth);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(auditoriaService).registrarAdmin(eq("admin@teste.com"), eq("USER_MGMT"), anyString(), eq(1L));
    }

    @Test
    @DisplayName("bloquearCliente com usuario nao encontrado deve retornar 400")
    void bloquearCliente_naoEncontrado_deveRetornar400() {
        when(usuarioService.buscarPorId(99L)).thenThrow(new IllegalArgumentException("nao encontrado"));

        ResponseEntity<?> resp = controller.bloquearCliente(99L, auth);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ── Pedidos ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("atualizarStatusPedido deve retornar fragmento e registrar auditoria")
    void atualizarStatusPedido_deveRetornarFragmentoEAuditoria() {
        Pedido pedido = new Pedido();
        when(pedidoService.atualizarStatus(1L, StatusPedido.ENVIADO)).thenReturn(pedido);

        String view = controller.atualizarStatusPedido(1L, StatusPedido.ENVIADO, auth, model);

        assertThat(view).isEqualTo("admin/fragments/linha_pedido :: linha");
        verify(model).addAttribute("pedido", pedido);
        verify(auditoriaService).registrarAdmin(eq("admin@teste.com"), eq("PEDIDO"), anyString(), eq(1L));
    }

    @Test
    @DisplayName("atualizarRastreamento deve retornar fragmento e registrar auditoria")
    void atualizarRastreamento_deveRetornarFragmentoEAuditoria() {
        Pedido pedido = new Pedido();
        when(pedidoService.atualizarCodigoRastreamento(1L, "BR123")).thenReturn(pedido);

        String view = controller.atualizarRastreamento(1L, "BR123", auth, model);

        assertThat(view).isEqualTo("admin/fragments/linha_pedido :: linha");
        verify(auditoriaService).registrarAdmin(eq("admin@teste.com"), eq("PEDIDO"), contains("BR123"), eq(1L));
    }
}
