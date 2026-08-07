package br.ufpb.dsc.mercado.audit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthAuditoriaListener — Testes Unitários")
class AuthAuditoriaListenerTest {

    @Mock
    private AuditoriaService auditoriaService;

    private AuthAuditoriaListener listener;

    @BeforeEach
    void setUp() {
        listener = new AuthAuditoriaListener(auditoriaService);
    }

    private UserDetails userDetails(String email, String... authorities) {
        List<GrantedAuthority> grantedAuthorities = List.of(authorities).stream()
                .map(SimpleGrantedAuthority::new)
                .map(GrantedAuthority.class::cast)
                .toList();
        return new User(email, "senha-qualquer", grantedAuthorities);
    }

    // ── onLoginSucesso ────────────────────────────────────────────────────────

    @Test
    @DisplayName("onLoginSucesso: SYSADMIN deve registrar papelAtor SYSADMIN")
    void onLoginSucesso_sysAdmin_devePapelSysAdmin() {
        UserDetails ud = userDetails("sys@test.com", "ROLE_SYSADMIN");
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(ud, null, ud.getAuthorities());
        AuthenticationSuccessEvent event = new AuthenticationSuccessEvent(auth);

        listener.onLoginSucesso(event);

        ArgumentCaptor<LogAuditoria> captor = ArgumentCaptor.forClass(LogAuditoria.class);
        verify(auditoriaService).registrar(captor.capture());
        LogAuditoria log = captor.getValue();
        assertThat(log.getPapelAtor()).isEqualTo("SYSADMIN");
        assertThat(log.getAtor()).isEqualTo("sys@test.com");
        assertThat(log.getCategoria()).isEqualTo("AUTH");
        assertThat(log.getResultado()).isEqualTo("SUCCESS");
        assertThat(log.getDescricao()).isEqualTo("Login realizado com sucesso");
    }

    @Test
    @DisplayName("onLoginSucesso: ADMIN deve registrar papelAtor ADMIN")
    void onLoginSucesso_admin_devePapelAdmin() {
        UserDetails ud = userDetails("admin@test.com", "ROLE_ADMIN");
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(ud, null, ud.getAuthorities());
        AuthenticationSuccessEvent event = new AuthenticationSuccessEvent(auth);

        listener.onLoginSucesso(event);

        ArgumentCaptor<LogAuditoria> captor = ArgumentCaptor.forClass(LogAuditoria.class);
        verify(auditoriaService).registrar(captor.capture());
        assertThat(captor.getValue().getPapelAtor()).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("onLoginSucesso: usuário sem papel especial deve registrar CLIENTE")
    void onLoginSucesso_cliente_devePapelCliente() {
        UserDetails ud = userDetails("cliente@test.com", "ROLE_CLIENTE");
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(ud, null, ud.getAuthorities());
        AuthenticationSuccessEvent event = new AuthenticationSuccessEvent(auth);

        listener.onLoginSucesso(event);

        ArgumentCaptor<LogAuditoria> captor = ArgumentCaptor.forClass(LogAuditoria.class);
        verify(auditoriaService).registrar(captor.capture());
        assertThat(captor.getValue().getPapelAtor()).isEqualTo("CLIENTE");
    }

    @Test
    @DisplayName("onLoginSucesso: principal que não é UserDetails deve usar toString como email")
    void onLoginSucesso_principalNaoUserDetails_usaToString() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("usuario-string", null, List.of());
        AuthenticationSuccessEvent event = new AuthenticationSuccessEvent(auth);

        listener.onLoginSucesso(event);

        ArgumentCaptor<LogAuditoria> captor = ArgumentCaptor.forClass(LogAuditoria.class);
        verify(auditoriaService).registrar(captor.capture());
        assertThat(captor.getValue().getAtor()).isEqualTo("usuario-string");
        assertThat(captor.getValue().getPapelAtor()).isEqualTo("CLIENTE");
    }

    @Test
    @DisplayName("onLoginSucesso: principal nulo deve resolver ator como 'desconhecido'")
    void onLoginSucesso_principalNulo_resolveDesconhecido() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(null, null, List.of());
        AuthenticationSuccessEvent event = new AuthenticationSuccessEvent(auth);

        listener.onLoginSucesso(event);

        ArgumentCaptor<LogAuditoria> captor = ArgumentCaptor.forClass(LogAuditoria.class);
        verify(auditoriaService).registrar(captor.capture());
        assertThat(captor.getValue().getAtor()).isEqualTo("desconhecido");
    }

    // ── onLoginFalha ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("onLoginFalha: deve registrar papelAtor DESCONHECIDO e resultado FAILURE")
    void onLoginFalha_deveRegistrarFalha() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("naoexiste@test.com", "senha-errada");
        AbstractAuthenticationFailureEvent event =
                new AuthenticationFailureBadCredentialsEvent(auth, new BadCredentialsException("Credenciais inválidas"));

        listener.onLoginFalha(event);

        ArgumentCaptor<LogAuditoria> captor = ArgumentCaptor.forClass(LogAuditoria.class);
        verify(auditoriaService).registrar(captor.capture());
        LogAuditoria log = captor.getValue();
        assertThat(log.getPapelAtor()).isEqualTo("DESCONHECIDO");
        assertThat(log.getAtor()).isEqualTo("naoexiste@test.com");
        assertThat(log.getCategoria()).isEqualTo("AUTH");
        assertThat(log.getResultado()).isEqualTo("FAILURE");
        assertThat(log.getDescricao()).contains("Credenciais inválidas");
    }

    @Test
    @DisplayName("onLoginFalha: deve incluir a mensagem da exceção na descrição")
    void onLoginFalha_descricaoDeveConterMensagemExcecao() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("user@test.com", "x");
        AbstractAuthenticationFailureEvent event =
                new AuthenticationFailureBadCredentialsEvent(auth, new BadCredentialsException("conta bloqueada"));

        listener.onLoginFalha(event);

        ArgumentCaptor<LogAuditoria> captor = ArgumentCaptor.forClass(LogAuditoria.class);
        verify(auditoriaService).registrar(captor.capture());
        assertThat(captor.getValue().getDescricao()).isEqualTo("Tentativa de login falhou: conta bloqueada");
    }
}
