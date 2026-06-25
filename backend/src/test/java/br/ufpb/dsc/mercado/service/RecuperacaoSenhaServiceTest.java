package br.ufpb.dsc.mercado.service;

import br.ufpb.dsc.mercado.domain.Papel;
import br.ufpb.dsc.mercado.domain.RecuperacaoSenha;
import br.ufpb.dsc.mercado.domain.Usuario;
import br.ufpb.dsc.mercado.repository.RecuperacaoSenhaRepository;
import br.ufpb.dsc.mercado.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecuperacaoSenhaService — Testes Unitários")
class RecuperacaoSenhaServiceTest {

    @Mock RecuperacaoSenhaRepository recuperacaoRepository;
    @Mock UsuarioRepository usuarioRepository;
    @Mock PasswordEncoder passwordEncoder;

    private RecuperacaoSenhaService service;

    @BeforeEach
    void setUp() {
        service = new RecuperacaoSenhaService(recuperacaoRepository, usuarioRepository, passwordEncoder);
    }

    private Usuario usuarioComId(Long id) {
        Usuario u = new Usuario("Ana", "ana@teste.com", "hash", Papel.CLIENTE);
        u.setId(id);
        return u;
    }

    @Test
    @DisplayName("solicitarRecuperacao deve gerar token e invalidar tokens antigos quando o e-mail existe")
    void solicitarRecuperacao_emailExistente_deveGerarToken() {
        Usuario usuario = usuarioComId(1L);
        when(usuarioRepository.findByEmail("ana@teste.com")).thenReturn(Optional.of(usuario));

        String token = service.solicitarRecuperacao("ana@teste.com");

        assertThat(token).isNotNull();
        assertThat(token).doesNotContain("-"); // UUID sem hífens
        verify(recuperacaoRepository).invalidarTokensDoUsuario(1L);
        verify(recuperacaoRepository).save(any(RecuperacaoSenha.class));
    }

    @Test
    @DisplayName("solicitarRecuperacao deve retornar null quando o e-mail não existe (evita enumeração)")
    void solicitarRecuperacao_emailInexistente_deveRetornarNull() {
        when(usuarioRepository.findByEmail("naoexiste@teste.com")).thenReturn(Optional.empty());

        String token = service.solicitarRecuperacao("naoexiste@teste.com");

        assertThat(token).isNull();
        verify(recuperacaoRepository, never()).invalidarTokensDoUsuario(any());
        verify(recuperacaoRepository, never()).save(any());
    }

    @Test
    @DisplayName("redefinirSenha deve atualizar a senha e marcar o token como usado quando válido")
    void redefinirSenha_tokenValido_deveAtualizarSenha() {
        Usuario usuario = usuarioComId(1L);
        RecuperacaoSenha recuperacao = new RecuperacaoSenha(usuario, "token123", Instant.now().plus(10, ChronoUnit.MINUTES));

        when(recuperacaoRepository.findByToken("token123")).thenReturn(Optional.of(recuperacao));
        when(passwordEncoder.encode("novaSenha123")).thenReturn("hashNovo");

        service.redefinirSenha("token123", "novaSenha123");

        assertThat(usuario.getSenha()).isEqualTo("hashNovo");
        assertThat(recuperacao.isUsado()).isTrue();
        verify(usuarioRepository).save(usuario);
        verify(recuperacaoRepository).save(recuperacao);
    }

    @Test
    @DisplayName("redefinirSenha deve lançar exceção quando o token não existe")
    void redefinirSenha_tokenInexistente_deveLancarExcecao() {
        when(recuperacaoRepository.findByToken("invalido")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.redefinirSenha("invalido", "novaSenha123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Token inválido ou expirado");

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("redefinirSenha deve lançar exceção quando o token já foi usado")
    void redefinirSenha_tokenJaUsado_deveLancarExcecao() {
        Usuario usuario = usuarioComId(1L);
        RecuperacaoSenha recuperacao = new RecuperacaoSenha(usuario, "token123", Instant.now().plus(10, ChronoUnit.MINUTES));
        recuperacao.setUsado(true);

        when(recuperacaoRepository.findByToken("token123")).thenReturn(Optional.of(recuperacao));

        assertThatThrownBy(() -> service.redefinirSenha("token123", "novaSenha123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Token inválido ou expirado");

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("redefinirSenha deve lançar exceção quando o token expirou")
    void redefinirSenha_tokenExpirado_deveLancarExcecao() {
        Usuario usuario = usuarioComId(1L);
        RecuperacaoSenha recuperacao = new RecuperacaoSenha(usuario, "token123", Instant.now().minus(1, ChronoUnit.MINUTES));

        when(recuperacaoRepository.findByToken("token123")).thenReturn(Optional.of(recuperacao));

        assertThatThrownBy(() -> service.redefinirSenha("token123", "novaSenha123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Token inválido ou expirado");

        verify(usuarioRepository, never()).save(any());
    }
}
