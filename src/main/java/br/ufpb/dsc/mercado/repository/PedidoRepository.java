package br.ufpb.dsc.mercado.repository;

import br.ufpb.dsc.mercado.domain.Pedido;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    List<Pedido> findByClienteIdOrderByCriadoEmDesc(Long clienteId);

    Page<Pedido> findByClienteIdOrderByCriadoEmDesc(Long clienteId, Pageable pageable);

    Page<Pedido> findAllByOrderByCriadoEmDesc(Pageable pageable);
}
