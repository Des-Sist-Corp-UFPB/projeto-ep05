package br.ufpb.dsc.mercado.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller de autenticação — gerencia as rotas relacionadas a login/logout.
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

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/cadastro")
    public String cadastro() {
        return "auth/cadastro";
    }

    /**
     * Rota raiz "/" — redireciona para o painel correto conforme o papel do usuário.
     * Evita o erro "No static resource ." quando o Spring Security tenta redirecionar
     * para "/" após o login ou quando o usuário acessa a raiz diretamente.
     */
    @GetMapping("/")
    public String raiz(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }
        boolean isSysAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SYSADMIN"));
        return isSysAdmin ? "redirect:/sysadmin/dashboard" : "redirect:/admin/dashboard";
    }
}
