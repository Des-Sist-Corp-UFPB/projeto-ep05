package br.ufpb.dsc.mercado.controller;

import br.ufpb.dsc.mercado.audit.LogAuditoria;
import br.ufpb.dsc.mercado.audit.LogAuditoriaRepository;
import br.ufpb.dsc.mercado.domain.Papel;
import br.ufpb.dsc.mercado.domain.StatusUsuario;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@DisplayName("SysAdminController — Testes de Integração com Auditoria")
class SysAdminControllerTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired MockMvc mockMvc;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired LogAuditoriaRepository logRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        logRepository.deleteAll();
        usuarioRepository.findAll().stream()
                .filter(u -> u.getPapel() != Papel.SYSADMIN)
                .forEach(usuarioRepository::delete);
    }

    // ── Dashboard ─────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "SYSADMIN")
    @DisplayName("GET /sysadmin/dashboard: SYSADMIN deve ter acesso")
    void dashboard_sysadmin_deveRetornarOk() throws Exception {
        mockMvc.perform(get("/sysadmin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("sysadmin/dashboard"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /sysadmin/dashboard: ADMIN deve receber 403")
    void dashboard_admin_deveRetornar403() throws Exception {
        mockMvc.perform(get("/sysadmin/dashboard"))
                .andExpect(status().isForbidden());
    }

    // ── Admins ────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "SYSADMIN")
    @DisplayName("GET /sysadmin/admins: deve listar admins com model correto")
    void listarAdmins_deveRetornarPagina() throws Exception {
        mockMvc.perform(get("/sysadmin/admins"))
                .andExpect(status().isOk())
                .andExpect(view().name("sysadmin/admins"))
                .andExpect(model().attributeExists("admins"));
    }

    @Test
    @WithMockUser(roles = "SYSADMIN")
    @DisplayName("GET /sysadmin/admins (HTMX): deve retornar fragmento da tabela")
    void listarAdmins_comHtmx_deveRetornarFragmento() throws Exception {
        mockMvc.perform(get("/sysadmin/admins").header("HX-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("sysadmin/fragments/tabela_admins :: tabela"));
    }

    @Test
    @WithMockUser(roles = "SYSADMIN")
    @DisplayName("GET /sysadmin/admins/novo: deve retornar formulário vazio")
    void novoAdminForm_deveRetornarFormVazio() throws Exception {
        mockMvc.perform(get("/sysadmin/admins/novo"))
                .andExpect(status().isOk())
                .andExpect(view().name("sysadmin/fragments/form_admin :: modal"))
                .andExpect(model().attributeExists("form"))
                .andExpect(model().attribute("adminId", (Object) null));
    }

    @Test
    @WithMockUser(username = "sys@test.com", roles = "SYSADMIN")
    @DisplayName("POST /sysadmin/admins: dados válidos devem criar admin e gerar log")
    void criarAdmin_dadosValidos_deveCriarELogar() throws Exception {
        mockMvc.perform(post("/sysadmin/admins")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("nome", "Novo Admin Teste")
                        .param("email", "novoadmin@sweetdelights.com")
                        .param("senha", "senha123")
                        .param("papel", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(view().name("sysadmin/fragments/tabela_admins :: tabela"))
                .andExpect(content().string(containsString("Novo Admin Teste")));

        // Verifica log de auditoria gerado
        List<LogAuditoria> logs = logRepository.findAll();
        assertThat(logs).isNotEmpty();
        assertThat(logs.get(0).getPapelAtor()).isEqualTo("SYSADMIN");
        assertThat(logs.get(0).getDescricao()).contains("novoadmin@sweetdelights.com");
    }

    @Test
    @WithMockUser(roles = "SYSADMIN")
    @DisplayName("POST /sysadmin/admins: e-mail duplicado deve retornar form com erro")
    void criarAdmin_emailDuplicado_deveRetornarFormComErro() throws Exception {
        usuarioRepository.save(new Usuario(
                "Admin Existente", "duplicado@sweetdelights.com",
                passwordEncoder.encode("senha123"), Papel.ADMIN));

        mockMvc.perform(post("/sysadmin/admins")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("nome", "Outro Admin")
                        .param("email", "duplicado@sweetdelights.com")
                        .param("senha", "senha123")
                        .param("papel", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(view().name("sysadmin/fragments/form_admin :: modal"))
                .andExpect(model().hasErrors());
    }

    @Test
    @WithMockUser(roles = "SYSADMIN")
    @DisplayName("POST /sysadmin/admins: dados inválidos devem retornar form com erros de validação")
    void criarAdmin_dadosInvalidos_deveRetornarFormComErros() throws Exception {
        mockMvc.perform(post("/sysadmin/admins")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("nome", "")
                        .param("email", "naoeemail")
                        .param("senha", "123")
                        .param("papel", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(view().name("sysadmin/fragments/form_admin :: modal"))
                .andExpect(model().hasErrors());
    }

    @Test
    @WithMockUser(roles = "SYSADMIN")
    @DisplayName("GET /sysadmin/admins/{id}/editar: deve retornar form preenchido")
    void editarAdminForm_deveRetornarFormPreenchido() throws Exception {
        Usuario admin = usuarioRepository.save(new Usuario(
                "Admin Edit", "edit@test.com",
                passwordEncoder.encode("senha123"), Papel.ADMIN));

        mockMvc.perform(get("/sysadmin/admins/{id}/editar", admin.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("sysadmin/fragments/form_admin :: modal"))
                .andExpect(model().attributeExists("form"))
                .andExpect(model().attribute("adminId", admin.getId()));
    }

    @Test
    @WithMockUser(username = "sys@test.com", roles = "SYSADMIN")
    @DisplayName("PUT /sysadmin/admins/{id}: deve atualizar admin e gerar log")
    void editarAdmin_deveAtualizarELogar() throws Exception {
        Usuario admin = usuarioRepository.save(new Usuario(
                "Admin Old", "old@test.com",
                passwordEncoder.encode("senha123"), Papel.ADMIN));

        mockMvc.perform(put("/sysadmin/admins/{id}", admin.getId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("nome", "Admin New")
                        .param("email", "new@test.com")
                        .param("senha", "")
                        .param("papel", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(view().name("sysadmin/fragments/tabela_admins :: tabela"));

        List<LogAuditoria> logs = logRepository.findAll();
        assertThat(logs).isNotEmpty();
        assertThat(logs.get(0).getCategoria()).isEqualTo("USER_MGMT");
    }

    @Test
    @WithMockUser(username = "sys@test.com", roles = "SYSADMIN")
    @DisplayName("POST /sysadmin/admins/{id}/bloquear: deve alternar status e gerar log")
    void bloquearAdmin_deveAlternarStatusELogar() throws Exception {
        Usuario admin = usuarioRepository.save(new Usuario(
                "Admin Para Bloquear", "bloquear@sweetdelights.com",
                passwordEncoder.encode("senha123"), Papel.ADMIN));

        mockMvc.perform(post("/sysadmin/admins/{id}/bloquear", admin.getId())
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("sysadmin/fragments/tabela_admins :: tabela"));

        Usuario atualizado = usuarioRepository.findById(admin.getId()).orElseThrow();
        assertThat(atualizado.getStatus()).isEqualTo(StatusUsuario.BLOQUEADO);

        List<LogAuditoria> logs = logRepository.findAll();
        assertThat(logs).isNotEmpty();
    }

    // ── Logs ──────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "SYSADMIN")
    @DisplayName("GET /sysadmin/logs: deve retornar página de logs com model correto")
    void visualizarLogs_deveRetornarPagina() throws Exception {
        mockMvc.perform(get("/sysadmin/logs"))
                .andExpect(status().isOk())
                .andExpect(view().name("sysadmin/logs"))
                .andExpect(model().attributeExists("logs"));
    }

    @Test
    @WithMockUser(roles = "SYSADMIN")
    @DisplayName("GET /sysadmin/logs: filtro por papel ADMIN deve funcionar")
    void visualizarLogs_comFiltroPapel_deveRetornarLogs() throws Exception {
        // Seed: cria log de ADMIN manualmente
        logRepository.save(LogAuditoria.builder()
                .papelAtor("ADMIN").ator("admin@test.com")
                .categoria("PRODUTO").descricao("Criou produto X").build());

        mockMvc.perform(get("/sysadmin/logs").param("papel", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(view().name("sysadmin/logs"))
                .andExpect(model().attributeExists("logs"));
    }

    @Test
    @WithMockUser(roles = "SYSADMIN")
    @DisplayName("GET /sysadmin/configuracoes: deve retornar página de configurações")
    void configuracoes_deveRetornarPagina() throws Exception {
        mockMvc.perform(get("/sysadmin/configuracoes"))
                .andExpect(status().isOk())
                .andExpect(view().name("sysadmin/configuracoes"));
    }
}
