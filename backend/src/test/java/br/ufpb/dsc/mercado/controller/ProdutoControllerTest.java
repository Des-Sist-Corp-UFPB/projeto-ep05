package br.ufpb.dsc.mercado.controller;

import br.ufpb.dsc.mercado.audit.LogAuditoria;
import br.ufpb.dsc.mercado.audit.LogAuditoriaRepository;
import br.ufpb.dsc.mercado.domain.Categoria;
import br.ufpb.dsc.mercado.domain.Produto;
import br.ufpb.dsc.mercado.repository.CategoriaRepository;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração do {@link ProdutoController}.
 *
 * <p>Usa roles = "ADMIN" em todos os @WithMockUser porque a SecurityConfig
 * exige ADMIN ou SYSADMIN para acessar /produtos/**.
 *
 * <p>O construtor de Produto usado nos testes é:
 * Produto(nome, descricao, preco, categoria, estoque) — categoria pode ser null.
 *
 * <p>Roda com H2 em memória via application-test.yml (perfil "test"), sem
 * necessidade de Docker — entra no "mvn test"/"mvn clean test jacoco:report" padrão.
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

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private LogAuditoriaRepository logRepository;

    private Produto produtoCadastrado;

    @BeforeEach
    void setUp() {
        logRepository.deleteAll();
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

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /produtos (HTMX): deve retornar fragmento da tabela")
    void listar_comHtmx_deveRetornarFragmento() throws Exception {
        mockMvc.perform(get("/produtos").header("HX-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("produtos/fragments/tabela :: tabela"))
                .andExpect(model().attributeExists("produtos"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /produtos: busca por nome deve filtrar resultados")
    void listar_comBusca_deveFiltrar() throws Exception {
        produtoRepository.save(new Produto("Feijão Carioca", "Feijão tipo 1", new BigDecimal("6.50"), null, 30));

        mockMvc.perform(get("/produtos").param("busca", "Feijão"))
                .andExpect(status().isOk())
                .andExpect(view().name("produtos/lista"))
                .andExpect(model().attribute("busca", "Feijão"))
                .andExpect(content().string(containsString("Feijão Carioca")))
                .andExpect(content().string(not(containsString("Arroz Integral"))));
    }

    // =========================================================================
    // TESTES: GET /produtos/fragmento-tabela
    // =========================================================================

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /produtos/fragmento-tabela: deve retornar fragmento da tabela com produtos")
    void fragmentoTabela_deveRetornarFragmento() throws Exception {
        mockMvc.perform(get("/produtos/fragmento-tabela"))
                .andExpect(status().isOk())
                .andExpect(view().name("produtos/fragments/tabela :: tabela"))
                .andExpect(model().attributeExists("produtos"))
                .andExpect(content().string(containsString("Arroz Integral")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /produtos/fragmento-tabela: busca deve filtrar resultados")
    void fragmentoTabela_comBusca_deveFiltrar() throws Exception {
        mockMvc.perform(get("/produtos/fragmento-tabela").param("busca", "Arroz"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("busca", "Arroz"))
                .andExpect(content().string(containsString("Arroz Integral")));
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
    @WithMockUser(username = "admin@sweetdelights.com", roles = "ADMIN")
    @DisplayName("POST /produtos: deve criar produto, registrar auditoria como ADMIN e retornar fragmento da linha")
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

        List<LogAuditoria> logs = logRepository.findAll();
        assertThat(logs).isNotEmpty();
        assertThat(logs.get(0).getPapelAtor()).isEqualTo("ADMIN");
        assertThat(logs.get(0).getAtor()).isEqualTo("admin@sweetdelights.com");
        assertThat(logs.get(0).getDescricao()).contains("Feijão Preto");
    }

    @Test
    @WithMockUser(username = "sys@sweetdelights.com", roles = "SYSADMIN")
    @DisplayName("POST /produtos: criado por SYSADMIN deve registrar auditoria com papel SYSADMIN")
    void criar_porSysAdmin_deveRegistrarAuditoriaComoSysAdmin() throws Exception {
        mockMvc.perform(post("/produtos")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("nome", "Quinoa Orgânica")
                        .param("descricao", "Quinoa premium")
                        .param("preco", "15.00")
                        .param("estoque", "5")
                        .param("ativo", "true"))
                .andExpect(status().isOk());

        List<LogAuditoria> logs = logRepository.findAll();
        assertThat(logs).isNotEmpty();
        assertThat(logs.get(0).getPapelAtor()).isEqualTo("SYSADMIN");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /produtos: com categoria válida deve associar a categoria")
    void criar_comCategoriaValida_deveAssociarCategoria() throws Exception {
        Categoria categoria = categoriaRepository.save(new Categoria("Grãos", "Categoria de grãos"));

        mockMvc.perform(post("/produtos")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("nome", "Lentilha")
                        .param("descricao", "Lentilha tipo 1")
                        .param("preco", "9.90")
                        .param("categoriaId", String.valueOf(categoria.getId()))
                        .param("estoque", "15")
                        .param("ativo", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Lentilha")));
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
                .andExpect(model().hasErrors())
                .andExpect(model().attributeExists("categorias"));

        assertThat(logRepository.findAll()).isEmpty();
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

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /produtos/{id}/editar: produto inexistente deve resultar em erro")
    void editarForm_produtoInexistente_deveRetornarErro() throws Exception {
        mockMvc.perform(get("/produtos/{id}/editar", 9999L))
                .andExpect(status().is5xxServerError());
    }

    // =========================================================================
    // TESTES: PUT /produtos/{id}
    // =========================================================================

    @Test
    @WithMockUser(username = "admin@sweetdelights.com", roles = "ADMIN")
    @DisplayName("PUT /produtos/{id}: dados válidos devem atualizar produto e registrar auditoria")
    void atualizar_dadosValidos_deveAtualizarERegistrarAuditoria() throws Exception {
        mockMvc.perform(put("/produtos/{id}", produtoCadastrado.getId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("nome", "Arroz Branco")
                        .param("descricao", "Arroz branco tipo 1")
                        .param("preco", "5.99")
                        .param("estoque", "40")
                        .param("ativo", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("produtos/fragments/linha :: linha"))
                .andExpect(model().attributeExists("produto"))
                .andExpect(content().string(containsString("Arroz Branco")));

        Produto atualizado = produtoRepository.findById(produtoCadastrado.getId()).orElseThrow();
        assertThat(atualizado.getNome()).isEqualTo("Arroz Branco");

        List<LogAuditoria> logs = logRepository.findAll();
        assertThat(logs).isNotEmpty();
        assertThat(logs.get(0).getCategoria()).isEqualTo("PRODUTO");
        assertThat(logs.get(0).getDescricao()).contains("Arroz Branco");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PUT /produtos/{id}: dados inválidos devem retornar formulário com erros e o produto original")
    void atualizar_dadosInvalidos_deveRetornarFormularioComErros() throws Exception {
        mockMvc.perform(put("/produtos/{id}", produtoCadastrado.getId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("nome", "")
                        .param("preco", "-1.00")
                        .param("estoque", "-5"))
                .andExpect(status().isOk())
                .andExpect(view().name("produtos/fragments/form :: modal"))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeExists("produto", "categorias"));

        Produto inalterado = produtoRepository.findById(produtoCadastrado.getId()).orElseThrow();
        assertThat(inalterado.getNome()).isEqualTo("Arroz Integral");
        assertThat(logRepository.findAll()).isEmpty();
    }

    // =========================================================================
    // TESTES: DELETE /produtos/{id}
    // =========================================================================

    @Test
    @WithMockUser(username = "admin@sweetdelights.com", roles = "ADMIN")
    @DisplayName("DELETE /produtos/{id}: deve excluir produto, registrar auditoria e retornar 200")
    void excluir_produtoExistente_deveRetornar200() throws Exception {
        mockMvc.perform(delete("/produtos/{id}", produtoCadastrado.getId())
                        .with(csrf()))
                .andExpect(status().isOk());

        assertThat(produtoRepository.findById(produtoCadastrado.getId())).isEmpty();

        List<LogAuditoria> logs = logRepository.findAll();
        assertThat(logs).isNotEmpty();
        assertThat(logs.get(0).getDescricao()).contains("Arroz Integral");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE /produtos/{id}: produto inexistente deve retornar 404")
    void excluir_produtoInexistente_deveRetornar404() throws Exception {
        mockMvc.perform(delete("/produtos/{id}", 9999L)
                        .with(csrf()))
                .andExpect(status().isNotFound());

        assertThat(logRepository.findAll()).isEmpty();
    }
}
