package br.ufpb.dsc.mercado.controller;

import br.ufpb.dsc.mercado.config.TokenProvider;
import br.ufpb.dsc.mercado.domain.Papel;
import br.ufpb.dsc.mercado.domain.StatusUsuario;
import br.ufpb.dsc.mercado.domain.Usuario;
import br.ufpb.dsc.mercado.dto.CadastroClienteRequest;
import br.ufpb.dsc.mercado.dto.LoginRequest;
import br.ufpb.dsc.mercado.dto.RecuperacaoSenhaDTO;
import br.ufpb.dsc.mercado.exception.ApiException;
import br.ufpb.dsc.mercado.service.RecuperacaoSenhaService;
import br.ufpb.dsc.mercado.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários puros: o controller é instanciado diretamente com
 * dependências mockadas (sem MockMvc/contexto Spring), pois toda a lógica
 * de negócio relevante já está aqui (não delega para a view layer).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthRestController — Testes Unitários")
class AuthRestControllerTest {

    @Mock UsuarioService usuarioService;
    @Mock TokenProvider tokenProvider;
    @Mock PasswordEncoder passwordEncoder;
    @Mock RecuperacaoSenhaService recuperacaoSenhaService;
    @Mock Environment environment;

    private AuthRestController controller;

    @BeforeEach
    void setUp() {
        controller = new AuthRestController(usuarioService, tokenProvider, passwordEncoder, recuperacaoSenhaService, environment);
    }

    private Usuario usuarioComId(Long id, String email, String senhaHash, Papel papel) {
        Usuario u = new Usuario("Ana", email, senhaHash, papel);
        u.setId(id);
        return u;
    }

    private CadastroClienteRequest cadastroValido() {
        return new CadastroClienteRequest(
                "Ana", "Silva", "ana@teste.com", "111.222.333-44", "(83) 99999-9999",
                LocalDate.of(2000, 1, 1), "Senha123", "Senha123"
        );
    }

    // ── cadastrar ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("cadastrar deve criar o usuário e retornar token de login")
    void cadastrar_dadosValidos_deveRetornarLoginResponse() {
        Usuario criado = usuarioComId(1L, "ana@teste.com", "hash", Papel.CLIENTE);
        when(usuarioService.cadastrarCliente(any())).thenReturn(criado);
        when(tokenProvider.gerarToken("ana@teste.com", "CLIENTE")).thenReturn("jwt-token");

        ResponseEntity<?> resposta = controller.cadastrar(cadastroValido());

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        var body = (br.ufpb.dsc.mercado.dto.LoginResponse) resposta.getBody();
        assertThat(body.token()).isEqualTo("jwt-token");
        assertThat(body.email()).isEqualTo("ana@teste.com");
        assertThat(body.papel()).isEqualTo(Papel.CLIENTE);
    }

    // ── login ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("login deve retornar token quando credenciais estão corretas")
    void login_credenciaisCorretas_deveRetornarToken() {
        Usuario usuario = usuarioComId(1L, "ana@teste.com", "hash", Papel.CLIENTE);
        when(usuarioService.buscarPorEmail("ana@teste.com")).thenReturn(usuario);
        when(passwordEncoder.matches("senha123", "hash")).thenReturn(true);
        when(tokenProvider.gerarToken("ana@teste.com", "CLIENTE")).thenReturn("jwt-token");

        ResponseEntity<?> resposta = controller.login(new LoginRequest("ana@teste.com", "senha123"));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        var body = (br.ufpb.dsc.mercado.dto.LoginResponse) resposta.getBody();
        assertThat(body.token()).isEqualTo("jwt-token");
    }

    @Test
    @DisplayName("login deve lançar 401 quando o e-mail não existe")
    void login_emailInexistente_deveLancar401() {
        when(usuarioService.buscarPorEmail("naoexiste@teste.com"))
                .thenThrow(ApiException.naoEncontrado("E-mail não cadastrado"));

        ApiException ex = catchThrowableOfType(
                () -> controller.login(new LoginRequest("naoexiste@teste.com", "x")),
                ApiException.class);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("login deve lançar 403 quando a conta está bloqueada")
    void login_contaBloqueada_deveLancar403() {
        Usuario usuario = usuarioComId(1L, "ana@teste.com", "hash", Papel.CLIENTE);
        usuario.setStatus(StatusUsuario.BLOQUEADO);
        when(usuarioService.buscarPorEmail("ana@teste.com")).thenReturn(usuario);

        ApiException ex = catchThrowableOfType(
                () -> controller.login(new LoginRequest("ana@teste.com", "qualquer")),
                ApiException.class);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    @DisplayName("login deve lançar 401 quando a senha está incorreta")
    void login_senhaIncorreta_deveLancar401() {
        Usuario usuario = usuarioComId(1L, "ana@teste.com", "hash", Papel.CLIENTE);
        when(usuarioService.buscarPorEmail("ana@teste.com")).thenReturn(usuario);
        when(passwordEncoder.matches("errada", "hash")).thenReturn(false);

        ApiException ex = catchThrowableOfType(
                () -> controller.login(new LoginRequest("ana@teste.com", "errada")),
                ApiException.class);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ── recuperar-senha ──────────────────────────────────────────────────────

    @Test
    @DisplayName("solicitarRecuperacao em produção não deve expor o token na resposta")
    void solicitarRecuperacao_forada_dev_naoDeveExporToken() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        when(recuperacaoSenhaService.solicitarRecuperacao("ana@teste.com")).thenReturn("token-secreto");

        ResponseEntity<AuthRestController.MensagemResponse> resposta =
                controller.solicitarRecuperacao(new RecuperacaoSenhaDTO.SolicitarRequest("ana@teste.com"));

        assertThat(resposta.getBody().token()).isNull();
        assertThat(resposta.getBody().mensagem()).contains("Se o e-mail estiver cadastrado");
    }

    @Test
    @DisplayName("solicitarRecuperacao no perfil dev deve expor o token na resposta")
    void solicitarRecuperacao_perfilDev_deveExporToken() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});
        when(recuperacaoSenhaService.solicitarRecuperacao("ana@teste.com")).thenReturn("token-secreto");

        ResponseEntity<AuthRestController.MensagemResponse> resposta =
                controller.solicitarRecuperacao(new RecuperacaoSenhaDTO.SolicitarRequest("ana@teste.com"));

        assertThat(resposta.getBody().token()).isEqualTo("token-secreto");
    }

    @Test
    @DisplayName("solicitarRecuperacao deve retornar mensagem genérica mesmo quando o e-mail não existe")
    void solicitarRecuperacao_emailInexistente_deveRetornarMensagemGenerica() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});
        when(recuperacaoSenhaService.solicitarRecuperacao("naoexiste@teste.com")).thenReturn(null);

        ResponseEntity<AuthRestController.MensagemResponse> resposta =
                controller.solicitarRecuperacao(new RecuperacaoSenhaDTO.SolicitarRequest("naoexiste@teste.com"));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody().token()).isNull();
    }

    // ── redefinir-senha ──────────────────────────────────────────────────────

    @Test
    @DisplayName("redefinirSenha deve delegar ao service e retornar mensagem de sucesso")
    void redefinirSenha_deveDelegarAoService() {
        ResponseEntity<AuthRestController.MensagemResponse> resposta =
                controller.redefinirSenha(new RecuperacaoSenhaDTO.RedefinirRequest("token123", "novaSenha123"));

        verify(recuperacaoSenhaService).redefinirSenha("token123", "novaSenha123");
        assertThat(resposta.getBody().mensagem()).isEqualTo("Senha redefinida com sucesso!");
    }
}
