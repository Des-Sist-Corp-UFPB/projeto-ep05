package br.ufpb.dsc.mercado.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface LogAuditoriaRepository
        extends JpaRepository<LogAuditoria, Long>,
                JpaSpecificationExecutor<LogAuditoria>,
                LogAuditoriaRepositoryCustom {

    Page<LogAuditoria> findAllByOrderByCriadoEmDesc(Pageable pageable);
}
