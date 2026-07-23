package br.ufpb.dsc.mercado.audit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("LogAuditoriaRepositoryImpl — Testes de Integração (H2)")
class LogAuditoriaRepositoryImplTest {

    @Autowired
    private LogAuditoriaRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();

        repository.save(LogAuditoria.builder()
                .papelAtor("ADMIN").ator("admin@test.com")
                .categoria("PRODUTO").descricao("Criou produto X").build());

        repository.save(LogAuditoria.builder()
                .papelAtor("SYSADMIN").ator("sys@test.com")
                .categoria("USER_MGMT").descricao("Criou admin").build());

        repository.save(LogAuditoria.builder()
                .papelAtor("ADMIN").ator("carlos.silva@test.com")
                .categoria("PEDIDO").descricao("Atualizou pedido").build());
    }

    @Test
    @DisplayName("filtrar: sem filtros deve retornar todos os registros")
    void filtrar_semFiltros_retornaTodos() {
        Page<LogAuditoria> page = repository.filtrar(null, null, null, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("filtrar: por papelAtor deve retornar apenas registros correspondentes")
    void filtrar_porPapel_retornaApenasCorrespondentes() {
        Page<LogAuditoria> page = repository.filtrar("ADMIN", null, null, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).allMatch(l -> l.getPapelAtor().equals("ADMIN"));
    }

    @Test
    @DisplayName("filtrar: por ator deve buscar de forma case-insensitive e parcial")
    void filtrar_porAtor_buscaCaseInsensitivePartial() {
        Page<LogAuditoria> page = repository.filtrar(null, "ADMIN@TEST", null, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getAtor()).isEqualTo("admin@test.com");
    }

    @Test
    @DisplayName("filtrar: por ator parcial deve casar múltiplos registros")
    void filtrar_porAtorParcial_casaMultiplos() {
        Page<LogAuditoria> page = repository.filtrar(null, "test.com", null, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("filtrar: por data 'desde' futura não deve retornar registros")
    void filtrar_porDataFutura_naoRetornaRegistros() {
        Instant amanha = Instant.now().plus(1, ChronoUnit.DAYS);

        Page<LogAuditoria> page = repository.filtrar(null, null, amanha, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("filtrar: por data 'desde' passada deve retornar todos os registros")
    void filtrar_porDataPassada_retornaTodos() {
        Instant ontem = Instant.now().minus(1, ChronoUnit.DAYS);

        Page<LogAuditoria> page = repository.filtrar(null, null, ontem, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("filtrar: combinando papel e ator deve aplicar ambos os predicados")
    void filtrar_combinandoPapelEAtor_aplicaAmbos() {
        Page<LogAuditoria> page = repository.filtrar("ADMIN", "carlos.silva", null, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getAtor()).isEqualTo("carlos.silva@test.com");
    }

    @Test
    @DisplayName("filtrar: deve ordenar por criadoEm decrescente")
    void filtrar_devOrdenarPorCriadoEmDesc() {
        Page<LogAuditoria> page = repository.filtrar(null, null, null, PageRequest.of(0, 10));

        List<LogAuditoria> content = page.getContent();
        assertThat(content).isSortedAccordingTo(
                (a, b) -> b.getCriadoEm().compareTo(a.getCriadoEm()));
    }

    @Test
    @DisplayName("filtrar: deve respeitar paginação")
    void filtrar_deveRespeitarPaginacao() {
        Pageable primeiraPagina = PageRequest.of(0, 2);

        Page<LogAuditoria> page = repository.filtrar(null, null, null, primeiraPagina);

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("filtrar: papel inexistente deve retornar página vazia")
    void filtrar_papelInexistente_retornaVazio() {
        Page<LogAuditoria> page = repository.filtrar("INEXISTENTE", null, null, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isZero();
        assertThat(page.getContent()).isEmpty();
    }
}
