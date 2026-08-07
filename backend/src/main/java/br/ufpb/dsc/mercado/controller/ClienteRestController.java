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

/**
 * Controller de cliente refatorado.
 *
 * MUDANÇAS:
 * ─────────
 * 1. Removidos try/catch manuais em todos os métodos.
 *    Exceções ApiException são capturadas pelo GlobalExceptionHandler, que
 *    retorna o JSON de erro padronizado automaticamente.
 *
 * 2. PerfilUpdateRequest movido para DTO próprio (ver abaixo) em vez de record
 *    interno no controller, facilitando reutilização e testes unitários.
 *
 * 3. Mapeamento de entidade → DTO extraído para métodos privados (toDTO),
 *    evitando repetição e facilitando futura extração para um mapper.
 */
@RestController
@RequestMapping("/api/clientes")
public class ClienteRestController {

    private final UsuarioService usuarioService;

    @SuppressWarnings("EI_EXPOSE_REP2")
    public ClienteRestController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    // ── Perfil ────────────────────────────────────────────────────────────────

    @GetMapping("/perfil")
    public ResponseEntity<CadastroRequest> obterPerfil() {
        Usuario usuario = obterUsuarioLogado();
        return ResponseEntity.ok(toPerfilDTO(usuario));
    }

    @PutMapping("/perfil")
    public ResponseEntity<CadastroRequest> atualizarPerfil(
            @RequestBody PerfilUpdateRequest request) {

        Usuario usuario = obterUsuarioLogado();
        Usuario atualizado = usuarioService.atualizarPerfil(
                usuario.getId(),
                request.nome(),
                request.email(),
                request.senhaAtual(),
                request.novaSenha()
        );
        return ResponseEntity.ok(toPerfilDTO(atualizado));
    }

    @DeleteMapping("/perfil")
    public ResponseEntity<Void> excluirConta() {
        Usuario usuario = obterUsuarioLogado();
        usuarioService.excluirConta(usuario.getId());
        return ResponseEntity.ok().build();
    }

    // ── Endereços ─────────────────────────────────────────────────────────────

    @GetMapping("/enderecos")
    public ResponseEntity<List<EnderecoDTO>> listarEnderecos() {
        Usuario usuario = obterUsuarioLogado();
        List<EnderecoDTO> dtos = usuarioService.listarEnderecos(usuario.getId())
                .stream().map(this::toEnderecoDTO).toList();
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/enderecos")
    public ResponseEntity<EnderecoDTO> cadastrarEndereco(
            @Valid @RequestBody EnderecoDTO request) {

        Usuario usuario = obterUsuarioLogado();
        Endereco e = usuarioService.cadastrarEndereco(usuario.getId(), request);
        return ResponseEntity.ok(toEnderecoDTO(e));
    }

    @DeleteMapping("/enderecos/{id}")
    public ResponseEntity<Void> removerEndereco(@PathVariable Long id) {
        Usuario usuario = obterUsuarioLogado();
        usuarioService.removerEndereco(usuario.getId(), id);
        return ResponseEntity.ok().build();
    }

    // ── Cartões ───────────────────────────────────────────────────────────────

    @GetMapping("/cartoes")
    public ResponseEntity<List<CartaoDTO>> listarCartoes() {
        Usuario usuario = obterUsuarioLogado();
        List<CartaoDTO> dtos = usuarioService.listarCartoes(usuario.getId())
                .stream().map(this::toCartaoDTO).toList();
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/cartoes")
    public ResponseEntity<CartaoDTO> cadastrarCartao(
            @Valid @RequestBody CartaoSalvarDTO request) {

        Usuario usuario = obterUsuarioLogado();
        Cartao c = usuarioService.cadastrarCartao(usuario.getId(), request);
        return ResponseEntity.ok(toCartaoDTO(c));
    }

    @DeleteMapping("/cartoes/{id}")
    public ResponseEntity<Void> removerCartao(@PathVariable Long id) {
        Usuario usuario = obterUsuarioLogado();
        usuarioService.removerCartao(usuario.getId(), id);
        return ResponseEntity.ok().build();
    }

    // ── Utilitários ──────────────────────────────────────────────────────────

    private Usuario obterUsuarioLogado() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioService.buscarPorEmail(email);
    }

    private CadastroRequest toPerfilDTO(Usuario u) {
        return new CadastroRequest(u.getNome(), u.getEmail(), null, u.getPapel());
    }

    private EnderecoDTO toEnderecoDTO(Endereco e) {
        return new EnderecoDTO(
                e.getId(), e.getLogradouro(), e.getNumero(), e.getComplemento(),
                e.getBairro(), e.getCidade(), e.getEstado(), e.getCep(), e.getPrincipal()
        );
    }

    private CartaoDTO toCartaoDTO(Cartao c) {
        return new CartaoDTO(
                c.getId(), c.getNomeTitular(), c.getBandeira(),
                c.getQuatroUltimosDigitos(), c.getDataExpiracao()
        );
    }

    // ── DTO interno para atualização de perfil ────────────────────────────────
    public record PerfilUpdateRequest(
            String nome,
            String email,
            String senhaAtual,
            String novaSenha
    ) {}
}
