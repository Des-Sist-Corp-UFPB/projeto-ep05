package br.ufpb.dsc.mercado.controller;

import br.ufpb.dsc.mercado.domain.Cupom;
import br.ufpb.dsc.mercado.domain.Pedido;
import br.ufpb.dsc.mercado.domain.Usuario;
import br.ufpb.dsc.mercado.dto.*;
import br.ufpb.dsc.mercado.service.CupomService;
import br.ufpb.dsc.mercado.service.PedidoService;
import br.ufpb.dsc.mercado.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoRestController {

    private final PedidoService pedidoService;
    private final UsuarioService usuarioService;
    private final CupomService cupomService;

    @SuppressWarnings("EI_EXPOSE_REP2") // Beans Spring são singletons gerenciados pelo container
    public PedidoRestController(PedidoService pedidoService,
                                UsuarioService usuarioService,
                                CupomService cupomService) {
        this.pedidoService = pedidoService;
        this.usuarioService = usuarioService;
        this.cupomService = cupomService;
    }

    private Usuario obterUsuarioLogado() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioService.buscarPorEmail(email);
    }

    @PostMapping
    public ResponseEntity<?> checkout(@Valid @RequestBody CheckoutRequest request) {
        try {
            Usuario usuario = obterUsuarioLogado();
            Pedido pedido = pedidoService.criarPedido(usuario.getId(), request);
            return ResponseEntity.ok(pedidoService.converterParaDTO(pedido));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<Page<PedidoDTO>> listarPedidosDoCliente(@PageableDefault(size = 10) Pageable pageable) {
        Usuario usuario = obterUsuarioLogado();
        Page<Pedido> pedidos = pedidoService.listarPorCliente(usuario.getId(), pageable);
        return ResponseEntity.ok(pedidos.map(pedidoService::converterParaDTO));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obterPedido(@PathVariable Long id) {
        try {
            Usuario usuario = obterUsuarioLogado();
            Pedido pedido = pedidoService.buscarPorId(id);
            if (!pedido.getCliente().getId().equals(usuario.getId()) && !usuario.getPapel().name().contains("ADMIN")) {
                return ResponseEntity.status(403).body("Você não tem permissão para visualizar este pedido");
            }
            return ResponseEntity.ok(pedidoService.converterParaDTO(pedido));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/cupom/{codigo}")
    public ResponseEntity<?> validarCupom(@PathVariable String codigo) {
        try {
            Cupom cupom = cupomService.validarCupom(codigo);
            return ResponseEntity.ok(new CupomDTO(
                    cupom.getCodigo(),
                    cupom.getDesconto(),
                    cupom.getTipo().name()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Record auxiliar
    public record CupomDTO(
            String codigo,
            java.math.BigDecimal desconto,
            String tipo
    ) {}
}
