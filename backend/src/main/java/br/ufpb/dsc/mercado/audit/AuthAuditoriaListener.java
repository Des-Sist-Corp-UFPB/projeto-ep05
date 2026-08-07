package br.ufpb.dsc.mercado.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(AuthAuditoriaListener.class);

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

        // Log estruturado para o Loki, além da auditoria já persistida no banco.
        // Cobre os logins via painel (ADMIN/SYSADMIN), que passam pelo form
        // login do Spring Security e não pelo AuthRestController REST.
        log.info("Login realizado com sucesso. papel={} email={}", papel, email);

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

        // Não logamos a mensagem de exceção completa do Spring Security aqui
        // porque em alguns casos ela pode ecoar a credencial submetida; mantemos
        // só o tipo de falha, disponível em event.getException().getClass().
        log.warn("Tentativa de login falhou no painel admin/sysadmin. tipo_falha={}",
                event.getException().getClass().getSimpleName());

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
