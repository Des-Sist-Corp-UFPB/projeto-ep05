package br.ufpb.dsc.mercado.controller;

import br.ufpb.dsc.mercado.config.TokenProvider;
import br.ufpb.dsc.mercado.domain.Papel;
import br.ufpb.dsc.mercado.domain.StatusUsuario;
import br.ufpb.dsc.mercado.domain.Usuario;
import br.ufpb.dsc.mercado.dto.CadastroRequest;
import br.ufpb.dsc.mercado.dto.CadastroClienteRequest;
import br.ufpb.dsc.mercado.dto.LoginRequest;
import br.ufpb.dsc.mercado.dto.LoginResponse;
import br.ufpb.dsc.mercado.dto.RecuperacaoSenhaDTO;
import br.ufpb.dsc.mercado.exception.ApiException;
import br.ufpb.dsc.mercado.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

/**
 * Controller de autenticação refatorado.
 *
 * MUDANÇAS:
 * ─────────
 * 1. Removidos try/catch manuais — exceções são tratadas pelo GlobalExceptionHandler.
 *    Resultado: código menor, mais legível, sem duplicação de lógica de erro.
 *
 * 2. Erros de login agora lançam ApiException tipada com HTTP correto:
 *    - 401 para credenciais inválidas
 *    - 403 para conta bloqueada
 *
 * 3. Adicionados endpoints de recuperação de senha:
 *    - POST /api/auth/recuperar-senha    → solicita token
 *    - POST /api/auth/redefinir-senha    → redefine com token
 */
@RestController
@RequestMapping("/api/auth")
public class AuthRestController {

    private final UsuarioService usuarioService;
    private final TokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;

    @SuppressWarnings("EI_EXPOSE_REP2")
    public AuthRestController(UsuarioService usuarioService,
                              TokenProvider tokenProvider,
                              PasswordEncoder passwordEncoder) {
        this.usuarioService = usuarioService;
        this.tokenProvider = tokenProvider;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Cadastro via API REST (usado pelo frontend React).
     * Aceita { nome, email, senha, papel } — os campos extras (cpf, sobrenome…)
     * são opcionais e usados apenas pelo cadastro web (Thymeleaf).
     * O papel é sempre forçado para CLIENTE independente do que o cliente enviar.
     */
    @PostMapping("/cadastro")
    public ResponseEntity<LoginResponse> cadastrar(@Valid @RequestBody CadastroRequest request) {
        CadastroRequest requestCliente = new CadastroRequest(
                request.nome(), request.email(), request.senha(), Papel.CLIENTE);
        Usuario usuario = usuarioService.cadastrar(requestCliente);

        return ResponseEntity.ok(buildLoginResponse(usuario));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        // buscarPorEmail já lança ApiException 404 se não existir → tratado como 401 abaixo
        Usuario usuario;
        try {
            usuario = usuarioService.buscarPorEmail(request.email());
        } catch (ApiException e) {
            // Não revelar se o e-mail existe — resposta genérica de credenciais inválidas
            throw ApiException.naoAutorizado("E-mail ou senha inválidos");
        }

        if (usuario.getStatus() == StatusUsuario.BLOQUEADO) {
            throw ApiException.proibido("Sua conta está bloqueada. Entre em contato com o suporte.");
        }

        if (!passwordEncoder.matches(request.senha(), usuario.getSenha())) {
            throw ApiException.naoAutorizado("E-mail ou senha inválidos");
        }

        return ResponseEntity.ok(buildLoginResponse(usuario));
    }

    /**
     * Passo 1 da recuperação de senha: solicita o envio do link/token.
     *
     * Resposta sempre genérica para não vazar se o e-mail existe.
     * Em DEV o token é retornado no corpo para facilitar testes.
     */
    @PostMapping("/recuperar-senha")
    public ResponseEntity<MensagemResponse> solicitarRecuperacao(
            @Valid @RequestBody RecuperacaoSenhaDTO.SolicitarRequest request) {

        // token é retornado apenas em dev; em prod, seria enviado por e-mail
        String token = usuarioService.gerarTokenRecuperacao(request.email());

        // Em produção, remover o token da resposta e apenas retornar a mensagem genérica
        return ResponseEntity.ok(new MensagemResponse(
                "Se o e-mail estiver cadastrado, você receberá um link em breve.",
                token // remover em produção
        ));
    }

    /**
     * Passo 2 da recuperação de senha: valida o token e salva a nova senha.
     */
    @PostMapping("/redefinir-senha")
    public ResponseEntity<MensagemResponse> redefinirSenha(
            @Valid @RequestBody RecuperacaoSenhaDTO.RedefinirRequest request) {

        usuarioService.redefinirSenha(request.token(), request.novaSenha());

        return ResponseEntity.ok(new MensagemResponse("Senha redefinida com sucesso!", null));
    }

    // ── Utilitários ──────────────────────────────────────────────────────────

    private LoginResponse buildLoginResponse(Usuario usuario) {
        String token = tokenProvider.gerarToken(usuario.getEmail(), usuario.getPapel().name());
        return new LoginResponse(token, usuario.getEmail(), usuario.getNome(), usuario.getPapel());
    }

    /** Resposta simples de mensagem para endpoints sem entidade de retorno. */
    public record MensagemResponse(String mensagem, String token) {}
}
