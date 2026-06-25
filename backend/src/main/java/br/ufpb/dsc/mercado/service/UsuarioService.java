package br.ufpb.dsc.mercado.service;

import br.ufpb.dsc.mercado.domain.*;
import br.ufpb.dsc.mercado.dto.*;
import br.ufpb.dsc.mercado.dto.CadastroClienteRequest;
import br.ufpb.dsc.mercado.exception.ApiException;
import br.ufpb.dsc.mercado.repository.CartaoRepository;
import br.ufpb.dsc.mercado.repository.EnderecoRepository;
import br.ufpb.dsc.mercado.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Serviço de usuários refatorado.
 *
 * MUDANÇAS EM RELAÇÃO À VERSÃO ANTERIOR:
 * ─────────────────────────────────────
 * 1. IllegalArgumentException → ApiException tipada (naoEncontrado, conflito…)
 *    O GlobalExceptionHandler já captura ApiException e retorna o HTTP correto.
 *    Controllers deixam de ter try/catch manual.
 *
 * 2. Métodos de endereço e cartão continuam iguais, apenas exceções tipadas.
 */
@Service
@Transactional(readOnly = true)
public class UsuarioService {

    // ── Token store em memória ────────────────────────────────────────────────
    // Chave: token UUID  |  Valor: email + expiry
    private record TokenEntry(String email, Instant expiry) {}
    private final Map<String, TokenEntry> tokenStore = new ConcurrentHashMap<>();
    private static final long TOKEN_TTL_SECONDS = 30 * 60; // 30 minutos

    // ── Dependências ─────────────────────────────────────────────────────────
    private final UsuarioRepository usuarioRepository;
    private final EnderecoRepository enderecoRepository;
    private final CartaoRepository cartaoRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          EnderecoRepository enderecoRepository,
                          CartaoRepository cartaoRepository,
                          PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.enderecoRepository = enderecoRepository;
        this.cartaoRepository = cartaoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ── Consultas básicas ─────────────────────────────────────────────────────

    public Usuario buscarPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> ApiException.naoEncontrado("Usuário não encontrado com ID: " + id));
    }

    public Usuario buscarPorEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.naoEncontrado("E-mail não cadastrado"));
    }

    public Page<Usuario> listarPorPapel(Papel papel, String busca, Pageable pageable) {
        if (busca != null && !busca.isBlank()) {
            return usuarioRepository.findByPapelAndNomeContainingIgnoreCase(papel, busca, pageable);
        }
        return usuarioRepository.findByPapel(papel, pageable);
    }

    public List<Usuario> listarTodosPorPapel(Papel papel) {
        return usuarioRepository.findByPapel(papel);
    }

    // ── Cadastro ──────────────────────────────────────────────────────────────

    @Transactional
    public Usuario cadastrar(CadastroRequest request) {
        if (usuarioRepository.existsByEmail(request.email())) {
            throw ApiException.conflito("Já existe uma conta com este e-mail");
        }

        Usuario usuario = new Usuario(
                request.nome(),
                request.email(),
                passwordEncoder.encode(request.senha()),
                request.papel()
        );

        return usuarioRepository.save(usuario);
    }

    @Transactional
    public Usuario cadastrarCliente(CadastroClienteRequest request) {
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Já existe uma conta com este e-mail");
        }
        if (usuarioRepository.existsByCpf(request.cpf())) {
            throw new IllegalArgumentException("Já existe uma conta com este CPF");
        }
        if (!request.senhasConferem()) {
            throw new IllegalArgumentException("As senhas não conferem");
        }
        Usuario usuario = new Usuario(
                request.nome() + " " + request.sobrenome(),
                request.email(),
                passwordEncoder.encode(request.senha()),
                Papel.CLIENTE
        );
        usuario.setSobrenome(request.sobrenome());
        usuario.setCpf(request.cpf());
        usuario.setTelefone(request.telefone());
        usuario.setDataNascimento(request.dataNascimento());
        return usuarioRepository.save(usuario);
    }

    // ── Atualização de perfil ─────────────────────────────────────────────────

    @Transactional
    public Usuario atualizarPerfil(Long id, String nome, String email,
                                   String senhaAtual, String novaSenha) {
        Usuario usuario = buscarPorId(id);

        if (!usuario.getEmail().equalsIgnoreCase(email)
                && usuarioRepository.existsByEmail(email)) {
            throw ApiException.conflito("Este e-mail já está em uso por outro usuário");
        }

        usuario.setNome(nome);
        usuario.setEmail(email);

        if (senhaAtual != null && !senhaAtual.isBlank()
                && novaSenha != null && !novaSenha.isBlank()) {
            if (!passwordEncoder.matches(senhaAtual, usuario.getSenha())) {
                throw ApiException.requisicaoInvalida("Senha atual incorreta");
            }
            usuario.setSenha(passwordEncoder.encode(novaSenha));
        }

        return usuarioRepository.save(usuario);
    }

    /**
     * Atualiza um administrador pelo SysAdmin (sem exigir senha atual).
     * Se novaSenha for nula ou vazia, a senha existente é mantida.
     */
    @Transactional
    public Usuario atualizarAdmin(Long id, String nome, String email, String novaSenha) {
        Usuario usuario = buscarPorId(id);

        if (!usuario.getEmail().equalsIgnoreCase(email)
                && usuarioRepository.existsByEmailAndIdNot(email, id)) {
            throw ApiException.conflito("Este e-mail já está em uso por outro usuário");
        }

        usuario.setNome(nome);
        usuario.setEmail(email);

        if (novaSenha != null && !novaSenha.isBlank()) {
            usuario.setSenha(passwordEncoder.encode(novaSenha));
        }

        return usuarioRepository.save(usuario);
    }

    // ── Administração ─────────────────────────────────────────────────────────

    @Transactional
    public void alternarStatus(Long id) {
        Usuario usuario = buscarPorId(id);
        if (usuario.getPapel() == Papel.SYSADMIN) {
            throw ApiException.proibido("Não é possível bloquear um SYSADMIN");
        }
        StatusUsuario novoStatus = usuario.getStatus() == StatusUsuario.ATIVO
                ? StatusUsuario.BLOQUEADO
                : StatusUsuario.ATIVO;
        usuario.setStatus(novoStatus);
        usuarioRepository.save(usuario);
    }

    @Transactional
    public void excluirConta(Long id) {
        Usuario usuario = buscarPorId(id);
        usuarioRepository.delete(usuario);
    }

    // ── Endereços ─────────────────────────────────────────────────────────────

    public List<Endereco> listarEnderecos(Long clienteId) {
        return enderecoRepository.findByClienteIdOrderByCriadoEmDesc(clienteId);
    }

    public Endereco buscarEnderecoPorId(Long enderecoId) {
        return enderecoRepository.findById(enderecoId)
                .orElseThrow(() -> ApiException.naoEncontrado("Endereço não encontrado com ID: " + enderecoId));
    }

    @Transactional
    public Endereco cadastrarEndereco(Long clienteId, EnderecoDTO dto) {
        Usuario cliente = buscarPorId(clienteId);
        List<Endereco> existentes = enderecoRepository.findByClienteIdOrderByCriadoEmDesc(clienteId);

        Endereco endereco = new Endereco();
        endereco.setCliente(cliente);
        endereco.setLogradouro(dto.logradouro());
        endereco.setNumero(dto.numero());
        endereco.setComplemento(dto.complemento());
        endereco.setBairro(dto.bairro());
        endereco.setCidade(dto.cidade());
        endereco.setEstado(dto.estado());
        endereco.setCep(dto.cep());

        boolean serPrincipal = existentes.isEmpty() || dto.principal();
        endereco.setPrincipal(serPrincipal);

        if (serPrincipal) {
            desmarcarEnderecosPrincipais(clienteId);
        }

        return enderecoRepository.save(endereco);
    }

    @Transactional
    public void removerEndereco(Long clienteId, Long enderecoId) {
        Endereco endereco = buscarEnderecoPorId(enderecoId);
        if (!endereco.getCliente().getId().equals(clienteId)) {
            throw ApiException.proibido("Este endereço não pertence a este cliente");
        }

        boolean eraPrincipal = endereco.getPrincipal();
        enderecoRepository.delete(endereco);

        if (eraPrincipal) {
            List<Endereco> restantes = enderecoRepository.findByClienteIdOrderByCriadoEmDesc(clienteId);
            if (!restantes.isEmpty()) {
                Endereco novoPrincipal = restantes.get(0);
                novoPrincipal.setPrincipal(true);
                enderecoRepository.save(novoPrincipal);
            }
        }
    }

    private void desmarcarEnderecosPrincipais(Long clienteId) {
        enderecoRepository.findByClienteIdOrderByCriadoEmDesc(clienteId).stream()
                .filter(Endereco::getPrincipal)
                .forEach(e -> {
                    e.setPrincipal(false);
                    enderecoRepository.save(e);
                });
    }

    // ── Cartões ───────────────────────────────────────────────────────────────

    public List<Cartao> listarCartoes(Long clienteId) {
        return cartaoRepository.findByClienteId(clienteId);
    }

    public Cartao buscarCartaoPorId(Long cartaoId) {
        return cartaoRepository.findById(cartaoId)
                .orElseThrow(() -> ApiException.naoEncontrado("Cartão não encontrado com ID: " + cartaoId));
    }

    @Transactional
    public Cartao cadastrarCartao(Long clienteId, CartaoSalvarDTO dto) {
        Usuario cliente = buscarPorId(clienteId);

        Cartao cartao = new Cartao();
        cartao.setCliente(cliente);
        cartao.setNomeTitular(dto.nomeTitular());
        cartao.setBandeira(dto.bandeira());

        String numStr = dto.numeroCartao().replaceAll("\\s+", "");
        cartao.setQuatroUltimosDigitos(numStr.substring(numStr.length() - 4));
        cartao.setTokenPagamento("TOK_" + UUID.randomUUID().toString().replace("-", "").toUpperCase());
        cartao.setDataExpiracao(dto.dataExpiracao());

        return cartaoRepository.save(cartao);
    }

    @Transactional
    public void removerCartao(Long clienteId, Long cartaoId) {
        Cartao cartao = buscarCartaoPorId(cartaoId);
        if (!cartao.getCliente().getId().equals(clienteId)) {
            throw ApiException.proibido("Este cartão não pertence a este cliente");
        }
        cartaoRepository.delete(cartao);
    }
}
