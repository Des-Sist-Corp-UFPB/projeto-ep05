package br.ufpb.dsc.mercado.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;

public interface LogAuditoriaRepositoryCustom {

    Page<LogAuditoria> filtrar(String papel, String ator, Instant desde, Pageable pageable);
}
