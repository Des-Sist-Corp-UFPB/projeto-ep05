package br.ufpb.dsc.mercado.audit;

import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Serviço central de auditoria.
 *
 * Uso:
 * <pre>
 *   auditoriaService.registrar(LogAuditoria.builder()
 *       .papelAtor("ADMIN")
 *       .ator(email)
 *       .categoria("PRODUTO")
 *       .descricao("Criou produto: " + nome)
 *       .recursoId(produto.getId())
 *       .build());
 * </pre>
 *
 * O método {@code registrar} roda em transação própria (REQUIRES_NEW) para
 * garantir que o log seja salvo mesmo se a transação principal sofrer rollback.
 */
@Service
public class AuditoriaService {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaService.class);

    private final LogAuditoriaRepository repository;

    public AuditoriaService(LogAuditoriaRepository repository) {
        this.repository = repository;
    }

    // ── Escrita ───────────────────────────────────────────────────────────────

    /**
     * Persiste um registro de auditoria em transação própria.
     * Nunca propaga exceções — um erro de log não deve derrubar a requisição.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(LogAuditoria entry) {
        try {
            repository.save(entry);
        } catch (Exception ex) {
            log.error("[AUDITORIA] Falha ao persistir log: {}", ex.getMessage(), ex);
        }
    }

    /** Atalho para ações de ADMIN */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarAdmin(String ator, String categoria, String descricao, Long recursoId) {
        registrar(LogAuditoria.builder()
                .papelAtor("ADMIN")
                .ator(ator)
                .categoria(categoria)
                .descricao(descricao)
                .recursoId(recursoId)
                .build());
    }

    /** Atalho para ações de CLIENTE */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarCliente(String ator, String categoria, String descricao, Long recursoId) {
        registrar(LogAuditoria.builder()
                .papelAtor("CLIENTE")
                .ator(ator)
                .categoria(categoria)
                .descricao(descricao)
                .recursoId(recursoId)
                .build());
    }

    /** Atalho para ações do SYSADMIN */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarSysAdmin(String ator, String categoria, String descricao, Long recursoId) {
        registrar(LogAuditoria.builder()
                .papelAtor("SYSADMIN")
                .ator(ator)
                .categoria(categoria)
                .descricao(descricao)
                .recursoId(recursoId)
                .build());
    }

    /** Atalho para eventos do sistema */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarSystem(String categoria, String descricao) {
        registrar(LogAuditoria.builder()
                .papelAtor("SYSTEM")
                .ator("SYSTEM")
                .categoria(categoria)
                .descricao(descricao)
                .build());
    }

    // ── Leitura ───────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<LogAuditoria> listar(String papel, String ator, Integer diasAtras, Pageable pageable) {
        Instant desde = (diasAtras != null && diasAtras > 0)
                ? Instant.now().minus(diasAtras, ChronoUnit.DAYS)
                : null;

        String papelFiltro = (papel != null && !papel.isBlank()) ? papel : null;
        String atorFiltro  = (ator  != null && !ator.isBlank())  ? ator  : null;

        if (papelFiltro == null && atorFiltro == null && desde == null) {
            return repository.findAllByOrderByCriadoEmDesc(pageable);
        }

        return repository.filtrar(papelFiltro, atorFiltro, desde, pageable);
    }
}
