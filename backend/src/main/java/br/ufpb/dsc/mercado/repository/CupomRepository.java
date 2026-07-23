package br.ufpb.dsc.mercado.repository;

import br.ufpb.dsc.mercado.domain.Cupom;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CupomRepository extends JpaRepository<Cupom, Long> {

    Optional<Cupom> findByCodigoIgnoreCase(String codigo);

    Optional<Cupom> findByCodigoIgnoreCaseAndAtivoTrue(String codigo);

    boolean existsByCodigoIgnoreCase(String codigo);
}
