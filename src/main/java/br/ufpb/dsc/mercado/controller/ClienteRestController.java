package br.ufpb.dsc.mercado.controller;

import br.ufpb.dsc.mercado.domain.Cartao;
import br.ufpb.dsc.mercado.domain.Endereco;
import br.ufpb.dsc.mercado.domain.Usuario;
import br.ufpb.dsc.mercado.dto.*;
import br.ufpb.dsc.mercado.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/clientes")
public class ClienteRestController {

    private final UsuarioService usuarioService;

    public ClienteRestController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    private Usuario obterUsuarioLogado() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioService.buscarPorEmail(email);
    }

    @GetMapping("/perfil")
    public ResponseEntity<?> obterPerfil() {
        Usuario usuario = obterUsuarioLogado();
        return ResponseEntity.ok(new CadastroRequest(
                usuario.getNome(),
                usuario.getEmail(),
                null,
                usuario.getPapel()
        ));
    }

    @PutMapping("/perfil")
    public ResponseEntity<?> atualizarPerfil(@RequestBody PerfilUpdateRequest request) {
        try {
            Usuario usuario = obterUsuarioLogado();
            Usuario atualizado = usuarioService.atualizarPerfil(
                    usuario.getId(),
                    request.nome(),
                    request.email(),
                    request.senhaAtual(),
                    request.novaSenha()
            );
            return ResponseEntity.ok(new CadastroRequest(
                    atualizado.getNome(),
                    atualizado.getEmail(),
                    null,
                    atualizado.getPapel()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/perfil")
    public ResponseEntity<?> excluirConta() {
        Usuario usuario = obterUsuarioLogado();
        usuarioService.excluirConta(usuario.getId());
        return ResponseEntity.ok().build();
    }

    // === ENDEREÇOS ===
    @GetMapping("/enderecos")
    public ResponseEntity<List<EnderecoDTO>> listarEnderecos() {
        Usuario usuario = obterUsuarioLogado();
        List<EnderecoDTO> dtos = usuarioService.listarEnderecos(usuario.getId()).stream()
                .map(e -> new EnderecoDTO(
                        e.getId(),
                        e.getLogradouro(),
                        e.getNumero(),
                        e.getComplemento(),
                        e.getBairro(),
                        e.getCidade(),
                        e.getEstado(),
                        e.getCep(),
                        e.getPrincipal()
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/enderecos")
    public ResponseEntity<?> cadastrarEndereco(@Valid @RequestBody EnderecoDTO request) {
        try {
            Usuario usuario = obterUsuarioLogado();
            Endereco e = usuarioService.cadastrarEndereco(usuario.getId(), request);
            return ResponseEntity.ok(new EnderecoDTO(
                    e.getId(),
                    e.getLogradouro(),
                    e.getNumero(),
                    e.getComplemento(),
                    e.getBairro(),
                    e.getCidade(),
                    e.getEstado(),
                    e.getCep(),
                    e.getPrincipal()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/enderecos/{id}")
    public ResponseEntity<?> removerEndereco(@PathVariable Long id) {
        try {
            Usuario usuario = obterUsuarioLogado();
            usuarioService.removerEndereco(usuario.getId(), id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // === CARTÕES ===
    @GetMapping("/cartoes")
    public ResponseEntity<List<CartaoDTO>> listarCartoes() {
        Usuario usuario = obterUsuarioLogado();
        List<CartaoDTO> dtos = usuarioService.listarCartoes(usuario.getId()).stream()
                .map(c -> new CartaoDTO(
                        c.getId(),
                        c.getNomeTitular(),
                        c.getBandeira(),
                        c.getQuatroUltimosDigitos(),
                        c.getDataExpiracao()
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/cartoes")
    public ResponseEntity<?> cadastrarCartao(@Valid @RequestBody CartaoSalvarDTO request) {
        try {
            Usuario usuario = obterUsuarioLogado();
            Cartao c = usuarioService.cadastrarCartao(usuario.getId(), request);
            return ResponseEntity.ok(new CartaoDTO(
                    c.getId(),
                    c.getNomeTitular(),
                    c.getBandeira(),
                    c.getQuatroUltimosDigitos(),
                    c.getDataExpiracao()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/cartoes/{id}")
    public ResponseEntity<?> removerCartao(@PathVariable Long id) {
        try {
            Usuario usuario = obterUsuarioLogado();
            usuarioService.removerCartao(usuario.getId(), id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Record para atualização de perfil
    public record PerfilUpdateRequest(
            String nome,
            String email,
            String senhaAtual,
            String novaSenha
    ) {}
}
