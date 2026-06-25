package br.ufpb.dsc.mercado.controller;

import br.ufpb.dsc.mercado.domain.Produto;
import br.ufpb.dsc.mercado.repository.ProdutoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração do {@link ProdutoController} com Testcontainers.
 *
 * <p>Usa roles = "ADMIN" em todos os @WithMockUser porque a SecurityConfig
 * exige ADMIN ou SYSADMIN para acessar /produtos/**.
 *
 * <p>O construtor de Produto usado nos testes é:
 * Produto(nome, descricao, preco, categoria, estoque) — categoria pode ser null.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("ProdutoController — Testes de Integração")
class ProdutoControllerTest {


    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProdutoRepository produtoRepository;

    private Produto produtoCadastrado;

    @BeforeEach
    void setUp() {
        produtoRepository.deleteAll();
        // Construtor correto: (nome, descricao, preco, categoria, estoque)
        // categoria = null é aceito (ON DELETE SET NULL no schema)
        produtoCadastrado = produtoRepository.save(
                new Produto("Arroz Integral", "Arroz integral tipo 1", new BigDecimal("8.99"), null, 50)
        );
    }

    // =========================================================================
    // TESTES: GET /produtos
    // =========================================================================

    @Test
    @WithMockUser(username = "admin@sweetdelights.com", roles = "ADMIN")
    @DisplayName("GET /produtos: deve retornar página de listagem com status 200")
    void listar_usuarioAutenticado_deveRetornarPaginaLista() throws Exception {
        mockMvc.perform(get("/produtos"))
                .andExpect(status().isOk())
                .andExpect(view().name("produtos/lista"))
                .andExpect(model().attributeExists("produtos"))
                .andExpect(content().string(containsString("Arroz Integral")));
    }

    @Test
    @DisplayName("GET /produtos: usuário não autenticado deve ser redirecionado para /login")
    void listar_semAutenticacao_deveRedirecionarParaLogin() throws Exception {
        mockMvc.perform(get("/produtos"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    // =========================================================================
    // TESTES: GET /produtos/novo
    // =========================================================================

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /produtos/novo: deve retornar fragmento do formulário vazio")
    void novoForm_deveRetornarFragmentoFormulario() throws Exception {
        mockMvc.perform(get("/produtos/novo"))
                .andExpect(status().isOk())
                .andExpect(view().name("produtos/fragments/form :: modal"))
                .andExpect(model().attributeExists("form"))
                .andExpect(model().attribute("produto", nullValue()));
    }

    // =========================================================================
    // TESTES: POST /produtos
    // =========================================================================

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /produtos: deve criar produto e retornar fragmento da linha")
    void criar_dadosValidos_deveCriarERetornarLinha() throws Exception {
        mockMvc.perform(post("/produtos")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("nome", "Feijão Preto")
                        .param("descricao", "Feijão preto premium")
                        .param("preco", "7.50")
                        .param("estoque", "10")
                        .param("ativo", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("produtos/fragments/linha :: linha"))
                .andExpect(model().attributeExists("produto"))
                .andExpect(content().string(containsString("Feijão Preto")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /produtos: dados inválidos devem retornar formulário com erros")
    void criar_dadosInvalidos_deveRetornarFormularioComErros() throws Exception {
        mockMvc.perform(post("/produtos")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("nome", "")        // nome vazio — inválido
                        .param("preco", "-1.00")  // preço negativo — inválido
                        .param("estoque", "-5"))  // estoque negativo — inválido
                .andExpect(status().isOk())
                .andExpect(view().name("produtos/fragments/form :: modal"))
                .andExpect(model().hasErrors());
    }

    // =========================================================================
    // TESTES: GET /produtos/{id}/editar
    // =========================================================================

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /produtos/{id}/editar: deve retornar formulário preenchido")
    void editarForm_produtoExistente_deveRetornarFormularioPreenchido() throws Exception {
        mockMvc.perform(get("/produtos/{id}/editar", produtoCadastrado.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("produtos/fragments/form :: modal"))
                .andExpect(model().attributeExists("form", "produto"))
                .andExpect(content().string(containsString("Arroz Integral")));
    }

    // =========================================================================
    // TESTES: DELETE /produtos/{id}
    // =========================================================================

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE /produtos/{id}: deve excluir produto e retornar 200")
    void excluir_produtoExistente_deveRetornar200() throws Exception {
        mockMvc.perform(delete("/produtos/{id}", produtoCadastrado.getId())
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE /produtos/{id}: produto inexistente deve retornar 404")
    void excluir_produtoInexistente_deveRetornar404() throws Exception {
        mockMvc.perform(delete("/produtos/{id}", 9999L)
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }
}
