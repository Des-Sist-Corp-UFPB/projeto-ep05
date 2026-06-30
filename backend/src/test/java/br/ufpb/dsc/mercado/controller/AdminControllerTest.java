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
import org.mockito.ArgumentCaptor;
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
    @DisplayName("dashboard deve popular faturamento, pedidos, cancelados e clientes no model")
    void dashboard_devePopularModel() {
        when(pedidoService.calcularFaturamentoTotal()).thenReturn(BigDecimal.valueOf(1500));
        when(pedidoRepository.count()).thenReturn(10L);
        when(pedidoService.contarPorStatus(StatusPedido.CANCELADO)).thenReturn(2L);
        when(usuarioService.listarTodosPorPapel(Papel.CLIENTE)).thenReturn(List.of(new Usuario()));

        String view = controller.dashboard(model);

        assertThat(view).isEqualTo("admin/dashboard");
        verify(model).addAttribute("faturamentoTotal", BigDecimal.valueOf(1500));
        verify(model).addAttribute("totalPedidos", 10L);
        verify(model).addAttribute("totalPedidosCancelados", 2L);
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
    @DisplayName("novaCategoriaForm deve popular form vazio e categoria nula")
    void novaCategoriaForm_devePopularFormVazio() {
        String view = controller.novaCategoriaForm(model);

        assertThat(view).isEqualTo("admin/fragments/form_categoria :: modal");
        verify(model).addAttribute(eq("form"), any(CategoriaDTO.class));
        verify(model).addAttribute("categoria", null);
    }

    @Test
    @DisplayName("editarCategoriaForm deve popular form preenchido com a categoria existente")
    void editarCategoriaForm_devePopularFormPreenchido() {
        Categoria c = new Categoria("Bolos", "Categoria de bolos");
        c.setId(5L);
        when(categoriaService.buscarPorId(5L)).thenReturn(c);

        String view = controller.editarCategoriaForm(5L, model);

        assertThat(view).isEqualTo("admin/fragments/form_categoria :: modal");
        verify(model).addAttribute("categoria", c);

        ArgumentCaptor<CategoriaDTO> captor = ArgumentCaptor.forClass(CategoriaDTO.class);
        verify(model).addAttribute(eq("form"), captor.capture());
        assertThat(captor.getValue().nome()).isEqualTo("Bolos");
    }

    @Test
    @DisplayName("atualizarCategoria com erros de validacao deve retornar formulario com categoria atual")
    void atualizarCategoria_comErros_deveRetornarFormulario() {
        when(bindingResult.hasErrors()).thenReturn(true);
        Categoria atual = new Categoria("Bolos", "desc");
        atual.setId(5L);
        when(categoriaService.buscarPorId(5L)).thenReturn(atual);

        CategoriaDTO dto = new CategoriaDTO(5L, "", null);
        String view = controller.atualizarCategoria(5L, dto, bindingResult, auth, model);

        assertThat(view).isEqualTo("admin/fragments/form_categoria :: modal");
        verify(model).addAttribute("categoria", atual);
        verify(categoriaService, never()).atualizar(any(), any());
    }

    @Test
    @DisplayName("atualizarCategoria com sucesso deve retornar linha e registrar auditoria")
    void atualizarCategoria_comSucesso_deveRetornarLinhaERegistrarAuditoria() {
        when(bindingResult.hasErrors()).thenReturn(false);
        Categoria atualizada = new Categoria("Tortas", "desc");
        atualizada.setId(5L);
        when(categoriaService.atualizar(eq(5L), any())).thenReturn(atualizada);

        CategoriaDTO dto = new CategoriaDTO(5L, "Tortas", "desc");
        String view = controller.atualizarCategoria(5L, dto, bindingResult, auth, model);

        assertThat(view).isEqualTo("admin/fragments/linha_categoria :: linha");
        verify(model).addAttribute("categoria", atualizada);
        verify(auditoriaService).registrarAdmin(eq("admin@teste.com"), eq("PRODUTO"), anyString(), eq(5L));
    }

    @Test
    @DisplayName("atualizarCategoria com nome duplicado deve rejeitar campo e retornar formulario")
    void atualizarCategoria_nomeDuplicado_deveRejeitarCampo() {
        when(bindingResult.hasErrors()).thenReturn(false);
        when(categoriaService.atualizar(eq(5L), any())).thenThrow(new IllegalArgumentException("Ja existe"));
        Categoria atual = new Categoria("Bolos", "desc");
        atual.setId(5L);
        when(categoriaService.buscarPorId(5L)).thenReturn(atual);

        CategoriaDTO dto = new CategoriaDTO(5L, "Tortas", "desc");
        String view = controller.atualizarCategoria(5L, dto, bindingResult, auth, model);

        assertThat(view).isEqualTo("admin/fragments/form_categoria :: modal");
        verify(bindingResult).rejectValue(eq("nome"), anyString(), contains("Ja existe"));
        verify(model).addAttribute("categoria", atual);
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
    @DisplayName("listarCupons sem HTMX deve retornar view completa")
    void listarCupons_semHtmx_deveRetornarViewCompleta() {
        when(cupomService.listar(any())).thenReturn(Page.empty());

        String view = controller.listarCupons(0, null, model);

        assertThat(view).isEqualTo("admin/cupons");
        verify(model).addAttribute(eq("cupons"), any());
    }

    @Test
    @DisplayName("listarCupons com HTMX deve retornar fragmento")
    void listarCupons_comHtmx_deveRetornarFragmento() {
        when(cupomService.listar(any())).thenReturn(Page.empty());

        String view = controller.listarCupons(0, "true", model);

        assertThat(view).isEqualTo("admin/fragments/tabela_cupons :: tabela");
    }

    @Test
    @DisplayName("novoCupomForm deve popular cupom vazio e os tipos disponiveis")
    void novoCupomForm_devePopularModel() {
        String view = controller.novoCupomForm(model);

        assertThat(view).isEqualTo("admin/fragments/form_cupom :: modal");
        verify(model).addAttribute(eq("cupom"), any(Cupom.class));
        verify(model).addAttribute(eq("tipos"), eq(TipoCupom.values()));
    }

    @Test
    @DisplayName("criarCupom com erros de validacao deve retornar formulario")
    void criarCupom_comErros_deveRetornarFormulario() {
        when(bindingResult.hasErrors()).thenReturn(true);
        Cupom cupom = new Cupom();

        String view = controller.criarCupom(cupom, bindingResult, auth, model);

        assertThat(view).isEqualTo("admin/fragments/form_cupom :: modal");
        verify(cupomService, never()).criar(any());
    }

    @Test
    @DisplayName("criarCupom com sucesso deve retornar linha e registrar auditoria")
    void criarCupom_comSucesso_deveRetornarLinhaERegistrarAuditoria() {
        when(bindingResult.hasErrors()).thenReturn(false);
        Cupom criado = new Cupom();
        criado.setId(3L);
        criado.setCodigo("PROMO10");
        when(cupomService.criar(any())).thenReturn(criado);

        String view = controller.criarCupom(new Cupom(), bindingResult, auth, model);

        assertThat(view).isEqualTo("admin/fragments/linha_cupom :: linha");
        verify(model).addAttribute("cupom", criado);
        verify(auditoriaService).registrarAdmin(eq("admin@teste.com"), eq("PRODUTO"), contains("PROMO10"), eq(3L));
    }

    @Test
    @DisplayName("criarCupom com codigo duplicado deve rejeitar campo e retornar formulario")
    void criarCupom_codigoDuplicado_deveRejeitarCampo() {
        when(bindingResult.hasErrors()).thenReturn(false);
        when(cupomService.criar(any())).thenThrow(new IllegalArgumentException("Codigo ja existe"));

        String view = controller.criarCupom(new Cupom(), bindingResult, auth, model);

        assertThat(view).isEqualTo("admin/fragments/form_cupom :: modal");
        verify(bindingResult).rejectValue(eq("codigo"), anyString(), contains("Codigo ja existe"));
    }

    @Test
    @DisplayName("alternarCupom com sucesso deve retornar 200 e registrar auditoria")
    void alternarCupom_comSucesso_deveRetornar200() {
        doNothing().when(cupomService).alternarAtivo(1L);

        ResponseEntity<?> resp = controller.alternarCupom(1L, auth);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(auditoriaService).registrarAdmin(eq("admin@teste.com"), eq("PRODUTO"), anyString(), eq(1L));
    }

    @Test
    @DisplayName("alternarCupom inexistente deve retornar 400")
    void alternarCupom_inexistente_deveRetornar400() {
        doThrow(new IllegalArgumentException("nao encontrado")).when(cupomService).alternarAtivo(99L);

        ResponseEntity<?> resp = controller.alternarCupom(99L, auth);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).isEqualTo("nao encontrado");
    }

    @Test
    @DisplayName("excluirCupom com sucesso deve retornar 200 e registrar auditoria")
    void excluirCupom_comSucesso_deveRetornar200() {
        doNothing().when(cupomService).excluir(2L);

        ResponseEntity<Void> resp = controller.excluirCupom(2L, auth);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(auditoriaService).registrarAdmin(eq("admin@teste.com"), eq("PRODUTO"), anyString(), eq(2L));
    }

    @Test
    @DisplayName("excluirCupom inexistente deve retornar 404")
    void excluirCupom_inexistente_deveRetornar404() {
        doThrow(new IllegalArgumentException("nao encontrado")).when(cupomService).excluir(99L);

        ResponseEntity<Void> resp = controller.excluirCupom(99L, auth);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── Clientes ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("listarClientes sem HTMX deve retornar view completa")
    void listarClientes_semHtmx_deveRetornarViewCompleta() {
        when(usuarioService.listarPorPapel(eq(Papel.CLIENTE), eq(""), any())).thenReturn(Page.empty());

        String view = controller.listarClientes("", 0, null, model);

        assertThat(view).isEqualTo("admin/clientes");
        verify(model).addAttribute("busca", "");
    }

    @Test
    @DisplayName("listarClientes com HTMX e busca deve retornar fragmento filtrado")
    void listarClientes_comHtmxEBusca_deveRetornarFragmento() {
        when(usuarioService.listarPorPapel(eq(Papel.CLIENTE), eq("Ana"), any())).thenReturn(Page.empty());

        String view = controller.listarClientes("Ana", 0, "true", model);

        assertThat(view).isEqualTo("admin/fragments/tabela_clientes :: tabela");
        verify(model).addAttribute("busca", "Ana");
    }

    @Test
    @DisplayName("bloquearCliente com sucesso deve retornar 200 e registrar auditoria")
    void bloquearCliente_comSucesso_deveRetornar200() {
        Usuario alvo = new Usuario();
        alvo.setEmail("cliente@teste.com");
        when(usuarioService.buscarPorId(1L)).thenReturn(alvo);
        when(usuarioService.alternarStatus(1L)).thenReturn(StatusUsuario.BLOQUEADO);

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
    @DisplayName("listarPedidos sem HTMX e sem filtro de status deve retornar view completa")
    void listarPedidos_semHtmxSemStatus_deveRetornarViewCompleta() {
        when(pedidoService.listarTodos(isNull(), any())).thenReturn(Page.empty());

        String view = controller.listarPedidos(0, null, null, model);

        assertThat(view).isEqualTo("admin/pedidos");
        verify(model).addAttribute("statusSelecionado", (String) null);
        verify(pedidoService).listarTodos(isNull(), any());
    }

    @Test
    @DisplayName("listarPedidos com HTMX e status valido deve filtrar e retornar fragmento")
    void listarPedidos_comHtmxEStatusValido_deveFiltrarERetornarFragmento() {
        when(pedidoService.listarTodos(eq(StatusPedido.ENVIADO), any())).thenReturn(Page.empty());

        String view = controller.listarPedidos(0, "enviado", "true", model);

        assertThat(view).isEqualTo("admin/fragments/tabela_pedidos :: tabela");
        verify(pedidoService).listarTodos(eq(StatusPedido.ENVIADO), any());
    }

    @Test
    @DisplayName("listarPedidos com status invalido deve ignorar o filtro e listar todos")
    void listarPedidos_comStatusInvalido_deveIgnorarFiltro() {
        when(pedidoService.listarTodos(isNull(), any())).thenReturn(Page.empty());

        String view = controller.listarPedidos(0, "status-que-nao-existe", null, model);

        assertThat(view).isEqualTo("admin/pedidos");
        verify(pedidoService).listarTodos(isNull(), any());
    }

    @Test
    @DisplayName("listarPedidos com status em branco deve ignorar o filtro e listar todos")
    void listarPedidos_comStatusEmBranco_deveIgnorarFiltro() {
        when(pedidoService.listarTodos(isNull(), any())).thenReturn(Page.empty());

        String view = controller.listarPedidos(0, "   ", null, model);

        assertThat(view).isEqualTo("admin/pedidos");
        verify(pedidoService).listarTodos(isNull(), any());
    }

    @Test
    @DisplayName("obterPedido deve popular pedido e lista de status no model")
    void obterPedido_devePopularModel() {
        Pedido pedido = new Pedido();
        when(pedidoService.buscarPorId(1L)).thenReturn(pedido);

        String view = controller.obterPedido(1L, model);

        assertThat(view).isEqualTo("admin/fragments/modal_pedido :: modal");
        verify(model).addAttribute("pedido", pedido);
        verify(model).addAttribute(eq("statusList"), eq(StatusPedido.values()));
    }

    @Test
    @DisplayName("obterPedidoJson com pedido existente deve retornar 200 com o DTO")
    void obterPedidoJson_existente_deveRetornar200() {
        Pedido pedido = new Pedido();
        when(pedidoService.buscarPorId(1L)).thenReturn(pedido);
        when(pedidoService.converterParaDTO(pedido)).thenReturn(null);

        ResponseEntity<?> resp = controller.obterPedidoJson(1L);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("obterPedidoJson com pedido inexistente deve retornar 404")
    void obterPedidoJson_inexistente_deveRetornar404() {
        when(pedidoService.buscarPorId(99L)).thenThrow(new IllegalArgumentException("nao encontrado"));

        ResponseEntity<?> resp = controller.obterPedidoJson(99L);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

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
