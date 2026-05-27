package br.ufpb.dsc.mercado.controller;

import br.ufpb.dsc.mercado.domain.Papel;
import br.ufpb.dsc.mercado.domain.Usuario;
import br.ufpb.dsc.mercado.dto.CadastroRequest;
import br.ufpb.dsc.mercado.repository.PedidoRepository;
import br.ufpb.dsc.mercado.repository.ProdutoRepository;
import br.ufpb.dsc.mercado.service.UsuarioService;
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
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/sysadmin")
public class SysAdminController {

    private static final String HEADER_HTMX = "HX-Request";
    private final UsuarioService usuarioService;
    private final ProdutoRepository produtoRepository;
    private final PedidoRepository pedidoRepository;

    public SysAdminController(UsuarioService usuarioService,
                              ProdutoRepository produtoRepository,
                              PedidoRepository pedidoRepository) {
        this.usuarioService = usuarioService;
        this.produtoRepository = produtoRepository;
        this.pedidoRepository = pedidoRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("totalProdutos", produtoRepository.count());
        model.addAttribute("totalPedidos", pedidoRepository.count());
        model.addAttribute("totalClientes", usuarioService.listarTodosPorPapel(Papel.CLIENTE).size());
        model.addAttribute("totalAdmins", usuarioService.listarTodosPorPapel(Papel.ADMIN).size());

        BigDecimal faturamentoTotal = pedidoRepository.findAll().stream()
                .map(p -> p.getTotalGeral())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        model.addAttribute("faturamentoTotal", faturamentoTotal);

        return "sysadmin/dashboard";
    }

    @GetMapping("/admins")
    public String listarAdmins(
            @RequestParam(name = "busca", required = false, defaultValue = "") String busca,
            @RequestParam(name = "pagina", defaultValue = "0") int pagina,
            @RequestHeader(value = HEADER_HTMX, required = false) String htmx,
            Model model) {

        PageRequest pageRequest = PageRequest.of(pagina, 10, Sort.by("nome").ascending());
        Page<Usuario> admins = usuarioService.listarPorPapel(Papel.ADMIN, busca, pageRequest);

        model.addAttribute("admins", admins);
        model.addAttribute("busca", busca);
        model.addAttribute("paginaAtual", pagina);

        if (htmx != null) {
            return "sysadmin/fragments/tabela_admins :: tabela";
        }
        return "sysadmin/admins";
    }

    @GetMapping("/admins/novo")
    public String novoAdminForm(Model model) {
        model.addAttribute("form", new CadastroRequest(null, null, null, Papel.ADMIN));
        return "sysadmin/fragments/form_admin :: modal";
    }

    @PostMapping("/admins")
    public String criarAdmin(
            @Valid @ModelAttribute("form") CadastroRequest form,
            BindingResult bindingResult,
            Model model) {

        if (bindingResult.hasErrors()) {
            return "sysadmin/fragments/form_admin :: modal";
        }

        try {
            CadastroRequest requestAdmin = new CadastroRequest(
                    form.nome(),
                    form.email(),
                    form.senha(),
                    Papel.ADMIN
            );
            Usuario novoAdmin = usuarioService.cadastrar(requestAdmin);
            model.addAttribute("admin", novoAdmin);
            return "sysadmin/fragments/linha_admin :: linha";
        } catch (IllegalArgumentException e) {
            bindingResult.rejectValue("email", "error.form", e.getMessage());
            return "sysadmin/fragments/form_admin :: modal";
        }
    }

    @PostMapping("/admins/{id}/bloquear")
    @ResponseBody
    public ResponseEntity<?> alternarStatus(@PathVariable Long id) {
        try {
            usuarioService.alternarStatus(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/logs")
    public String visualisarLogs(Model model) {
        List<String> logs = new ArrayList<>();
        logs.add("[2026-05-26 20:20:15] SYSADMIN: Criou novo Administrador (admin2@mercado.com)");
        logs.add("[2026-05-26 19:45:10] SYSTEM: Backup do banco de dados executado com sucesso");
        logs.add("[2026-05-26 18:30:22] ADMIN (admin@mercado.com): Cadastrou novo produto 'Smartphone Galaxy S24'");
        logs.add("[2026-05-26 17:15:02] SECURITY: Tentativa de login suspeita bloqueada para o IP 192.168.10.45");
        logs.add("[2026-05-26 15:10:55] SYSADMIN: Alterou configurações de e-mail SMTP");
        model.addAttribute("logs", logs);
        return "sysadmin/logs";
    }

    @GetMapping("/configuracoes")
    public String configuracoes() {
        return "sysadmin/configuracoes";
    }
}
