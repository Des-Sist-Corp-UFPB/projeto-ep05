package br.ufpb.dsc.mercado.audit;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

/**
 * Captura eventos de login bem-sucedido e falha de autenticação
 * publicados automaticamente pelo Spring Security.
 */
@Component
public class AuthAuditoriaListener {

    private final AuditoriaService auditoriaService;

    public AuthAuditoriaListener(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @EventListener
    public void onLoginSucesso(AuthenticationSuccessEvent event) {
        String email = resolverEmail(event.getAuthentication().getPrincipal());
        boolean isSysAdmin = event.getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SYSADMIN"));
        boolean isAdmin = event.getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        String papel = isSysAdmin ? "SYSADMIN" : isAdmin ? "ADMIN" : "CLIENTE";
        String descricao = "Login realizado com sucesso";

        auditoriaService.registrar(LogAuditoria.builder()
                .papelAtor(papel)
                .ator(email)
                .categoria("AUTH")
                .descricao(descricao)
                .build());
    }

    @EventListener
    public void onLoginFalha(AbstractAuthenticationFailureEvent event) {
        String email = resolverEmail(event.getAuthentication().getPrincipal());
        auditoriaService.registrar(LogAuditoria.builder()
                .papelAtor("DESCONHECIDO")
                .ator(email)
                .categoria("AUTH")
                .descricao("Tentativa de login falhou: " + event.getException().getMessage())
                .falha()
                .build());
    }

    private String resolverEmail(Object principal) {
        if (principal instanceof UserDetails ud) return ud.getUsername();
        return principal != null ? principal.toString() : "desconhecido";
    }
}
