package br.ufpb.dsc.mercado.controller;

import br.ufpb.dsc.mercado.audit.AuditoriaService;
import br.ufpb.dsc.mercado.domain.*;
import br.ufpb.dsc.mercado.dto.CategoriaDTO;
import br.ufpb.dsc.mercado.repository.PedidoRepository;
import br.ufpb.dsc.mercado.service.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final String HEADER_HTMX = "HX-Request";

    private final CategoriaService categoriaService;
    private final CupomService cupomService;
    private final UsuarioService usuarioService;
    private final PedidoService pedidoService;
    private final PedidoRepository pedidoRepository;
    private final AuditoriaService auditoriaService;

    @SuppressWarnings("EI_EXPOSE_REP2")
    public AdminController(CategoriaService categoriaService,
                           CupomService cupomService,
                           UsuarioService usuarioService,
                           PedidoService pedidoService,
                           PedidoRepository pedidoRepository,
                           AuditoriaService auditoriaService) {
        this.categoriaService = categoriaService;
        this.cupomService = cupomService;
        this.usuarioService = usuarioService;
        this.pedidoService = pedidoService;
        this.pedidoRepository = pedidoRepository;
        this.auditoriaService = auditoriaService;
    }

    @GetMapping({"", "/"})
    public String raiz() { return "redirect:/admin/dashboard"; }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("faturamentoTotal", pedidoService.calcularFaturamentoTotal());
        model.addAttribute("totalPedidos", pedidoRepository.count());
        model.addAttribute("totalClientes", usuarioService.listarTodosPorPapel(Papel.CLIENTE).size());
        return "admin/dashboard";
    }

    // === CATEGORIAS ===

    @GetMapping("/categorias")
    public String listarCategorias(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestHeader(value = HEADER_HTMX, required = false) String htmx,
            Model model) {
        PageRequest pr = PageRequest.of(pagina, 10, Sort.by("nome").ascending());
        model.addAttribute("categorias", categoriaService.listar(pr));
        model.addAttribute("paginaAtual", pagina);
        return htmx != null ? "admin/fragments/tabela_categorias :: tabela" : "admin/categorias";
    }

    @GetMapping("/categorias/nova")
    public String novaCategoriaForm(Model model) {
        model.addAttribute("form", new CategoriaDTO(null, null, null));
        model.addAttribute("categoria", null);
        return "admin/fragments/form_categoria :: modal";
    }

    @GetMapping("/categorias/{id}/editar")
    public String editarCategoriaForm(@PathVariable Long id, Model model) {
        Categoria c = categoriaService.buscarPorId(id);
        model.addAttribute("form", new CategoriaDTO(c.getId(), c.getNome(), c.getDescricao()));
        model.addAttribute("categoria", c);
        return "admin/fragments/form_categoria :: modal";
    }

    @PostMapping("/categorias")
    public String criarCategoria(
            @Valid @ModelAttribute("form") CategoriaDTO form,
            BindingResult bindingResult,
            Authentication auth,
            Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("categoria", null);
            return "admin/fragments/form_categoria :: modal";
        }
        try {
            Categoria c = categoriaService.criar(form);
            auditoriaService.registrarAdmin(atorEmail(auth), "PRODUTO",
                    "Criou categoria: " + c.getNome(), c.getId());
            model.addAttribute("categoria", c);
            return "admin/fragments/linha_categoria :: linha";
        } catch (IllegalArgumentException e) {
            bindingResult.rejectValue("nome", "error.form", e.getMessage());
            model.addAttribute("categoria", null);
            return "admin/fragments/form_categoria :: modal";
        }
    }

    @PutMapping("/categorias/{id}")
    public String atualizarCategoria(
            @PathVariable Long id,
            @Valid @ModelAttribute("form") CategoriaDTO form,
            BindingResult bindingResult,
            Authentication auth,
            Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("categoria", categoriaService.buscarPorId(id));
            return "admin/fragments/form_categoria :: modal";
        }
        try {
            Categoria c = categoriaService.atualizar(id, form);
            auditoriaService.registrarAdmin(atorEmail(auth), "PRODUTO",
                    "Atualizou categoria ID " + id + ": " + c.getNome(), id);
            model.addAttribute("categoria", c);
            return "admin/fragments/linha_categoria :: linha";
        } catch (IllegalArgumentException e) {
            bindingResult.rejectValue("nome", "error.form", e.getMessage());
            model.addAttribute("categoria", categoriaService.buscarPorId(id));
            return "admin/fragments/form_categoria :: modal";
        }
    }

    @DeleteMapping("/categorias/{id}")
    @ResponseBody
    public ResponseEntity<Void> excluirCategoria(@PathVariable Long id, Authentication auth) {
        try {
            categoriaService.excluir(id);
            auditoriaService.registrarAdmin(atorEmail(auth), "PRODUTO",
                    "Excluiu categoria ID " + id, id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // === CUPONS ===

    @GetMapping("/cupons")
    public String listarCupons(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestHeader(value = HEADER_HTMX, required = false) String htmx,
            Model model) {
        PageRequest pr = PageRequest.of(pagina, 10, Sort.by("codigo").ascending());
        model.addAttribute("cupons", cupomService.listar(pr));
        model.addAttribute("paginaAtual", pagina);
        return htmx != null ? "admin/fragments/tabela_cupons :: tabela" : "admin/cupons";
    }

    @GetMapping("/cupons/novo")
    public String novoCupomForm(Model model) {
        model.addAttribute("cupom", new Cupom());
        model.addAttribute("tipos", TipoCupom.values());
        return "admin/fragments/form_cupom :: modal";
    }

    @PostMapping("/cupons")
    public String criarCupom(
            @Valid @ModelAttribute("cupom") Cupom cupom,
            BindingResult bindingResult,
            Authentication auth,
            Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("tipos", TipoCupom.values());
            return "admin/fragments/form_cupom :: modal";
        }
        try {
            Cupom c = cupomService.criar(cupom);
            auditoriaService.registrarAdmin(atorEmail(auth), "PRODUTO",
                    "Criou cupom: " + c.getCodigo(), c.getId());
            model.addAttribute("cupom", c);
            return "admin/fragments/linha_cupom :: linha";
        } catch (IllegalArgumentException e) {
            bindingResult.rejectValue("codigo", "error.form", e.getMessage());
            model.addAttribute("tipos", TipoCupom.values());
            return "admin/fragments/form_cupom :: modal";
        }
    }

    @PostMapping("/cupons/{id}/alternar")
    @ResponseBody
    public ResponseEntity<?> alternarCupom(@PathVariable Long id, Authentication auth) {
        try {
            cupomService.alternarAtivo(id);
            auditoriaService.registrarAdmin(atorEmail(auth), "PRODUTO",
                    "Alternou status do cupom ID " + id, id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/cupons/{id}")
    @ResponseBody
    public ResponseEntity<Void> excluirCupom(@PathVariable Long id, Authentication auth) {
        try {
            cupomService.excluir(id);
            auditoriaService.registrarAdmin(atorEmail(auth), "PRODUTO",
                    "Excluiu cupom ID " + id, id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // === CLIENTES ===

    @GetMapping("/clientes")
    public String listarClientes(
            @RequestParam(required = false, defaultValue = "") String busca,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestHeader(value = HEADER_HTMX, required = false) String htmx,
            Model model) {
        PageRequest pr = PageRequest.of(pagina, 10, Sort.by("nome").ascending());
        model.addAttribute("clientes", usuarioService.listarPorPapel(Papel.CLIENTE, busca, pr));
        model.addAttribute("busca", busca);
        model.addAttribute("paginaAtual", pagina);
        return htmx != null ? "admin/fragments/tabela_clientes :: tabela" : "admin/clientes";
    }

    @PostMapping("/clientes/{id}/bloquear")
    @ResponseBody
    public ResponseEntity<?> bloquearCliente(@PathVariable Long id, Authentication auth) {
        try {
            Usuario alvo = usuarioService.buscarPorId(id);
            StatusUsuario novoStatus = usuarioService.alternarStatus(id);
            auditoriaService.registrarAdmin(atorEmail(auth), "USER_MGMT",
                    "Alterou status do cliente " + alvo.getEmail() + " → " + novoStatus.name(), id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // === PEDIDOS ===

    @GetMapping("/pedidos")
    public String listarPedidos(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestHeader(value = HEADER_HTMX, required = false) String htmx,
            Model model) {
        PageRequest pr = PageRequest.of(pagina, 10);
        model.addAttribute("pedidos", pedidoService.listarTodos(pr));
        model.addAttribute("paginaAtual", pagina);
        return htmx != null ? "admin/fragments/tabela_pedidos :: tabela" : "admin/pedidos";
    }

    @GetMapping("/pedidos/{id}")
    public String obterPedido(@PathVariable Long id, Model model) {
        model.addAttribute("pedido", pedidoService.buscarPorId(id));
        model.addAttribute("statusList", StatusPedido.values());
        return "admin/fragments/modal_pedido :: modal";
    }

    @PostMapping("/pedidos/{id}/status")
    public String atualizarStatusPedido(
            @PathVariable Long id,
            @RequestParam StatusPedido status,
            Authentication auth,
            Model model) {
        Pedido pedido = pedidoService.atualizarStatus(id, status);
        auditoriaService.registrarAdmin(atorEmail(auth), "PEDIDO",
                "Atualizou status do pedido #" + id + " → " + status, id);
        model.addAttribute("pedido", pedido);
        return "admin/fragments/linha_pedido :: linha";
    }

    @PostMapping("/pedidos/{id}/rastreamento")
    public String atualizarRastreamento(
            @PathVariable Long id,
            @RequestParam String codigoRastreamento,
            Authentication auth,
            Model model) {
        Pedido pedido = pedidoService.atualizarCodigoRastreamento(id, codigoRastreamento);
        auditoriaService.registrarAdmin(atorEmail(auth), "PEDIDO",
                "Adicionou rastreamento ao pedido #" + id + ": " + codigoRastreamento, id);
        model.addAttribute("pedido", pedido);
        return "admin/fragments/linha_pedido :: linha";
    }

    // ── Util ──────────────────────────────────────────────────────────────────

    private String atorEmail(Authentication auth) {
        return (auth != null) ? auth.getName() : "desconhecido";
    }
}
