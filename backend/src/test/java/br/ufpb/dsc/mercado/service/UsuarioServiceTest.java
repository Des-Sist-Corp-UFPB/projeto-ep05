package br.ufpb.dsc.mercado.service;

import br.ufpb.dsc.mercado.domain.Papel;
import br.ufpb.dsc.mercado.domain.StatusUsuario;
import br.ufpb.dsc.mercado.domain.Usuario;
import br.ufpb.dsc.mercado.dto.CadastroRequest;
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
import org.springframework.security.crypto.password.PasswordEncoder;

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
}
