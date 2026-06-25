package br.ufpb.dsc.mercado.audit;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Repository
public class LogAuditoriaRepositoryImpl implements LogAuditoriaRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Page<LogAuditoria> filtrar(String papel, String ator, Instant desde, Pageable pageable) {
        Specification<LogAuditoria> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (papel != null) {
                predicates.add(cb.equal(root.get("papelAtor"), papel));
            }
            if (ator != null) {
                predicates.add(cb.like(
                    cb.lower(root.get("ator")),
                    "%" + ator.toLowerCase() + "%"
                ));
            }
            if (desde != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("criadoEm"), desde));
            }
            if (query != null) {
                query.orderBy(cb.desc(root.get("criadoEm")));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        // Usa SimpleJpaRepository para executar a Specification com suporte a paginação
        SimpleJpaRepository<LogAuditoria, Long> jpaRepo =
                new SimpleJpaRepository<>(LogAuditoria.class, em);

        return jpaRepo.findAll(spec, pageable);
    }
}
