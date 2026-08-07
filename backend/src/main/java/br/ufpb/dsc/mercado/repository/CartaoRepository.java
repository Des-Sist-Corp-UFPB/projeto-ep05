package br.ufpb.dsc.mercado.repository;

import br.ufpb.dsc.mercado.domain.Cartao;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CartaoRepository extends JpaRepository<Cartao, Long> {

    List<Cartao> findByClienteId(Long clienteId);
}
