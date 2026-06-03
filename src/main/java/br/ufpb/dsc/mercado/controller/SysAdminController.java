package br.ufpb.dsc.mercado.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import br.ufpb.dsc.mercado.domain.Papel;
import br.ufpb.dsc.mercado.domain.Usuario;
import br.ufpb.dsc.mercado.dto.CadastroRequest;
import br.ufpb.dsc.mercado.repository.PedidoRepository;
import br.ufpb.dsc.mercado.repository.ProdutoRepository;
import br.ufpb.dsc.mercado.service.UsuarioService;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/sysadmin")
public class SysAdminController {

    private static final String HEADER_HTMX = "HX-Request";
    private final UsuarioService usuarioService;
    private final ProdutoRepository produtoRepository;
    private final PedidoRepository pedidoRepository;

    @SuppressWarnings("EI_EXPOSE_REP2") // Beans Spring são singletons gerenciados pelo container
    public SysAdminController(UsuarioService usuarioService,
                              ProdutoRepository produtoRepository,
                              PedidoRepository pedidoRepository) {
        this.usuarioService = usuarioService;
        this.produtoRepository = produtoRepository;
        this.pedidoRepository = pedidoRepository;
    }

    // ── Dashboard ─────────────────────────────────────────────────────────────

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
            // Retorna só o fragmento da tabela para buscas via HTMX
            return "sysadmin/fragments/tabela_admins :: tabela";
        }
        return "sysadmin/admins";
    }

    /**
     * Retorna o fragmento do formulário de criação de admin (modal vazio via HTMX).
     */
    @GetMapping("/admins/novo")
    public String novoAdminForm(Model model) {
        // CadastroRequest com campos vazios — papel ADMIN fixo
        model.addAttribute("form", new CadastroRequest("", "", "", Papel.ADMIN));
        model.addAttribute("adminId", null);
        return "sysadmin/fragments/form_admin :: modal";
    }

    /**
     * Retorna o formulário pré-preenchido para edição (modal via HTMX).
     */
    @GetMapping("/admins/{id}/editar")
    public String editarAdminForm(@PathVariable Long id, Model model) {
        Usuario admin = usuarioService.buscarPorId(id);
        // Senha em branco — não exibimos a senha existente
        model.addAttribute("form", new CadastroRequest(admin.getNome(), admin.getEmail(), "", Papel.ADMIN));
        model.addAttribute("adminId", id);
        return "sysadmin/fragments/form_admin :: modal";
    }

    /**
     * Cria um novo administrador.
     *
     * <p>CORREÇÃO DO BUG: o campo "papel" chegava como null porque o formulário
     * não enviava o campo hidden corretamente. Agora forçamos papel=ADMIN
     * independente do que vem no form, garantindo que CadastroRequest seja válido.
     *
     * <p>Em caso de sucesso, retorna a tabela completa atualizada (não só a linha).
     * O HTMX substitui o #tabela-admins com a nova versão e o modal fecha via script do layout.
     */
    @PostMapping("/admins")
    public String criarAdmin(
            @Valid @ModelAttribute("form") CadastroRequest form,
            BindingResult bindingResult,
            Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("adminId", null);
            return "sysadmin/fragments/form_admin :: modal";
        }

        try {
            // Garante papel ADMIN independente do que veio no form
            CadastroRequest requestAdmin = new CadastroRequest(
                    form.nome(),
                    form.email(),
                    form.senha(),
                    Papel.ADMIN
            );
            usuarioService.cadastrar(requestAdmin);

            // Sucesso: retorna tabela atualizada (pág. 0, sem filtro de busca)
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

    /**
     * Atualiza os dados de um administrador existente..
     */
    @PutMapping("/admins/{id}")
    public String editarAdmin(
            @PathVariable Long id,
            @Valid @ModelAttribute("form") CadastroRequest form,
            BindingResult bindingResult,
            Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("adminId", id);
            return "sysadmin/fragments/form_admin :: modal";
        }

        try {
            Usuario admin = usuarioService.buscarPorId(id);
            // Atualiza campos — senha só muda se vier preenchida
            String novaSenha = (form.senha() != null && !form.senha().isBlank())
                    ? form.senha() : null;
            usuarioService.atualizarPerfil(id, form.nome(), form.email(), null, novaSenha);

            // Retorna tabela atualizada
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

    /**
     * Alterna o status (ATIVO ↔ BLOQUEADO) de um admin ou cliente.
     * Retorna a tabela de admins atualizada.
     */
    @PostMapping("/admins/{id}/bloquear")
    public String alternarStatus(
            @PathVariable Long id,
            Model model) {

        usuarioService.alternarStatus(id);

        Page<Usuario> admins = usuarioService.listarPorPapel(
                Papel.ADMIN, "", PageRequest.of(0, 10, Sort.by("nome").ascending()));
        model.addAttribute("admins", admins);
        model.addAttribute("busca", "");

        return "sysadmin/fragments/tabela_admins :: tabela";
    }

    // ── Logs & Configurações ──────────────────────────────────────────────────

    @GetMapping("/logs")
    public String visualizarLogs(Model model) {
        List<String> logs = new ArrayList<>();
        logs.add("[2026-05-26 20:20:15] SYSADMIN: Criou novo Administrador (admin2@sweetdelights.com)");
        logs.add("[2026-05-26 19:45:10] SYSTEM: Backup do banco de dados executado com sucesso");
        logs.add("[2026-05-26 18:30:22] ADMIN (admin@sweetdelights.com): Cadastrou novo produto 'Bolo de Chocolate'");
        logs.add("[2026-05-26 17:15:02] SECURITY: Tentativa de login suspeita bloqueada para o IP 192.168.10.45");
        logs.add("[2026-05-26 15:10:55] SYSADMIN: Alterou configurações de e-mail SMTP");
        logs.add("[2026-05-25 11:00:00] SYSTEM: Migração V2 do banco executada com sucesso");
        model.addAttribute("logs", logs);
        return "sysadmin/logs";
    }

    @GetMapping("/configuracoes")
    public String configuracoes() {
        return "sysadmin/configuracoes";
    }
}
