package br.ufpb.dsc.mercado.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface LogAuditoriaRepository extends JpaRepository<LogAuditoria, Long> {

    Page<LogAuditoria> findAllByOrderByCriadoEmDesc(Pageable pageable);

    @Query("""
            SELECT l FROM LogAuditoria l
            WHERE (:papel IS NULL OR l.papelAtor = :papel)
              AND (:ator  IS NULL OR LOWER(l.ator) LIKE LOWER(CONCAT('%', :ator, '%')))
              AND (:desde IS NULL OR l.criadoEm >= :desde)
            ORDER BY l.criadoEm DESC
            """)
    Page<LogAuditoria> filtrar(
            @Param("papel") String papel,
            @Param("ator")  String ator,
            @Param("desde") Instant desde,
            Pageable pageable);
}
