package br.ufpb.dsc.mercado.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthController {

    @GetMapping("/login")
    public String login() {
        return "auth/login";
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
