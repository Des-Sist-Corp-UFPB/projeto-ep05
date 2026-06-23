package br.ufpb.dsc.mercado.controller;

import br.ufpb.dsc.mercado.audit.AuditoriaService;
import br.ufpb.dsc.mercado.audit.LogAuditoria;
import br.ufpb.dsc.mercado.domain.Papel;
import br.ufpb.dsc.mercado.domain.Usuario;
import br.ufpb.dsc.mercado.dto.CadastroRequest;
import br.ufpb.dsc.mercado.repository.PedidoRepository;
import br.ufpb.dsc.mercado.repository.ProdutoRepository;
import br.ufpb.dsc.mercado.service.PedidoService;
import br.ufpb.dsc.mercado.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/sysadmin")
public class SysAdminController {

    private static final String HEADER_HTMX = "HX-Request";

    private final UsuarioService usuarioService;
    private final ProdutoRepository produtoRepository;
    private final PedidoRepository pedidoRepository;
    private final PedidoService pedidoService;
    private final AuditoriaService auditoriaService;

    @SuppressWarnings("EI_EXPOSE_REP2")
    public SysAdminController(UsuarioService usuarioService,
                              ProdutoRepository produtoRepository,
                              PedidoRepository pedidoRepository,
                              PedidoService pedidoService,
                              AuditoriaService auditoriaService) {
        this.usuarioService = usuarioService;
        this.produtoRepository = produtoRepository;
        this.pedidoRepository = pedidoRepository;
        this.pedidoService = pedidoService;
        this.auditoriaService = auditoriaService;
    }

    // ── Dashboard ─────────────────────────────────────────────────────────────

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("totalProdutos", produtoRepository.count());
        model.addAttribute("totalPedidos", pedidoRepository.count());
        model.addAttribute("totalClientes", usuarioService.listarTodosPorPapel(Papel.CLIENTE).size());
        model.addAttribute("totalAdmins", usuarioService.listarTodosPorPapel(Papel.ADMIN).size());
        model.addAttribute("faturamentoTotal", pedidoService.calcularFaturamentoTotal());
        return "sysadmin/dashboard";
    }

    // ── Gerenciar Admins ──────────────────────────────────────────────────────

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
        model.addAttribute("form", new CadastroRequest("", "", "", Papel.ADMIN));
        model.addAttribute("adminId", null);
        return "sysadmin/fragments/form_admin :: modal";
    }

    @GetMapping("/admins/{id}/editar")
    public String editarAdminForm(@PathVariable Long id, Model model) {
        Usuario admin = usuarioService.buscarPorId(id);
        model.addAttribute("form", new CadastroRequest(admin.getNome(), admin.getEmail(), "", Papel.ADMIN));
        model.addAttribute("adminId", id);
        return "sysadmin/fragments/form_admin :: modal";
    }

    @PostMapping("/admins")
    public String criarAdmin(
            @Valid @ModelAttribute("form") CadastroRequest form,
            BindingResult bindingResult,
            Authentication auth,
            Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("adminId", null);
            return "sysadmin/fragments/form_admin :: modal";
        }

        try {
            CadastroRequest requestAdmin = new CadastroRequest(
                    form.nome(), form.email(), form.senha(), Papel.ADMIN);
            Usuario novoAdmin = usuarioService.cadastrar(requestAdmin);

            auditoriaService.registrarSysAdmin(
                    atorEmail(auth),
                    "USER_MGMT",
                    "Criou novo Administrador: " + novoAdmin.getEmail(),
                    novoAdmin.getId());

            Page<Usuario> admins = usuarioService.listarPorPapel(
                    Papel.ADMIN, "", PageRequest.of(0, 10, Sort.by("nome").ascending()));
            model.addAttribute("admins", admins);
            model.addAttribute("busca", "");
            return "sysadmin/fragments/tabela_admins :: tabela";

        } catch (IllegalArgumentException e) {
            bindingResult.rejectValue("email", "error.form", e.getMessage());
            model.addAttribute("adminId", null);
            return "sysadmin/fragments/form_admin :: modal";
        }
    }

    @PutMapping("/admins/{id}")
    public String editarAdmin(
            @PathVariable Long id,
            @Valid @ModelAttribute("form") CadastroRequest form,
            BindingResult bindingResult,
            Authentication auth,
            Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("adminId", id);
            return "sysadmin/fragments/form_admin :: modal";
        }

        try {
            String novaSenha = (form.senha() != null && !form.senha().isBlank())
                    ? form.senha() : null;
            usuarioService.atualizarPerfil(id, form.nome(), form.email(), null, novaSenha);

            auditoriaService.registrarSysAdmin(
                    atorEmail(auth),
                    "USER_MGMT",
                    "Editou Administrador ID " + id + " → " + form.email(),
                    id);

            Page<Usuario> admins = usuarioService.listarPorPapel(
                    Papel.ADMIN, "", PageRequest.of(0, 10, Sort.by("nome").ascending()));
            model.addAttribute("admins", admins);
            model.addAttribute("busca", "");
            return "sysadmin/fragments/tabela_admins :: tabela";

        } catch (IllegalArgumentException e) {
            bindingResult.rejectValue("email", "error.form", e.getMessage());
            model.addAttribute("adminId", id);
            return "sysadmin/fragments/form_admin :: modal";
        }
    }

    @PostMapping("/admins/{id}/bloquear")
    public String alternarStatus(@PathVariable Long id, Authentication auth, Model model) {
        Usuario alvo = usuarioService.buscarPorId(id);
        usuarioService.alternarStatus(id);

        String novoStatus = alvo.getStatus().name().equals("ATIVO") ? "BLOQUEADO" : "ATIVO";
        auditoriaService.registrarSysAdmin(
                atorEmail(auth),
                "USER_MGMT",
                "Alterou status do Administrador " + alvo.getEmail() + " → " + novoStatus,
                id);

        Page<Usuario> admins = usuarioService.listarPorPapel(
                Papel.ADMIN, "", PageRequest.of(0, 10, Sort.by("nome").ascending()));
        model.addAttribute("admins", admins);
        model.addAttribute("busca", "");
        return "sysadmin/fragments/tabela_admins :: tabela";
    }

    @DeleteMapping("/admins/{id}")
    public String excluirAdmin(@PathVariable Long id, Authentication auth, Model model) {
        Usuario alvo = usuarioService.buscarPorId(id);
        String email = alvo.getEmail();
        usuarioService.excluirConta(id);
        auditoriaService.registrarSysAdmin(
                atorEmail(auth),
                "USER_MGMT",
                "Excluiu Administrador: " + email,
                id);
        Page<Usuario> admins = usuarioService.listarPorPapel(
                Papel.ADMIN, "", PageRequest.of(0, 10, Sort.by("nome").ascending()));
        model.addAttribute("admins", admins);
        model.addAttribute("busca", "");
        return "sysadmin/fragments/tabela_admins :: tabela";
    }

    // ── Logs ──────────────────────────────────────────────────────────────────

    @GetMapping("/logs")
    public String visualizarLogs(
            @RequestParam(required = false) String papel,
            @RequestParam(required = false) String ator,
            @RequestParam(required = false) Integer dias,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestHeader(value = HEADER_HTMX, required = false) String htmx,
            Model model) {

        PageRequest pr = PageRequest.of(pagina, 25);
        Page<LogAuditoria> logs = auditoriaService.listar(papel, ator, dias, pr);

        model.addAttribute("logs", logs);
        model.addAttribute("filtroPapel", papel);
        model.addAttribute("filtroAtor", ator);
        model.addAttribute("filtroDias", dias);
        model.addAttribute("paginaAtual", pagina);

        if (htmx != null) {
            return "sysadmin/fragments/tabela_logs :: tabela";
        }
        return "sysadmin/logs";
    }

    @GetMapping("/configuracoes")
    public String configuracoes() {
        return "sysadmin/configuracoes";
    }

    // ── Util ──────────────────────────────────────────────────────────────────

    private String atorEmail(Authentication auth) {
        return (auth != null) ? auth.getName() : "desconhecido";
    }
}
