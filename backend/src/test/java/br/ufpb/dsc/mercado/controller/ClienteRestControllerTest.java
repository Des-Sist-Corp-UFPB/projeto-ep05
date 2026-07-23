package br.ufpb.dsc.mercado.controller;

import br.ufpb.dsc.mercado.domain.Cartao;
import br.ufpb.dsc.mercado.domain.Endereco;
import br.ufpb.dsc.mercado.domain.Papel;
import br.ufpb.dsc.mercado.domain.Usuario;
import br.ufpb.dsc.mercado.dto.CartaoSalvarDTO;
import br.ufpb.dsc.mercado.dto.EnderecoDTO;
import br.ufpb.dsc.mercado.service.UsuarioService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários puros do controller, instanciado diretamente com o
 * UsuarioService mockado. O controller lê o usuário logado via
 * SecurityContextHolder, então simulamos isso manualmente em cada teste.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ClienteRestController — Testes Unitários")
class ClienteRestControllerTest {

    @Mock UsuarioService usuarioService;

    private ClienteRestController controller;

    @BeforeEach
    void setUp() {
        controller = new ClienteRestController(usuarioService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Usuario usuarioLogado(Long id, String email) {
        Usuario u = new Usuario("Ana", email, "hash", Papel.CLIENTE);
        u.setId(id);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, null, List.of()));
        when(usuarioService.buscarPorEmail(email)).thenReturn(u);
        return u;
    }

    // ── perfil ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("obterPerfil deve retornar os dados do usuário autenticado, sem a senha")
    void obterPerfil_deveRetornarDadosSemSenha() {
        usuarioLogado(1L, "ana@teste.com");

        ResponseEntity<?> resposta = controller.obterPerfil();

        var body = (br.ufpb.dsc.mercado.dto.CadastroRequest) resposta.getBody();
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(body.nome()).isEqualTo("Ana");
        assertThat(body.email()).isEqualTo("ana@teste.com");
        assertThat(body.senha()).isNull();
    }

    @Test
    @DisplayName("atualizarPerfil deve delegar para o service com os dados do usuário logado")
    void atualizarPerfil_deveDelegarParaService() {
        Usuario logado = usuarioLogado(1L, "ana@teste.com");
        Usuario atualizado = new Usuario("Ana Nova", "ananova@teste.com", "hash", Papel.CLIENTE);
        atualizado.setId(1L);
        when(usuarioService.atualizarPerfil(1L, "Ana Nova", "ananova@teste.com", "atual123", "nova123"))
                .thenReturn(atualizado);

        ResponseEntity<?> resposta = controller.atualizarPerfil(
                new ClienteRestController.PerfilUpdateRequest("Ana Nova", "ananova@teste.com", "atual123", "nova123"));

        var body = (br.ufpb.dsc.mercado.dto.CadastroRequest) resposta.getBody();
        assertThat(body.nome()).isEqualTo("Ana Nova");
        assertThat(body.email()).isEqualTo("ananova@teste.com");
    }

    @Test
    @DisplayName("excluirConta deve delegar a exclusão para o service usando o id do usuário logado")
    void excluirConta_deveDelegarParaService() {
        usuarioLogado(1L, "ana@teste.com");

        ResponseEntity<Void> resposta = controller.excluirConta();

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(usuarioService).excluirConta(1L);
    }

    // ── endereços ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("listarEnderecos deve retornar os endereços do usuário logado mapeados para DTO")
    void listarEnderecos_deveMapearParaDTO() {
        usuarioLogado(1L, "ana@teste.com");
        Endereco e = new Endereco();
        e.setId(10L);
        e.setLogradouro("Rua A");
        e.setNumero("100");
        e.setBairro("Centro");
        e.setCidade("João Pessoa");
        e.setEstado("PB");
        e.setCep("58000000");
        e.setPrincipal(true);
        when(usuarioService.listarEnderecos(1L)).thenReturn(List.of(e));

        ResponseEntity<List<EnderecoDTO>> resposta = controller.listarEnderecos();

        assertThat(resposta.getBody()).hasSize(1);
        assertThat(resposta.getBody().get(0).cep()).isEqualTo("58000000");
    }

    @Test
    @DisplayName("cadastrarEndereco deve delegar ao service usando o id do usuário logado")
    void cadastrarEndereco_deveDelegarParaService() {
        usuarioLogado(1L, "ana@teste.com");
        EnderecoDTO request = new EnderecoDTO(null, "Rua B", "200", null, "Centro", "JP", "PB", "58000000", true);
        Endereco salvo = new Endereco();
        salvo.setId(20L);
        salvo.setLogradouro("Rua B");
        salvo.setNumero("200");
        salvo.setBairro("Centro");
        salvo.setCidade("JP");
        salvo.setEstado("PB");
        salvo.setCep("58000000");
        salvo.setPrincipal(true);
        when(usuarioService.cadastrarEndereco(1L, request)).thenReturn(salvo);

        ResponseEntity<EnderecoDTO> resposta = controller.cadastrarEndereco(request);

        assertThat(resposta.getBody().id()).isEqualTo(20L);
        verify(usuarioService).cadastrarEndereco(1L, request);
    }

    @Test
    @DisplayName("removerEndereco deve delegar ao service com clienteId e enderecoId")
    void removerEndereco_deveDelegarParaService() {
        usuarioLogado(1L, "ana@teste.com");

        ResponseEntity<Void> resposta = controller.removerEndereco(10L);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(usuarioService).removerEndereco(1L, 10L);
    }

    // ── cartões ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("listarCartoes deve retornar os cartões do usuário logado mapeados para DTO")
    void listarCartoes_deveMapearParaDTO() {
        usuarioLogado(1L, "ana@teste.com");
        Cartao c = new Cartao();
        c.setId(30L);
        c.setNomeTitular("Ana Silva");
        c.setBandeira("VISA");
        c.setQuatroUltimosDigitos("1111");
        c.setDataExpiracao("12/2030");
        when(usuarioService.listarCartoes(1L)).thenReturn(List.of(c));

        ResponseEntity<List<br.ufpb.dsc.mercado.dto.CartaoDTO>> resposta = controller.listarCartoes();

        assertThat(resposta.getBody()).hasSize(1);
        assertThat(resposta.getBody().get(0).quatroUltimosDigitos()).isEqualTo("1111");
    }

    @Test
    @DisplayName("cadastrarCartao deve delegar ao service usando o id do usuário logado")
    void cadastrarCartao_deveDelegarParaService() {
        usuarioLogado(1L, "ana@teste.com");
        CartaoSalvarDTO request = new CartaoSalvarDTO("Ana Silva", "4111111111111111", "VISA", "12/2030", "123");
        Cartao salvo = new Cartao();
        salvo.setId(40L);
        salvo.setNomeTitular("Ana Silva");
        salvo.setBandeira("VISA");
        salvo.setQuatroUltimosDigitos("1111");
        salvo.setDataExpiracao("12/2030");
        when(usuarioService.cadastrarCartao(1L, request)).thenReturn(salvo);

        ResponseEntity<br.ufpb.dsc.mercado.dto.CartaoDTO> resposta = controller.cadastrarCartao(request);

        assertThat(resposta.getBody().id()).isEqualTo(40L);
    }

    @Test
    @DisplayName("removerCartao deve delegar ao service com clienteId e cartaoId")
    void removerCartao_deveDelegarParaService() {
        usuarioLogado(1L, "ana@teste.com");

        ResponseEntity<Void> resposta = controller.removerCartao(30L);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(usuarioService).removerCartao(1L, 30L);
    }
}
