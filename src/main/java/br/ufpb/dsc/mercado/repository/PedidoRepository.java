package br.ufpb.dsc.mercado.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import br.ufpb.dsc.mercado.domain.Pedido;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    List<Pedido> findByClienteIdOrderByCriadoEmDesc(Long clienteId);

    Page<Pedido> findByClienteIdOrderByCriadoEmDesc(Long clienteId, Pageable pageable);

    Page<Pedido> findAllByOrderByCriadoEmDesc(Pageable pageable);

    @Query("SELECT COALESCE(SUM(p.totalGeral), 0) FROM Pedido p")
    BigDecimal somarFaturamentoTotal();
}
