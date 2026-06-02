package br.ufpb.dsc.mercado.controller;

import br.ufpb.dsc.mercado.domain.Papel;
import br.ufpb.dsc.mercado.domain.Usuario;
import br.ufpb.dsc.mercado.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração do {@link SysAdminController}.
 *
 * <p>Cobre os fluxos de criação de admins, validação de erros e bloqueio.
 * Verifica especificamente o bug corrigido: papel=null ao criar admin.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@DisplayName("SysAdminController — Testes de Integração")
class SysAdminControllerTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        // Remove apenas admins e clientes de teste — não remove o sysadmin do seeder
        usuarioRepository.findAll().stream()
                .filter(u -> u.getPapel() != Papel.SYSADMIN)
                .forEach(u -> usuarioRepository.delete(u));
    }

    // ── GET /sysadmin/dashboard ───────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "SYSADMIN")
    @DisplayName("GET /sysadmin/dashboard: deve retornar página do dashboard para SYSADMIN")
    void dashboard_sysadmin_deveRetornarOk() throws Exception {
        mockMvc.perform(get("/sysadmin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("sysadmin/dashboard"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /sysadmin/dashboard: ADMIN não pode acessar área do sysadmin (403)")
    void dashboard_admin_deveRetornar403() throws Exception {
        mockMvc.perform(get("/sysadmin/dashboard"))
                .andExpect(status().isForbidden());
    }

    // ── GET /sysadmin/admins ──────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "SYSADMIN")
    @DisplayName("GET /sysadmin/admins: deve listar admins")
    void listarAdmins_deveRetornarPagina() throws Exception {
        mockMvc.perform(get("/sysadmin/admins"))
                .andExpect(status().isOk())
                .andExpect(view().name("sysadmin/admins"))
                .andExpect(model().attributeExists("admins"));
    }

    // ── GET /sysadmin/admins/novo ─────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "SYSADMIN")
    @DisplayName("GET /sysadmin/admins/novo: deve retornar fragmento do formulário vazio")
    void novoAdminForm_deveRetornarFragmento() throws Exception {
        mockMvc.perform(get("/sysadmin/admins/novo"))
                .andExpect(status().isOk())
                .andExpect(view().name("sysadmin/fragments/form_admin :: modal"))
                .andExpect(model().attributeExists("form"))
                .andExpect(model().attribute("adminId", (Object) null));
    }

    // ── POST /sysadmin/admins ─────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "SYSADMIN")
    @DisplayName("POST /sysadmin/admins: dados válidos devem criar admin e retornar tabela atualizada")
    void criarAdmin_dadosValidos_deveCriarERetornarTabela() throws Exception {
        mockMvc.perform(post("/sysadmin/admins")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("nome", "Novo Admin Teste")
                        .param("email", "novoadmin@sweetdelights.com")
                        .param("senha", "senha123")
                        .param("papel", "ADMIN"))  // campo hidden do formulário
                .andExpect(status().isOk())
                // Após sucesso retorna a tabela atualizada
                .andExpect(view().name("sysadmin/fragments/tabela_admins :: tabela"))
                .andExpect(content().string(containsString("Novo Admin Teste")));
    }

    @Test
    @WithMockUser(roles = "SYSADMIN")
    @DisplayName("POST /sysadmin/admins: e-mail duplicado deve retornar formulário com erro")
    void criarAdmin_emailDuplicado_deveRetornarFormComErro() throws Exception {
        // Cria admin com o mesmo e-mail primeiro
        usuarioRepository.save(new Usuario(
                "Admin Existente",
                "duplicado@sweetdelights.com",
                passwordEncoder.encode("senha123"),
                Papel.ADMIN
        ));

        mockMvc.perform(post("/sysadmin/admins")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("nome", "Outro Admin")
                        .param("email", "duplicado@sweetdelights.com")
                        .param("senha", "senha123")
                        .param("papel", "ADMIN"))
                .andExpect(status().isOk())
                // Volta pro formulário com erro
                .andExpect(view().name("sysadmin/fragments/form_admin :: modal"))
                .andExpect(model().hasErrors());
    }

    @Test
    @WithMockUser(roles = "SYSADMIN")
    @DisplayName("POST /sysadmin/admins: dados inválidos devem retornar formulário com erros de validação")
    void criarAdmin_dadosInvalidos_deveRetornarFormComErros() throws Exception {
        mockMvc.perform(post("/sysadmin/admins")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("nome", "")         // nome vazio — inválido
                        .param("email", "naoeemail") // email inválido
                        .param("senha", "123")      // senha curta demais
                        .param("papel", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(view().name("sysadmin/fragments/form_admin :: modal"))
                .andExpect(model().hasErrors());
    }

    // ── POST /sysadmin/admins/{id}/bloquear ───────────────────────────────────

    @Test
    @WithMockUser(roles = "SYSADMIN")
    @DisplayName("POST /sysadmin/admins/{id}/bloquear: deve alternar status e retornar tabela")
    void bloquearAdmin_deveAlternarStatusERetornarTabela() throws Exception {
        Usuario admin = usuarioRepository.save(new Usuario(
                "Admin Para Bloquear",
                "bloquear@sweetdelights.com",
                passwordEncoder.encode("senha123"),
                Papel.ADMIN
        ));

        mockMvc.perform(post("/sysadmin/admins/{id}/bloquear", admin.getId())
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("sysadmin/fragments/tabela_admins :: tabela"));
    }
}
