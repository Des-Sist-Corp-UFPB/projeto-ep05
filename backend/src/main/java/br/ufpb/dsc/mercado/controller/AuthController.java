package br.ufpb.dsc.mercado.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import br.ufpb.dsc.mercado.dto.CadastroClienteRequest;
import br.ufpb.dsc.mercado.service.UsuarioService;
import jakarta.validation.Valid;

/**
 * Controller de autenticação — gerencia as rotas relacionadas a login/logout e cadastro.
 *
 * <p>O Spring Security cuida automaticamente do processamento do formulário de login
 * (validação de credenciais, criação da sessão, etc.). Este controller apenas
 * serve a <strong>página</strong> de login — o Spring Security intercepta o POST.
 *
 * <p><strong>Fluxo do Spring Security Form Login:</strong>
 * <ol>
 *   <li>Usuário acessa uma rota protegida (ex.: {@code /produtos}).</li>
 *   <li>Spring Security detecta que não está autenticado e redireciona para {@code /login}.</li>
 *   <li>Este controller serve a página HTML de login.</li>
 *   <li>Usuário submete o formulário com username/password para {@code POST /login}.</li>
 *   <li>Spring Security intercepta o POST, valida as credenciais e redireciona para {@code /produtos}.</li>
 * </ol>
 *
 * <p>A URL de processamento do login ({@code POST /login}) é gerenciada <em>internamente</em>
 * pelo Spring Security — não precisamos (nem devemos) criar um método para ela aqui.
 *
 * @author DSC - UFPB Campus IV
 */
@Controller
public class AuthController {

    private final UsuarioService usuarioService;

    public AuthController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/admin/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/admin/cadastro")
    public String cadastro() {
        return "auth/cadastro";
    }

    @PostMapping("/admin/cadastro")
    public String processarCadastro(
            @Valid @ModelAttribute CadastroClienteRequest form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            Map<String, String> erros = new HashMap<>();
            bindingResult.getFieldErrors().forEach(e -> erros.put(e.getField(), e.getDefaultMessage()));
            model.addAttribute("errosCampos", erros);
            model.addAttribute("dadosAnteriores", form);
            return "auth/cadastro";
        }

        if (!form.senhasConferem()) {
            model.addAttribute("errosCampos", Map.of("confirmacaoSenha", "As senhas não conferem"));
            model.addAttribute("dadosAnteriores", form);
            return "auth/cadastro";
        }

        try {
            usuarioService.cadastrarCliente(form);
            redirectAttributes.addFlashAttribute("sucesso",
                    "Conta criada com sucesso! Faça login para continuar.");
            return "redirect:/admin/login";
        } catch (IllegalArgumentException e) {
            model.addAttribute("erro", e.getMessage());
            model.addAttribute("dadosAnteriores", form);
            return "auth/cadastro";
        }
    }

    /**
     * Rota raiz "/" — redireciona para o painel correto conforme o papel do usuário.
     * Evita o erro "No static resource ." quando o Spring Security tenta redirecionar
     * para "/" após o login ou quando o usuário acessa a raiz diretamente.
     */
    @GetMapping("/")
    public String raiz(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/admin/login";
        }
        boolean isSysAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SYSADMIN"));
        return isSysAdmin ? "redirect:/sysadmin/dashboard" : "redirect:/admin/dashboard";
    }
}
