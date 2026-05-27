package br.ufpb.dsc.mercado.controller;

import br.ufpb.dsc.mercado.domain.*;
import br.ufpb.dsc.mercado.dto.CategoriaDTO;
import br.ufpb.dsc.mercado.repository.PedidoRepository;
import br.ufpb.dsc.mercado.service.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final String HEADER_HTMX = "HX-Request";

    private final CategoriaService categoriaService;
    private final CupomService cupomService;
    private final UsuarioService usuarioService;
    private final PedidoService pedidoService;
    private final PedidoRepository pedidoRepository;

    public AdminController(CategoriaService categoriaService,
                           CupomService cupomService,
                           UsuarioService usuarioService,
                           PedidoService pedidoService,
                           PedidoRepository pedidoRepository) {
        this.categoriaService = categoriaService;
        this.cupomService = cupomService;
        this.usuarioService = usuarioService;
        this.pedidoService = pedidoService;
        this.pedidoRepository = pedidoRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        BigDecimal faturamentoTotal = pedidoRepository.findAll().stream()
                .map(p -> p.getTotalGeral())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        model.addAttribute("faturamentoTotal", faturamentoTotal);
        model.addAttribute("totalPedidos", pedidoRepository.count());
        model.addAttribute("totalClientes", usuarioService.listarTodosPorPapel(Papel.CLIENTE).size());
        return "admin/dashboard";
    }

    // === CATEGORIAS ===
    @GetMapping("/categorias")
    public String listarCategorias(
            @RequestParam(name = "pagina", defaultValue = "0") int pagina,
            @RequestHeader(value = HEADER_HTMX, required = false) String htmx,
            Model model) {
        
        PageRequest pageRequest = PageRequest.of(pagina, 10, Sort.by("nome").ascending());
        Page<Categoria> categorias = categoriaService.listar(pageRequest);
        
        model.addAttribute("categorias", categorias);
        model.addAttribute("paginaAtual", pagina);

        if (htmx != null) {
            return "admin/fragments/tabela_categorias :: tabela";
        }
        return "admin/categorias";
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
            Model model) {
        
        if (bindingResult.hasErrors()) {
            model.addAttribute("categoria", null);
            return "admin/fragments/form_categoria :: modal";
        }
        try {
            Categoria c = categoriaService.criar(form);
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
            Model model) {
        
        if (bindingResult.hasErrors()) {
            Categoria c = categoriaService.buscarPorId(id);
            model.addAttribute("categoria", c);
            return "admin/fragments/form_categoria :: modal";
        }
        try {
            Categoria c = categoriaService.atualizar(id, form);
            model.addAttribute("categoria", c);
            return "admin/fragments/linha_categoria :: linha";
        } catch (IllegalArgumentException e) {
            bindingResult.rejectValue("nome", "error.form", e.getMessage());
            Categoria c = categoriaService.buscarPorId(id);
            model.addAttribute("categoria", c);
            return "admin/fragments/form_categoria :: modal";
        }
    }

    @DeleteMapping("/categorias/{id}")
    @ResponseBody
    public ResponseEntity<Void> excluirCategoria(@PathVariable Long id) {
        try {
            categoriaService.excluir(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // === CUPONS ===
    @GetMapping("/cupons")
    public String listarCupons(
            @RequestParam(name = "pagina", defaultValue = "0") int pagina,
            @RequestHeader(value = HEADER_HTMX, required = false) String htmx,
            Model model) {

        PageRequest pageRequest = PageRequest.of(pagina, 10, Sort.by("codigo").ascending());
        Page<Cupom> cupons = cupomService.listar(pageRequest);

        model.addAttribute("cupons", cupons);
        model.addAttribute("paginaAtual", pagina);

        if (htmx != null) {
            return "admin/fragments/tabela_cupons :: tabela";
        }
        return "admin/cupons";
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
            Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("tipos", TipoCupom.values());
            return "admin/fragments/form_cupom :: modal";
        }
        try {
            Cupom c = cupomService.criar(cupom);
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
    public ResponseEntity<?> alternarCupom(@PathVariable Long id) {
        try {
            cupomService.alternarAtivo(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/cupons/{id}")
    @ResponseBody
    public ResponseEntity<Void> excluirCupom(@PathVariable Long id) {
        try {
            cupomService.excluir(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // === CLIENTES ===
    @GetMapping("/clientes")
    public String listarClientes(
            @RequestParam(name = "busca", required = false, defaultValue = "") String busca,
            @RequestParam(name = "pagina", defaultValue = "0") int pagina,
            @RequestHeader(value = HEADER_HTMX, required = false) String htmx,
            Model model) {

        PageRequest pageRequest = PageRequest.of(pagina, 10, Sort.by("nome").ascending());
        Page<Usuario> clientes = usuarioService.listarPorPapel(Papel.CLIENTE, busca, pageRequest);

        model.addAttribute("clientes", clientes);
        model.addAttribute("busca", busca);
        model.addAttribute("paginaAtual", pagina);

        if (htmx != null) {
            return "admin/fragments/tabela_clientes :: tabela";
        }
        return "admin/clientes";
    }

    @PostMapping("/clientes/{id}/bloquear")
    @ResponseBody
    public ResponseEntity<?> bloquearCliente(@PathVariable Long id) {
        try {
            usuarioService.alternarStatus(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // === PEDIDOS ===
    @GetMapping("/pedidos")
    public String listarPedidos(
            @RequestParam(name = "pagina", defaultValue = "0") int pagina,
            @RequestHeader(value = HEADER_HTMX, required = false) String htmx,
            Model model) {

        PageRequest pageRequest = PageRequest.of(pagina, 10);
        Page<Pedido> pedidos = pedidoService.listarTodos(pageRequest);

        model.addAttribute("pedidos", pedidos);
        model.addAttribute("paginaAtual", pagina);

        if (htmx != null) {
            return "admin/fragments/tabela_pedidos :: tabela";
        }
        return "admin/pedidos";
    }

    @GetMapping("/pedidos/{id}")
    public String obterPedido(@PathVariable Long id, Model model) {
        Pedido pedido = pedidoService.buscarPorId(id);
        model.addAttribute("pedido", pedido);
        model.addAttribute("statusList", StatusPedido.values());
        return "admin/fragments/modal_pedido :: modal";
    }

    @PostMapping("/pedidos/{id}/status")
    public String atualizarStatusPedido(
            @PathVariable Long id,
            @RequestParam StatusPedido status,
            Model model) {
        
        Pedido pedido = pedidoService.atualizarStatus(id, status);
        model.addAttribute("pedido", pedido);
        // Retorna a linha da tabela de pedidos atualizada
        return "admin/fragments/linha_pedido :: linha";
    }

    @PostMapping("/pedidos/{id}/rastreamento")
    public String atualizarRastreamento(
            @PathVariable Long id,
            @RequestParam String codigoRastreamento,
            Model model) {
        
        Pedido pedido = pedidoService.atualizarCodigoRastreamento(id, codigoRastreamento);
        model.addAttribute("pedido", pedido);
        return "admin/fragments/linha_pedido :: linha";
    }
}
