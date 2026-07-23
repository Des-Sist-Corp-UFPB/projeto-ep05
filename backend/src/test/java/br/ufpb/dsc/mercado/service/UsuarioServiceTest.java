package br.ufpb.dsc.mercado.service;

import br.ufpb.dsc.mercado.domain.Cartao;
import br.ufpb.dsc.mercado.domain.Endereco;
import br.ufpb.dsc.mercado.domain.Papel;
import br.ufpb.dsc.mercado.domain.StatusUsuario;
import br.ufpb.dsc.mercado.domain.Usuario;
import br.ufpb.dsc.mercado.dto.CadastroClienteRequest;
import br.ufpb.dsc.mercado.dto.CadastroRequest;
import br.ufpb.dsc.mercado.dto.CartaoSalvarDTO;
import br.ufpb.dsc.mercado.dto.EnderecoDTO;
import br.ufpb.dsc.mercado.exception.ApiException;
import br.ufpb.dsc.mercado.repository.CartaoRepository;
import br.ufpb.dsc.mercado.repository.EnderecoRepository;
import br.ufpb.dsc.mercado.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UsuarioService — Testes Unitários")
class UsuarioServiceTest {

    @Mock UsuarioRepository usuarioRepository;
    @Mock EnderecoRepository enderecoRepository;
    @Mock CartaoRepository cartaoRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock MercadoPagoService mercadoPagoService;

    private UsuarioService service;

    @BeforeEach
    void setUp() {
        service = new UsuarioService(usuarioRepository, enderecoRepository, cartaoRepository, passwordEncoder, mercadoPagoService);
    }

    private Usuario criarUsuario(Long id, String email, Papel papel) {
        Usuario u = new Usuario("Nome Teste", email, "hash", papel);
        u.setId(id);
        return u;
    }

    // ── buscarPorId ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("buscarPorId: deve retornar usuário existente")
    void buscarPorId_existente_deveRetornar() {
        Usuario u = criarUsuario(1L, "a@b.com", Papel.CLIENTE);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(u));

        assertThat(service.buscarPorId(1L)).isSameAs(u);
    }

    @Test
    @DisplayName("buscarPorId: deve lançar ApiException para ID inexistente")
    void buscarPorId_inexistente_deveLancarApiException() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.buscarPorId(99L)).isInstanceOf(ApiException.class);
    }

    // ── buscarPorEmail ────────────────────────────────────────────────────────

    @Test
    @DisplayName("buscarPorEmail: deve retornar usuário existente")
    void buscarPorEmail_existente_deveRetornar() {
        Usuario u = criarUsuario(1L, "a@b.com", Papel.ADMIN);
        when(usuarioRepository.findByEmail("a@b.com")).thenReturn(Optional.of(u));
        assertThat(service.buscarPorEmail("a@b.com")).isSameAs(u);
    }

    @Test
    @DisplayName("buscarPorEmail: e-mail inexistente deve lançar ApiException")
    void buscarPorEmail_inexistente_deveLancarApiException() {
        when(usuarioRepository.findByEmail(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.buscarPorEmail("x@y.com")).isInstanceOf(ApiException.class);
    }

    // ── cadastrar ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("cadastrar: e-mail novo deve salvar usuário")
    void cadastrar_emailNovo_deveSalvar() {
        when(usuarioRepository.existsByEmail("new@b.com")).thenReturn(false);
        when(passwordEncoder.encode("senha123")).thenReturn("hash123");
        Usuario saved = criarUsuario(2L, "new@b.com", Papel.ADMIN);
        when(usuarioRepository.save(any())).thenReturn(saved);

        CadastroRequest req = new CadastroRequest("Nome", "new@b.com", "senha123", Papel.ADMIN);
        Usuario result = service.cadastrar(req);

        assertThat(result).isSameAs(saved);
        verify(usuarioRepository).save(any());
    }

    @Test
    @DisplayName("cadastrar: e-mail duplicado deve lançar ApiException")
    void cadastrar_emailDuplicado_deveLancarApiException() {
        when(usuarioRepository.existsByEmail("dup@b.com")).thenReturn(true);
        CadastroRequest req = new CadastroRequest("Nome", "dup@b.com", "senha123", Papel.CLIENTE);
        assertThatThrownBy(() -> service.cadastrar(req)).isInstanceOf(ApiException.class);
        verify(usuarioRepository, never()).save(any());
    }

    // ── atualizarPerfil ───────────────────────────────────────────────────────

    @Test
    @DisplayName("atualizarPerfil: mesmo e-mail deve salvar sem verificar duplicata de outro")
    void atualizarPerfil_mesmoEmail_deveSalvar() {
        Usuario u = criarUsuario(1L, "a@b.com", Papel.ADMIN);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(u));
        when(usuarioRepository.save(any())).thenReturn(u);

        service.atualizarPerfil(1L, "Novo Nome", "a@b.com", null, null);

        verify(usuarioRepository).save(u);
        assertThat(u.getNome()).isEqualTo("Novo Nome");
    }

    @Test
    @DisplayName("atualizarPerfil: e-mail novo já em uso deve lançar ApiException")
    void atualizarPerfil_emailEmUso_deveLancarApiException() {
        Usuario u = criarUsuario(1L, "a@b.com", Papel.ADMIN);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(u));
        when(usuarioRepository.existsByEmail("outro@b.com")).thenReturn(true);

        assertThatThrownBy(() -> service.atualizarPerfil(1L, "N", "outro@b.com", null, null))
                .isInstanceOf(ApiException.class);
    }

    @Test
    @DisplayName("atualizarPerfil: senha atual errada deve lançar ApiException")
    void atualizarPerfil_senhaAtualErrada_deveLancarApiException() {
        Usuario u = criarUsuario(1L, "a@b.com", Papel.CLIENTE);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("errada", "hash")).thenReturn(false);

        assertThatThrownBy(() ->
                service.atualizarPerfil(1L, "N", "a@b.com", "errada", "nova123"))
                .isInstanceOf(ApiException.class);
    }

    // ── alternarStatus ────────────────────────────────────────────────────────

    @Test
    @DisplayName("alternarStatus: ATIVO → BLOQUEADO")
    void alternarStatus_ativo_deveBloquear() {
        Usuario u = criarUsuario(1L, "a@b.com", Papel.ADMIN);
        u.setStatus(StatusUsuario.ATIVO);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(u));
        when(usuarioRepository.save(any())).thenReturn(u);

        service.alternarStatus(1L);

        assertThat(u.getStatus()).isEqualTo(StatusUsuario.BLOQUEADO);
    }

    @Test
    @DisplayName("alternarStatus: BLOQUEADO → ATIVO")
    void alternarStatus_bloqueado_deveAtivar() {
        Usuario u = criarUsuario(1L, "a@b.com", Papel.ADMIN);
        u.setStatus(StatusUsuario.BLOQUEADO);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(u));
        when(usuarioRepository.save(any())).thenReturn(u);

        service.alternarStatus(1L);

        assertThat(u.getStatus()).isEqualTo(StatusUsuario.ATIVO);
    }

    @Test
    @DisplayName("alternarStatus: SYSADMIN não pode ser bloqueado")
    void alternarStatus_sysadmin_deveLancarApiException() {
        Usuario u = criarUsuario(1L, "sys@b.com", Papel.SYSADMIN);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(u));

        assertThatThrownBy(() -> service.alternarStatus(1L)).isInstanceOf(ApiException.class);
    }

    // ── excluirConta ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("excluirConta: deve chamar delete")
    void excluirConta_deveExcluir() {
        Usuario u = criarUsuario(1L, "a@b.com", Papel.CLIENTE);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(u));

        service.excluirConta(1L);

        verify(usuarioRepository).delete(u);
    }

    // ── listarPorPapel ────────────────────────────────────────────────────────

    @Test
    @DisplayName("listarPorPapel: com busca em branco deve usar findByPapel paginado")
    void listarPorPapel_semBusca_deveUsarFindByPapel() {
        Pageable pageable = Pageable.unpaged();
        Page<Usuario> page = new PageImpl<>(List.of(criarUsuario(1L, "a@b.com", Papel.CLIENTE)));
        when(usuarioRepository.findByPapel(Papel.CLIENTE, pageable)).thenReturn(page);

        Page<Usuario> result = service.listarPorPapel(Papel.CLIENTE, "  ", pageable);

        assertThat(result).isSameAs(page);
        verify(usuarioRepository).findByPapel(Papel.CLIENTE, pageable);
        verify(usuarioRepository, never()).findByPapelAndNomeContainingIgnoreCase(any(), any(), any());
    }

    @Test
    @DisplayName("listarPorPapel: com busca preenchida deve filtrar por nome")
    void listarPorPapel_comBusca_deveFiltrarPorNome() {
        Pageable pageable = Pageable.unpaged();
        Page<Usuario> page = new PageImpl<>(List.of(criarUsuario(1L, "a@b.com", Papel.ADMIN)));
        when(usuarioRepository.findByPapelAndNomeContainingIgnoreCase(Papel.ADMIN, "ana", pageable))
                .thenReturn(page);

        Page<Usuario> result = service.listarPorPapel(Papel.ADMIN, "ana", pageable);

        assertThat(result).isSameAs(page);
        verify(usuarioRepository).findByPapelAndNomeContainingIgnoreCase(Papel.ADMIN, "ana", pageable);
    }

    @Test
    @DisplayName("listarTodosPorPapel: deve delegar ao repositório")
    void listarTodosPorPapel_deveDelegar() {
        List<Usuario> usuarios = List.of(criarUsuario(1L, "a@b.com", Papel.CLIENTE));
        when(usuarioRepository.findByPapel(Papel.CLIENTE)).thenReturn(usuarios);

        assertThat(service.listarTodosPorPapel(Papel.CLIENTE)).isSameAs(usuarios);
    }

    // ── cadastrarCliente ──────────────────────────────────────────────────────

    private CadastroClienteRequest criarRequestCliente(String email, String cpf, String senha, String confirmacao) {
        return new CadastroClienteRequest("Nome", "Sobrenome", email, cpf, "(83) 99999-9999",
                LocalDate.of(2000, 1, 1), senha, confirmacao);
    }

    @Test
    @DisplayName("cadastrarCliente: dados válidos deve salvar cliente")
    void cadastrarCliente_valido_deveSalvar() {
        CadastroClienteRequest req = criarRequestCliente("c@b.com", "111.222.333-44", "Senha123", "Senha123");
        when(usuarioRepository.existsByEmail("c@b.com")).thenReturn(false);
        when(usuarioRepository.existsByCpf("111.222.333-44")).thenReturn(false);
        when(passwordEncoder.encode("Senha123")).thenReturn("hashSenha");
        Usuario salvo = criarUsuario(5L, "c@b.com", Papel.CLIENTE);
        when(usuarioRepository.save(any())).thenReturn(salvo);

        Usuario result = service.cadastrarCliente(req);

        assertThat(result).isSameAs(salvo);
        verify(usuarioRepository).save(argThat(u ->
                u.getNome().equals("Nome Sobrenome")
                        && u.getCpf().equals("111.222.333-44")
                        && u.getSobrenome().equals("Sobrenome")
                        && u.getPapel() == Papel.CLIENTE));
    }

    @Test
    @DisplayName("cadastrarCliente: e-mail duplicado deve lançar IllegalArgumentException")
    void cadastrarCliente_emailDuplicado_deveLancarExcecao() {
        CadastroClienteRequest req = criarRequestCliente("c@b.com", "111.222.333-44", "Senha123", "Senha123");
        when(usuarioRepository.existsByEmail("c@b.com")).thenReturn(true);

        assertThatThrownBy(() -> service.cadastrarCliente(req))
                .isInstanceOf(IllegalArgumentException.class);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("cadastrarCliente: CPF duplicado deve lançar IllegalArgumentException")
    void cadastrarCliente_cpfDuplicado_deveLancarExcecao() {
        CadastroClienteRequest req = criarRequestCliente("c@b.com", "111.222.333-44", "Senha123", "Senha123");
        when(usuarioRepository.existsByEmail("c@b.com")).thenReturn(false);
        when(usuarioRepository.existsByCpf("111.222.333-44")).thenReturn(true);

        assertThatThrownBy(() -> service.cadastrarCliente(req))
                .isInstanceOf(IllegalArgumentException.class);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("cadastrarCliente: senhas diferentes deve lançar IllegalArgumentException")
    void cadastrarCliente_senhasDiferentes_deveLancarExcecao() {
        CadastroClienteRequest req = criarRequestCliente("c@b.com", "111.222.333-44", "Senha123", "Outra123");
        when(usuarioRepository.existsByEmail("c@b.com")).thenReturn(false);
        when(usuarioRepository.existsByCpf("111.222.333-44")).thenReturn(false);

        assertThatThrownBy(() -> service.cadastrarCliente(req))
                .isInstanceOf(IllegalArgumentException.class);
        verify(usuarioRepository, never()).save(any());
    }

    // ── atualizarPerfil (troca de senha) ─────────────────────────────────────

    @Test
    @DisplayName("atualizarPerfil: senha atual correta deve trocar a senha")
    void atualizarPerfil_senhaCorreta_deveTrocarSenha() {
        Usuario u = criarUsuario(1L, "a@b.com", Papel.CLIENTE);
        u.setSenha("hashAntiga");
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("antiga", "hashAntiga")).thenReturn(true);
        when(passwordEncoder.encode("nova123")).thenReturn("hashNova");
        when(usuarioRepository.save(any())).thenReturn(u);

        service.atualizarPerfil(1L, "Nome", "a@b.com", "antiga", "nova123");

        assertThat(u.getSenha()).isEqualTo("hashNova");
    }

    // ── atualizarAdmin ────────────────────────────────────────────────────────

    @Test
    @DisplayName("atualizarAdmin: e-mail novo já usado por outro deve lançar ApiException")
    void atualizarAdmin_emailEmUsoPorOutro_deveLancarApiException() {
        Usuario u = criarUsuario(1L, "admin@b.com", Papel.ADMIN);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(u));
        when(usuarioRepository.existsByEmailAndIdNot("novo@b.com", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.atualizarAdmin(1L, "Nome", "novo@b.com", null))
                .isInstanceOf(ApiException.class);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("atualizarAdmin: sem nova senha deve manter senha existente")
    void atualizarAdmin_semNovaSenha_deveManterSenha() {
        Usuario u = criarUsuario(1L, "admin@b.com", Papel.ADMIN);
        u.setSenha("hashAtual");
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(u));
        when(usuarioRepository.save(any())).thenReturn(u);

        service.atualizarAdmin(1L, "Novo Nome", "admin@b.com", "  ");

        assertThat(u.getSenha()).isEqualTo("hashAtual");
        assertThat(u.getNome()).isEqualTo("Novo Nome");
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    @DisplayName("atualizarAdmin: com nova senha deve atualizar a senha")
    void atualizarAdmin_comNovaSenha_deveAtualizarSenha() {
        Usuario u = criarUsuario(1L, "admin@b.com", Papel.ADMIN);
        u.setSenha("hashAntiga");
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(u));
        when(passwordEncoder.encode("novaSenha")).thenReturn("hashNova");
        when(usuarioRepository.save(any())).thenReturn(u);

        service.atualizarAdmin(1L, "Nome", "admin@b.com", "novaSenha");

        assertThat(u.getSenha()).isEqualTo("hashNova");
    }

    // ── Endereços ─────────────────────────────────────────────────────────────

    private EnderecoDTO criarEnderecoDTO(boolean principal) {
        return new EnderecoDTO(null, "Rua A", "100", "Apto 1", "Centro", "Cidade", "PB", "58000-000", principal);
    }

    @Test
    @DisplayName("listarEnderecos: deve delegar ao repositório")
    void listarEnderecos_deveDelegar() {
        List<Endereco> enderecos = List.of(new Endereco());
        when(enderecoRepository.findByClienteIdOrderByCriadoEmDesc(1L)).thenReturn(enderecos);

        assertThat(service.listarEnderecos(1L)).isSameAs(enderecos);
    }

    @Test
    @DisplayName("buscarEnderecoPorId: existente deve retornar")
    void buscarEnderecoPorId_existente_deveRetornar() {
        Endereco e = new Endereco();
        when(enderecoRepository.findById(10L)).thenReturn(Optional.of(e));

        assertThat(service.buscarEnderecoPorId(10L)).isSameAs(e);
    }

    @Test
    @DisplayName("buscarEnderecoPorId: inexistente deve lançar ApiException")
    void buscarEnderecoPorId_inexistente_deveLancarApiException() {
        when(enderecoRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.buscarEnderecoPorId(99L)).isInstanceOf(ApiException.class);
    }

    @Test
    @DisplayName("cadastrarEndereco: primeiro endereço deve ser marcado como principal")
    void cadastrarEndereco_primeiroEndereco_deveSerPrincipal() {
        Usuario cliente = criarUsuario(1L, "a@b.com", Papel.CLIENTE);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(enderecoRepository.findByClienteIdOrderByCriadoEmDesc(1L)).thenReturn(List.of());
        when(enderecoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Endereco result = service.cadastrarEndereco(1L, criarEnderecoDTO(false));

        assertThat(result.getPrincipal()).isTrue();
    }

    @Test
    @DisplayName("cadastrarEndereco: endereço adicional marcado como principal deve desmarcar os demais")
    void cadastrarEndereco_marcadoPrincipal_deveDesmarcarOutros() {
        Usuario cliente = criarUsuario(1L, "a@b.com", Papel.CLIENTE);
        Endereco existente = new Endereco();
        existente.setPrincipal(true);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(enderecoRepository.findByClienteIdOrderByCriadoEmDesc(1L)).thenReturn(List.of(existente));
        when(enderecoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Endereco result = service.cadastrarEndereco(1L, criarEnderecoDTO(true));

        assertThat(result.getPrincipal()).isTrue();
        assertThat(existente.getPrincipal()).isFalse();
        verify(enderecoRepository, times(2)).save(any());
    }

    @Test
    @DisplayName("cadastrarEndereco: endereço adicional não principal não deve mexer nos demais")
    void cadastrarEndereco_naoPrincipal_naoDeveDesmarcarOutros() {
        Usuario cliente = criarUsuario(1L, "a@b.com", Papel.CLIENTE);
        Endereco existente = new Endereco();
        existente.setPrincipal(true);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(enderecoRepository.findByClienteIdOrderByCriadoEmDesc(1L)).thenReturn(List.of(existente));
        when(enderecoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Endereco result = service.cadastrarEndereco(1L, criarEnderecoDTO(false));

        assertThat(result.getPrincipal()).isFalse();
        assertThat(existente.getPrincipal()).isTrue();
        verify(enderecoRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("removerEndereco: endereço de outro cliente deve lançar ApiException")
    void removerEndereco_deOutroCliente_deveLancarApiException() {
        Usuario outroCliente = criarUsuario(2L, "outro@b.com", Papel.CLIENTE);
        Endereco endereco = new Endereco();
        endereco.setCliente(outroCliente);
        when(enderecoRepository.findById(10L)).thenReturn(Optional.of(endereco));

        assertThatThrownBy(() -> service.removerEndereco(1L, 10L)).isInstanceOf(ApiException.class);
        verify(enderecoRepository, never()).delete(any());
    }

    @Test
    @DisplayName("removerEndereco: principal com restantes deve promover o próximo a principal")
    void removerEndereco_principalComRestantes_devePromoverProximo() {
        Usuario cliente = criarUsuario(1L, "a@b.com", Papel.CLIENTE);
        Endereco principal = new Endereco();
        principal.setId(10L);
        principal.setCliente(cliente);
        principal.setPrincipal(true);
        Endereco restante = new Endereco();
        restante.setId(11L);
        restante.setPrincipal(false);

        when(enderecoRepository.findById(10L)).thenReturn(Optional.of(principal));
        when(enderecoRepository.findByClienteIdOrderByCriadoEmDesc(1L)).thenReturn(List.of(restante));

        service.removerEndereco(1L, 10L);

        verify(enderecoRepository).delete(principal);
        assertThat(restante.getPrincipal()).isTrue();
        verify(enderecoRepository).save(restante);
    }

    @Test
    @DisplayName("removerEndereco: não principal não deve promover nada")
    void removerEndereco_naoPrincipal_naoDevePromoverNada() {
        Usuario cliente = criarUsuario(1L, "a@b.com", Papel.CLIENTE);
        Endereco endereco = new Endereco();
        endereco.setId(10L);
        endereco.setCliente(cliente);
        endereco.setPrincipal(false);
        when(enderecoRepository.findById(10L)).thenReturn(Optional.of(endereco));

        service.removerEndereco(1L, 10L);

        verify(enderecoRepository).delete(endereco);
        verify(enderecoRepository, never()).save(any());
    }

    @Test
    @DisplayName("removerEndereco: principal sem restantes não deve promover nada")
    void removerEndereco_principalSemRestantes_naoDevePromoverNada() {
        Usuario cliente = criarUsuario(1L, "a@b.com", Papel.CLIENTE);
        Endereco endereco = new Endereco();
        endereco.setId(10L);
        endereco.setCliente(cliente);
        endereco.setPrincipal(true);
        when(enderecoRepository.findById(10L)).thenReturn(Optional.of(endereco));
        when(enderecoRepository.findByClienteIdOrderByCriadoEmDesc(1L)).thenReturn(List.of());

        service.removerEndereco(1L, 10L);

        verify(enderecoRepository).delete(endereco);
        verify(enderecoRepository, never()).save(any());
    }

    // ── Cartões ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("listarCartoes: deve delegar ao repositório")
    void listarCartoes_deveDelegar() {
        List<Cartao> cartoes = List.of(new Cartao());
        when(cartaoRepository.findByClienteId(1L)).thenReturn(cartoes);

        assertThat(service.listarCartoes(1L)).isSameAs(cartoes);
    }

    @Test
    @DisplayName("buscarCartaoPorId: existente deve retornar")
    void buscarCartaoPorId_existente_deveRetornar() {
        Cartao c = new Cartao();
        when(cartaoRepository.findById(10L)).thenReturn(Optional.of(c));

        assertThat(service.buscarCartaoPorId(10L)).isSameAs(c);
    }

    @Test
    @DisplayName("buscarCartaoPorId: inexistente deve lançar ApiException")
    void buscarCartaoPorId_inexistente_deveLancarApiException() {
        when(cartaoRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.buscarCartaoPorId(99L)).isInstanceOf(ApiException.class);
    }

    @Test
    @DisplayName("cadastrarCartao: dados válidos deve salvar cartão")
    void cadastrarCartao_valido_deveSalvar() {
        Usuario cliente = criarUsuario(1L, "a@b.com", Papel.CLIENTE);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(cartaoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CartaoSalvarDTO dto = new CartaoSalvarDTO("Fulano", "VISA", "1234", "12/2030", "tok-abc");
        Cartao result = service.cadastrarCartao(1L, dto);

        assertThat(result.getNomeTitular()).isEqualTo("Fulano");
        assertThat(result.getCliente()).isSameAs(cliente);
        verify(cartaoRepository).save(any());
    }

    @Test
    @DisplayName("removerCartao: cartão de outro cliente deve lançar ApiException")
    void removerCartao_deOutroCliente_deveLancarApiException() {
        Usuario outroCliente = criarUsuario(2L, "outro@b.com", Papel.CLIENTE);
        Cartao cartao = new Cartao();
        cartao.setCliente(outroCliente);
        when(cartaoRepository.findById(10L)).thenReturn(Optional.of(cartao));

        assertThatThrownBy(() -> service.removerCartao(1L, 10L)).isInstanceOf(ApiException.class);
        verify(cartaoRepository, never()).delete(any());
    }

    @Test
    @DisplayName("removerCartao: cartão do próprio cliente deve remover")
    void removerCartao_doProprioCliente_deveRemover() {
        Usuario cliente = criarUsuario(1L, "a@b.com", Papel.CLIENTE);
        Cartao cartao = new Cartao();
        cartao.setCliente(cliente);
        when(cartaoRepository.findById(10L)).thenReturn(Optional.of(cartao));

        service.removerCartao(1L, 10L);

        verify(cartaoRepository).delete(cartao);
    }
}
