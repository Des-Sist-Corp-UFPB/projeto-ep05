package br.ufpb.dsc.mercado.service;

import br.ufpb.dsc.mercado.domain.*;
import br.ufpb.dsc.mercado.dto.*;
import br.ufpb.dsc.mercado.repository.CartaoRepository;
import br.ufpb.dsc.mercado.repository.EnderecoRepository;
import br.ufpb.dsc.mercado.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class UsuarioService {

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

    public Usuario buscarPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado com ID: " + id));
    }

    public Usuario buscarPorEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado com e-mail: " + email));
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

    @Transactional
    public Usuario cadastrar(CadastroRequest request) {
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Já existe um usuário cadastrado com este e-mail");
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
    public Usuario atualizarPerfil(Long id, String nome, String email, String senhaAtual, String novaSenha) {
        Usuario usuario = buscarPorId(id);

        if (!usuario.getEmail().equalsIgnoreCase(email) && usuarioRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Este e-mail já está em uso por outro usuário");
        }

        usuario.setNome(nome);
        usuario.setEmail(email);

        if (senhaAtual != null && !senhaAtual.isBlank() && novaSenha != null && !novaSenha.isBlank()) {
            if (!passwordEncoder.matches(senhaAtual, usuario.getSenha())) {
                throw new IllegalArgumentException("Senha atual incorreta");
            }
            usuario.setSenha(passwordEncoder.encode(novaSenha));
        }

        return usuarioRepository.save(usuario);
    }

    @Transactional
    public void alternarStatus(Long id) {
        Usuario usuario = buscarPorId(id);
        if (usuario.getPapel() == Papel.SYSADMIN) {
            throw new IllegalArgumentException("Não é possível bloquear um administrador do sistema (SYSADMIN)");
        }
        usuario.setStatus(usuario.getStatus() == StatusUsuario.ATIVO ? StatusUsuario.BLOQUEADO : StatusUsuario.ATIVO);
        usuarioRepository.save(usuario);
    }

    @Transactional
    public void excluirConta(Long id) {
        Usuario usuario = buscarPorId(id);
        usuarioRepository.delete(usuario);
    }

    // === ENDEREÇOS ===
    public List<Endereco> listarEnderecos(Long clienteId) {
        return enderecoRepository.findByClienteIdOrderByCriadoEmDesc(clienteId);
    }

    public Endereco buscarEnderecoPorId(Long enderecoId) {
        return enderecoRepository.findById(enderecoId)
                .orElseThrow(() -> new IllegalArgumentException("Endereço não encontrado com ID: " + enderecoId));
    }

    @Transactional
    public Endereco cadastrarEndereco(Long clienteId, EnderecoDTO dto) {
        Usuario cliente = buscarPorId(clienteId);
        List<Endereco> enderecosExistentes = enderecoRepository.findByClienteIdOrderByCriadoEmDesc(clienteId);

        Endereco endereco = new Endereco();
        endereco.setCliente(cliente);
        endereco.setLogradouro(dto.logradouro());
        endereco.setNumero(dto.numero());
        endereco.setComplemento(dto.complemento());
        endereco.setBairro(dto.bairro());
        endereco.setCidade(dto.cidade());
        endereco.setEstado(dto.estado());
        endereco.setCep(dto.cep());
        
        // Se for o primeiro endereço ou principal for true, marque como principal e desmarque os outros
        boolean serPrincipal = enderecosExistentes.isEmpty() || dto.principal();
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
            throw new IllegalArgumentException("Este endereço não pertence a este cliente");
        }
        
        boolean eraPrincipal = endereco.getPrincipal();
        enderecoRepository.delete(endereco);

        if (eraPrincipal) {
            // Define o endereço mais recente como principal
            List<Endereco> restantes = enderecoRepository.findByClienteIdOrderByCriadoEmDesc(clienteId);
            if (!restantes.isEmpty()) {
                Endereco novoPrincipal = restantes.get(0);
                novoPrincipal.setPrincipal(true);
                enderecoRepository.save(novoPrincipal);
            }
        }
    }

    private void desmarcarEnderecosPrincipais(Long clienteId) {
        List<Endereco> principals = enderecoRepository.findByClienteIdOrderByCriadoEmDesc(clienteId);
        for (Endereco e : principals) {
            if (e.getPrincipal()) {
                e.setPrincipal(false);
                enderecoRepository.save(e);
            }
        }
    }

    // === CARTÕES ===
    public List<Cartao> listarCartoes(Long clienteId) {
        return cartaoRepository.findByClienteId(clienteId);
    }

    public Cartao buscarCartaoPorId(Long cartaoId) {
        return cartaoRepository.findById(cartaoId)
                .orElseThrow(() -> new IllegalArgumentException("Cartão não encontrado com ID: " + cartaoId));
    }

    @Transactional
    public Cartao cadastrarCartao(Long clienteId, CartaoSalvarDTO dto) {
        Usuario cliente = buscarPorId(clienteId);

        Cartao cartao = new Cartao();
        cartao.setCliente(cliente);
        cartao.setNomeTitular(dto.nomeTitular());
        cartao.setBandeira(dto.bandeira());
        
        // Pega os 4 últimos dígitos do número do cartão
        String numStr = dto.numeroCartao().trim();
        String ultimos = numStr.substring(numStr.length() - 4);
        cartao.setQuatroUltimosDigitos(ultimos);
        
        // Simula a geração de um token seguro
        cartao.setTokenPagamento("TOK_" + UUID.randomUUID().toString().replace("-", "").toUpperCase());
        cartao.setDataExpiracao(dto.dataExpiracao());

        return cartaoRepository.save(cartao);
    }

    @Transactional
    public void removerCartao(Long clienteId, Long cartaoId) {
        Cartao cartao = buscarCartaoPorId(cartaoId);
        if (!cartao.getCliente().getId().equals(clienteId)) {
            throw new IllegalArgumentException("Este cartão não pertence a este cliente");
        }
        cartaoRepository.delete(cartao);
    }
}
