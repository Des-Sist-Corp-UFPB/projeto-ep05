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

@Controller
public class AuthController {

    private final UsuarioService usuarioService;

    public AuthController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/cadastro")
    public String cadastro() {
        return "auth/cadastro";
    }

    @PostMapping("/cadastro")
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
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            model.addAttribute("erro", e.getMessage());
            model.addAttribute("dadosAnteriores", form);
            return "auth/cadastro";
        }
    }

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