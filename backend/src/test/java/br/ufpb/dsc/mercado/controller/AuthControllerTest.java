package br.ufpb.dsc.mercado.controller;

import br.ufpb.dsc.mercado.dto.CadastroClienteRequest;
import br.ufpb.dsc.mercado.domain.Usuario;
import br.ufpb.dsc.mercado.domain.Papel;
import br.ufpb.dsc.mercado.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController (MVC Thymeleaf) -- Testes Unitarios")
class AuthControllerTest {

    @Mock UsuarioService usuarioService;

    private AuthController controller;

    @BeforeEach
    void setUp() {
        controller = new AuthController(usuarioService);
    }

    private CadastroClienteRequest formValido() {
        return new CadastroClienteRequest(
                "Ana", "Silva", "ana@teste.com", "111.222.333-44",
                "(83) 99999-9999", LocalDate.of(2000, 1, 1),
                "Senha123", "Senha123");
    }

    @Test
    @DisplayName("GET /admin/login deve retornar a view auth/login")
    void login_deveRetornarViewLogin() {
        assertThat(controller.login()).isEqualTo("auth/login");
    }

    @Test
    @DisplayName("GET /admin/cadastro deve retornar a view auth/cadastro")
    void cadastro_deveRetornarViewCadastro() {
        assertThat(controller.cadastro()).isEqualTo("auth/cadastro");
    }

    @Test
    @DisplayName("POST /admin/cadastro com erros de validacao deve retornar auth/cadastro com erros")
    void processarCadastro_comErrosBindingResult_deveRetornarCadastro() {
        CadastroClienteRequest form = formValido();
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "form");
        bindingResult.rejectValue("email", "invalid", "E-mail invalido");
        Model model = new ConcurrentModel();
        RedirectAttributesModelMap redirectAttrs = new RedirectAttributesModelMap();

        String view = controller.processarCadastro(form, bindingResult, model, redirectAttrs);

        assertThat(view).isEqualTo("auth/cadastro");
        assertThat(model.containsAttribute("errosCampos")).isTrue();
        verify(usuarioService, never()).cadastrarCliente(any());
    }

    @Test
    @DisplayName("POST /admin/cadastro com senhas diferentes deve retornar auth/cadastro com erro de confirmacao")
    void processarCadastro_senhasNaoConferem_deveRetornarCadastro() {
        CadastroClienteRequest form = new CadastroClienteRequest(
                "Ana", "Silva", "ana@teste.com", "111.222.333-44",
                "(83) 99999-9999", LocalDate.of(2000, 1, 1),
                "Senha123", "Diferente123");
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "form");
        Model model = new ConcurrentModel();
        RedirectAttributesModelMap redirectAttrs = new RedirectAttributesModelMap();

        String view = controller.processarCadastro(form, bindingResult, model, redirectAttrs);

        assertThat(view).isEqualTo("auth/cadastro");
        @SuppressWarnings("unchecked")
        Map<String, Object> erros = (Map<String, Object>) model.getAttribute("errosCampos");
        assertThat(erros).containsKey("confirmacaoSenha");
        verify(usuarioService, never()).cadastrarCliente(any());
    }

    @Test
    @DisplayName("POST /admin/cadastro bem-sucedido deve redirecionar para /admin/login")
    void processarCadastro_sucesso_deveRedirecionarParaLogin() {
        CadastroClienteRequest form = formValido();
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "form");
        Model model = new ConcurrentModel();
        RedirectAttributesModelMap redirectAttrs = new RedirectAttributesModelMap();
        Usuario criado = new Usuario("Ana", "ana@teste.com", "hash", Papel.CLIENTE);
        when(usuarioService.cadastrarCliente(any())).thenReturn(criado);

        String view = controller.processarCadastro(form, bindingResult, model, redirectAttrs);

        assertThat(view).isEqualTo("redirect:/admin/login");
        verify(usuarioService).cadastrarCliente(form);
    }

    @Test
    @DisplayName("POST /admin/cadastro com e-mail duplicado deve retornar auth/cadastro com erro")
    void processarCadastro_emailDuplicado_deveRetornarCadastroComErro() {
        CadastroClienteRequest form = formValido();
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "form");
        Model model = new ConcurrentModel();
        RedirectAttributesModelMap redirectAttrs = new RedirectAttributesModelMap();
        when(usuarioService.cadastrarCliente(any()))
                .thenThrow(new IllegalArgumentException("E-mail ja cadastrado"));

        String view = controller.processarCadastro(form, bindingResult, model, redirectAttrs);

        assertThat(view).isEqualTo("auth/cadastro");
        assertThat(model.getAttribute("erro")).isEqualTo("E-mail ja cadastrado");
    }
}