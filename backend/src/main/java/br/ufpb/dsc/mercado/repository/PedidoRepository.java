package br.ufpb.dsc.mercado.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.ufpb.dsc.mercado.domain.Pedido;
import br.ufpb.dsc.mercado.domain.StatusPedido;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    List<Pedido> findByClienteIdOrderByCriadoEmDesc(Long clienteId);

    Page<Pedido> findByClienteIdOrderByCriadoEmDesc(Long clienteId, Pageable pageable);

    Page<Pedido> findAllByOrderByCriadoEmDesc(Pageable pageable);

    // Filtro por status (admin)
    Page<Pedido> findByStatusOrderByCriadoEmDesc(StatusPedido status, Pageable pageable);

    // Filtro por status para um cliente específico
    Page<Pedido> findByClienteIdAndStatusOrderByCriadoEmDesc(Long clienteId, StatusPedido status, Pageable pageable);

    // Faturamento real: exclui pedidos CANCELADOS
    @Query("SELECT COALESCE(SUM(p.totalGeral), 0) FROM Pedido p WHERE p.status <> 'CANCELADO'")
    BigDecimal somarFaturamentoTotal();

    // Contagem por status (para o dashboard)
    long countByStatus(StatusPedido status);
}
