package br.ufpb.dsc.mercado.audit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LogAuditoria — Testes de Builder e Entidade")
class LogAuditoriaTest {

    @Test
    @DisplayName("builder: deve construir com todos os campos")
    void builder_devePreencherTodosCampos() {
        LogAuditoria log = LogAuditoria.builder()
                .papelAtor("ADMIN")
                .ator("admin@test.com")
                .categoria("PRODUTO")
                .descricao("Criou produto X")
                .recursoId(10L)
                .build();

        assertThat(log.getPapelAtor()).isEqualTo("ADMIN");
        assertThat(log.getAtor()).isEqualTo("admin@test.com");
        assertThat(log.getCategoria()).isEqualTo("PRODUTO");
        assertThat(log.getDescricao()).isEqualTo("Criou produto X");
        assertThat(log.getRecursoId()).isEqualTo(10L);
        assertThat(log.getResultado()).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("builder: resultado padrão deve ser SUCCESS")
    void builder_resultadoPadrao_deveSerSuccess() {
        LogAuditoria log = LogAuditoria.builder()
                .papelAtor("SYSTEM").ator("SYSTEM")
                .categoria("SYSTEM").descricao("evento").build();

        assertThat(log.getResultado()).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("builder.falha(): resultado deve ser FAILURE")
    void builder_falha_deveSerFailure() {
        LogAuditoria log = LogAuditoria.builder()
                .papelAtor("ADMIN").ator("a@b.com")
                .categoria("AUTH").descricao("login falhou")
                .falha()
                .build();

        assertThat(log.getResultado()).isEqualTo("FAILURE");
    }

    @Test
    @DisplayName("builder: recursoId é opcional (pode ser null)")
    void builder_recursoIdOpcional() {
        LogAuditoria log = LogAuditoria.builder()
                .papelAtor("SYSTEM").ator("SYSTEM")
                .categoria("SYSTEM").descricao("sem recurso").build();

        assertThat(log.getRecursoId()).isNull();
    }
}
