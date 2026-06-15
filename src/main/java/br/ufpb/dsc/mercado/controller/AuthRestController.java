package br.ufpb.dsc.mercado.controller;

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
import br.ufpb.dsc.mercado.service.UsuarioService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthRestController {

    private final UsuarioService usuarioService;
    private final TokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;

    @SuppressWarnings("EI_EXPOSE_REP2") // Beans Spring são singletons gerenciados pelo container
    public AuthRestController(UsuarioService usuarioService,
                              TokenProvider tokenProvider,
                              PasswordEncoder passwordEncoder) {
        this.usuarioService = usuarioService;
        this.tokenProvider = tokenProvider;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/cadastro")
    public ResponseEntity<?> cadastrar(@Valid @RequestBody CadastroClienteRequest request) {
        try {
            Usuario usuario = usuarioService.cadastrarCliente(request);
            return ResponseEntity.ok(new LoginResponse(
                tokenProvider.gerarToken(usuario.getEmail(), usuario.getPapel().name()),
                usuario.getEmail(),
                usuario.getNome(),
                usuario.getPapel()
        ));
    } catch (IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            Usuario usuario = usuarioService.buscarPorEmail(request.email());
            
            if (usuario.getStatus() == StatusUsuario.BLOQUEADO) {
                return ResponseEntity.status(403).body("Sua conta está bloqueada pelo administrador");
            }

            if (!passwordEncoder.matches(request.senha(), usuario.getSenha())) {
                return ResponseEntity.status(401).body("Senha incorreta");
            }

            String token = tokenProvider.gerarToken(usuario.getEmail(), usuario.getPapel().name());
            
            return ResponseEntity.ok(new LoginResponse(
                    token,
                    usuario.getEmail(),
                    usuario.getNome(),
                    usuario.getPapel()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).body("E-mail não cadastrado");
        }
    }
}
