package br.ufpb.dsc.mercado.repository;

import br.ufpb.dsc.mercado.domain.Avaliacao;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AvaliacaoRepository extends JpaRepository<Avaliacao, Long> {

    List<Avaliacao> findByProdutoIdOrderByCriadoEmDesc(Long produtoId);

    boolean existsByProdutoIdAndClienteId(Long produtoId, Long clienteId);
}
