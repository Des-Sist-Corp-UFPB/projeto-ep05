package br.ufpb.dsc.mercado.controller;

import java.util.Arrays;

import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.ufpb.dsc.mercado.config.TokenProvider;
import br.ufpb.dsc.mercado.domain.StatusUsuario;
import br.ufpb.dsc.mercado.domain.Usuario;
import br.ufpb.dsc.mercado.dto.CadastroClienteRequest;
import br.ufpb.dsc.mercado.dto.LoginRequest;
import br.ufpb.dsc.mercado.dto.LoginResponse;
import br.ufpb.dsc.mercado.dto.RecuperacaoSenhaDTO;
import br.ufpb.dsc.mercado.exception.ApiException;
import br.ufpb.dsc.mercado.service.RecuperacaoSenhaService;
import br.ufpb.dsc.mercado.service.UsuarioService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthRestController {

    private final UsuarioService usuarioService;
    private final TokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final RecuperacaoSenhaService recuperacaoSenhaService;
    private final Environment environment;

    public AuthRestController(UsuarioService usuarioService,
                              TokenProvider tokenProvider,
                              PasswordEncoder passwordEncoder,
                              RecuperacaoSenhaService recuperacaoSenhaService,
                              Environment environment) {
        this.usuarioService = usuarioService;
        this.tokenProvider = tokenProvider;
        this.passwordEncoder = passwordEncoder;
        this.recuperacaoSenhaService = recuperacaoSenhaService;
        this.environment = environment;
    }

    @PostMapping("/cadastro")
    public ResponseEntity<LoginResponse> cadastrar(
            @Valid @RequestBody CadastroClienteRequest request) {
        Usuario usuario = usuarioService.cadastrarCliente(request);
        return ResponseEntity.ok(buildLoginResponse(usuario));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {
        Usuario usuario;
        try {
            usuario = usuarioService.buscarPorEmail(request.email());
        } catch (Exception e) {
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

    @PostMapping("/recuperar-senha")
    public ResponseEntity<MensagemResponse> solicitarRecuperacao(
            @Valid @RequestBody RecuperacaoSenhaDTO.SolicitarRequest request) {

        String token = recuperacaoSenhaService.solicitarRecuperacao(request.email());

        String tokenDev = isDev() ? token : null;

        return ResponseEntity.ok(new MensagemResponse(
                "Se o e-mail estiver cadastrado, você receberá um link em breve.",
                tokenDev
        ));
    }

    @PostMapping("/redefinir-senha")
    public ResponseEntity<MensagemResponse> redefinirSenha(
            @Valid @RequestBody RecuperacaoSenhaDTO.RedefinirRequest request) {

        recuperacaoSenhaService.redefinirSenha(request.token(), request.novaSenha());

        return ResponseEntity.ok(new MensagemResponse("Senha redefinida com sucesso!", null));
    }

    private LoginResponse buildLoginResponse(Usuario usuario) {
        String token = tokenProvider.gerarToken(usuario.getEmail(), usuario.getPapel().name());
        return new LoginResponse(token, usuario.getEmail(), usuario.getNome(), usuario.getPapel());
    }

    private boolean isDev() {
        return Arrays.asList(environment.getActiveProfiles()).contains("dev");
    }

    public record MensagemResponse(String mensagem, String token) {}
}