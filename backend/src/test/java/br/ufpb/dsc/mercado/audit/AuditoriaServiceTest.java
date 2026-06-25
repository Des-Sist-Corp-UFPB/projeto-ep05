package br.ufpb.dsc.mercado.audit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditoriaService — Testes Unitários")
class AuditoriaServiceTest {

    @Mock
    private LogAuditoriaRepository repository;

    private AuditoriaService service;

    @BeforeEach
    void setUp() {
        service = new AuditoriaService(repository);
    }

    // ── registrar ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("registrar: deve salvar o log no repositório")
    void registrar_deveSalvarNoRepositorio() {
        LogAuditoria log = LogAuditoria.builder()
                .papelAtor("ADMIN")
                .ator("admin@test.com")
                .categoria("PRODUTO")
                .descricao("Criou produto X")
                .build();

        service.registrar(log);

        ArgumentCaptor<LogAuditoria> captor = ArgumentCaptor.forClass(LogAuditoria.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getPapelAtor()).isEqualTo("ADMIN");
        assertThat(captor.getValue().getAtor()).isEqualTo("admin@test.com");
    }

    @Test
    @DisplayName("registrar: deve silenciar exceção de persistência sem propagar")
    void registrar_excecaoDeveSerSilenciada() {
        when(repository.save(any())).thenThrow(new RuntimeException("DB error"));

        LogAuditoria log = LogAuditoria.builder()
                .papelAtor("SYSTEM").ator("SYSTEM")
                .categoria("SYSTEM").descricao("test").build();

        // Não deve lançar exceção
        service.registrar(log);
    }

    // ── atalhos de papel ──────────────────────────────────────────────────────

    @Test
    @DisplayName("registrarAdmin: papelAtor deve ser ADMIN")
    void registrarAdmin_papelDeveSerAdmin() {
        service.registrarAdmin("admin@test.com", "PRODUTO", "Criou produto", 1L);

        ArgumentCaptor<LogAuditoria> captor = ArgumentCaptor.forClass(LogAuditoria.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getPapelAtor()).isEqualTo("ADMIN");
        assertThat(captor.getValue().getRecursoId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("registrarCliente: papelAtor deve ser CLIENTE")
    void registrarCliente_papelDeveSerCliente() {
        service.registrarCliente("cli@test.com", "PEDIDO", "Criou pedido", 42L);

        ArgumentCaptor<LogAuditoria> captor = ArgumentCaptor.forClass(LogAuditoria.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getPapelAtor()).isEqualTo("CLIENTE");
    }

    @Test
    @DisplayName("registrarSysAdmin: papelAtor deve ser SYSADMIN")
    void registrarSysAdmin_papelDeveSerSysAdmin() {
        service.registrarSysAdmin("sys@test.com", "USER_MGMT", "Criou admin", 5L);

        ArgumentCaptor<LogAuditoria> captor = ArgumentCaptor.forClass(LogAuditoria.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getPapelAtor()).isEqualTo("SYSADMIN");
    }

    @Test
    @DisplayName("registrarSystem: ator e papelAtor devem ser SYSTEM")
    void registrarSystem_deveUsarSystem() {
        service.registrarSystem("SYSTEM", "Backup executado");

        ArgumentCaptor<LogAuditoria> captor = ArgumentCaptor.forClass(LogAuditoria.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getPapelAtor()).isEqualTo("SYSTEM");
        assertThat(captor.getValue().getAtor()).isEqualTo("SYSTEM");
    }

    // ── listar ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("listar: sem filtros deve usar findAllByOrderByCriadoEmDesc")
    void listar_semFiltros_deveUsarMetodoSimples() {
        PageRequest pr = PageRequest.of(0, 10);
        Page<LogAuditoria> page = new PageImpl<>(List.of());
        when(repository.findAllByOrderByCriadoEmDesc(pr)).thenReturn(page);

        Page<LogAuditoria> result = service.listar(null, null, null, pr);

        assertThat(result).isSameAs(page);
        verify(repository).findAllByOrderByCriadoEmDesc(pr);
        verify(repository, never()).filtrar(any(), any(), any(), any());
    }

    @Test
    @DisplayName("listar: com filtro de papel deve usar filtrar")
    void listar_comFiltroPapel_deveUsarFiltrar() {
        PageRequest pr = PageRequest.of(0, 10);
        Page<LogAuditoria> page = new PageImpl<>(List.of());
        when(repository.filtrar(eq("ADMIN"), isNull(), isNull(), eq(pr))).thenReturn(page);

        Page<LogAuditoria> result = service.listar("ADMIN", null, null, pr);

        assertThat(result).isSameAs(page);
        verify(repository).filtrar("ADMIN", null, null, pr);
    }

    @Test
    @DisplayName("listar: com filtro de dias deve calcular Instant de corte")
    void listar_comFiltroDias_devePassarInstant() {
        PageRequest pr = PageRequest.of(0, 10);
        when(repository.filtrar(isNull(), isNull(), notNull(), eq(pr)))
                .thenReturn(new PageImpl<>(List.of()));

        service.listar(null, null, 7, pr);

        verify(repository).filtrar(isNull(), isNull(), notNull(), eq(pr));
    }

    @Test
    @DisplayName("listar: com filtro de ator deve usar filtrar")
    void listar_comFiltroAtor_deveUsarFiltrar() {
        PageRequest pr = PageRequest.of(0, 10);
        when(repository.filtrar(isNull(), eq("admin@test.com"), isNull(), eq(pr)))
                .thenReturn(new PageImpl<>(List.of()));

        service.listar("", "admin@test.com", 0, pr);

        verify(repository).filtrar(isNull(), eq("admin@test.com"), isNull(), eq(pr));
    }
}
