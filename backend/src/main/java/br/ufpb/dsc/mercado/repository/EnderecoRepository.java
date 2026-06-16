package br.ufpb.dsc.mercado.repository;

import br.ufpb.dsc.mercado.domain.Endereco;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EnderecoRepository extends JpaRepository<Endereco, Long> {

    List<Endereco> findByClienteIdOrderByCriadoEmDesc(Long clienteId);

    Optional<Endereco> findByClienteIdAndPrincipalTrue(Long clienteId);
}
