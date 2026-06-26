package br.ufpb.dsc.mercado.controller;

import br.ufpb.dsc.mercado.audit.AuditoriaService;
import br.ufpb.dsc.mercado.domain.Pedido;
import br.ufpb.dsc.mercado.domain.StatusPedido;
import br.ufpb.dsc.mercado.dto.PedidoDTO;
import br.ufpb.dsc.mercado.service.PedidoService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST endpoints para o painel admin gerenciar pedidos.
 * Protegido pela SecurityFilterChain do painel (session-based, role ADMIN/SYSADMIN).
 */
@RestController
@RequestMapping("/api/admin/pedidos")
public class PedidoAdminRestController {

    private final PedidoService pedidoService;
    private final AuditoriaService auditoriaService;

    @SuppressWarnings("EI_EXPOSE_REP2")
    public PedidoAdminRestController(PedidoService pedidoService, AuditoriaService auditoriaService) {
        this.pedidoService = pedidoService;
        this.auditoriaService = auditoriaService;
    }

    @GetMapping
    public ResponseEntity<Page<PedidoDTO>> listarTodos(@PageableDefault(size = 20) Pageable pageable) {
        Page<Pedido> pedidos = pedidoService.listarTodos(pageable);
        return ResponseEntity.ok(pedidos.map(pedidoService::converterParaDTO));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PedidoDTO> obterPedido(@PathVariable Long id) {
        Pedido pedido = pedidoService.buscarPorId(id);
        return ResponseEntity.ok(pedidoService.converterParaDTO(pedido));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<PedidoDTO> atualizarStatus(
            @PathVariable Long id,
            @RequestParam StatusPedido status,
            Authentication auth) {
        Pedido pedido = pedidoService.atualizarStatus(id, status);
        String ator = auth != null ? auth.getName() : "admin";
        auditoriaService.registrarAdmin(ator, "PEDIDO",
                "Atualizou status do pedido #" + id + " → " + status, id);
        return ResponseEntity.ok(pedidoService.converterParaDTO(pedido));
    }

    @PatchMapping("/{id}/rastreamento")
    public ResponseEntity<PedidoDTO> atualizarRastreamento(
            @PathVariable Long id,
            @RequestParam String codigoRastreamento,
            Authentication auth) {
        Pedido pedido = pedidoService.atualizarCodigoRastreamento(id, codigoRastreamento);
        String ator = auth != null ? auth.getName() : "admin";
        auditoriaService.registrarAdmin(ator, "PEDIDO",
                "Atualizou rastreamento do pedido #" + id + ": " + codigoRastreamento, id);
        return ResponseEntity.ok(pedidoService.converterParaDTO(pedido));
    }
}
