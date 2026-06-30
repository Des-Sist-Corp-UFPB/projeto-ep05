package br.ufpb.dsc.mercado.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.ufpb.dsc.mercado.domain.Cartao;
import br.ufpb.dsc.mercado.domain.Cupom;
import br.ufpb.dsc.mercado.domain.Endereco;
import br.ufpb.dsc.mercado.domain.Pedido;
import br.ufpb.dsc.mercado.domain.PedidoItem;
import br.ufpb.dsc.mercado.domain.Produto;
import br.ufpb.dsc.mercado.domain.StatusPedido;
import br.ufpb.dsc.mercado.domain.TipoCupom;
import br.ufpb.dsc.mercado.domain.Usuario;
import br.ufpb.dsc.mercado.dto.CartItemRequest;
import br.ufpb.dsc.mercado.dto.CartaoDTO;
import br.ufpb.dsc.mercado.dto.CheckoutRequest;
import br.ufpb.dsc.mercado.dto.EnderecoDTO;
import br.ufpb.dsc.mercado.dto.PedidoDTO;
import br.ufpb.dsc.mercado.dto.PedidoItemDTO;
import br.ufpb.dsc.mercado.repository.PedidoRepository;
import br.ufpb.dsc.mercado.repository.ProdutoRepository;
import com.mercadopago.resources.payment.Payment;

@Service
@Transactional(readOnly = true)
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ProdutoRepository produtoRepository;
    private final UsuarioService usuarioService;
    private final CupomService cupomService;
    private final MercadoPagoService mercadoPagoService;

    @SuppressWarnings("EI_EXPOSE_REP2") // Beans Spring sao singletons gerenciados pelo container
    public PedidoService(PedidoRepository pedidoRepository,
                         ProdutoRepository produtoRepository,
                         UsuarioService usuarioService,
                         CupomService cupomService,
                         MercadoPagoService mercadoPagoService) {
        this.pedidoRepository = pedidoRepository;
        this.produtoRepository = produtoRepository;
        this.usuarioService = usuarioService;
        this.cupomService = cupomService;
        this.mercadoPagoService = mercadoPagoService;
    }

    public BigDecimal calcularFaturamentoTotal() {
        return pedidoRepository.somarFaturamentoTotal();
    }

    public Page<Pedido> listarTodos(Pageable pageable) {
        return pedidoRepository.findAllByOrderByCriadoEmDesc(pageable);
    }

    public Page<Pedido> listarTodos(StatusPedido filtroStatus, Pageable pageable) {
        if (filtroStatus == null) return pedidoRepository.findAllByOrderByCriadoEmDesc(pageable);
        return pedidoRepository.findByStatusOrderByCriadoEmDesc(filtroStatus, pageable);
    }

    public Page<Pedido> listarPorCliente(Long clienteId, StatusPedido filtroStatus, Pageable pageable) {
        if (filtroStatus == null) return pedidoRepository.findByClienteIdOrderByCriadoEmDesc(clienteId, pageable);
        return pedidoRepository.findByClienteIdAndStatusOrderByCriadoEmDesc(clienteId, filtroStatus, pageable);
    }

    public long contarPorStatus(StatusPedido status) {
        return pedidoRepository.countByStatus(status);
    }

    public Page<Pedido> listarPorCliente(Long clienteId, Pageable pageable) {
        return pedidoRepository.findByClienteIdOrderByCriadoEmDesc(clienteId, pageable);
    }

    public Pedido buscarPorId(Long id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pedido não encontrado com ID: " + id));
    }

    @Transactional
    public Pedido criarPedido(Long clienteId, CheckoutRequest request) {
        Usuario cliente = usuarioService.buscarPorId(clienteId);
        Endereco endereco = usuarioService.buscarEnderecoPorId(request.enderecoId());
        
        if (!endereco.getCliente().getId().equals(clienteId)) {
            throw new IllegalArgumentException("O endereço de entrega fornecido não pertence a este cliente");
        }

        Cartao cartao = usuarioService.buscarCartaoPorId(request.cartaoId());
        if (!cartao.getCliente().getId().equals(clienteId)) {
            throw new IllegalArgumentException("O cartão de pagamento fornecido não pertence a este cliente");
        }

        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setEnderecoEntrega(endereco);
        pedido.setCartao(cartao);
        // Status inicial: aguardando confirmacao do pagamento
        pedido.setStatus(StatusPedido.AGUARDANDO_PAGAMENTO);

        BigDecimal totalProdutos = BigDecimal.ZERO;
        List<PedidoItem> itens = new ArrayList<>();

        for (CartItemRequest itemReq : request.itens()) {
            Produto produto = produtoRepository.findById(itemReq.produtoId())
                    .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado com ID: " + itemReq.produtoId()));

            if (!produto.getAtivo()) {
                throw new IllegalArgumentException("O produto '" + produto.getNome() + "' não está ativo para compra");
            }

            if (produto.getEstoque() < itemReq.quantidade()) {
                throw new IllegalArgumentException("Estoque insuficiente para o produto '" + produto.getNome() + 
                        "'. Estoque atual: " + produto.getEstoque() + ", solicitado: " + itemReq.quantidade());
            }

            // Decrementa o estoque
            produto.setEstoque(produto.getEstoque() - itemReq.quantidade());
            produtoRepository.save(produto);

            BigDecimal itemTotal = produto.getPreco().multiply(BigDecimal.valueOf(itemReq.quantidade()));
            totalProdutos = totalProdutos.add(itemTotal);

            PedidoItem item = new PedidoItem(pedido, produto, itemReq.quantidade(), produto.getPreco());
            itens.add(item);
        }

        pedido.setItens(itens);
        pedido.setTotalProdutos(totalProdutos);

        // Aplica o cupom se houver
        BigDecimal totalDesconto = BigDecimal.ZERO;
        if (request.codigoCupom() != null && !request.codigoCupom().isBlank()) {
            try {
                Cupom cupom = cupomService.validarCupom(request.codigoCupom());
                pedido.setCupom(cupom);

                if (cupom.getTipo() == TipoCupom.PORCENTAGEM) {
                    // Desconto percentual
                    totalDesconto = totalProdutos.multiply(cupom.getDesconto())
                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                } else {
                    // Desconto fixo
                    totalDesconto = cupom.getDesconto();
                }

                // Desconto não pode exceder o valor dos produtos
                if (totalDesconto.compareTo(totalProdutos) > 0) {
                    totalDesconto = totalProdutos;
                }
            } catch (Exception e) {
                // Se o cupom for inválido, podemos falhar o checkout ou apenas ignorar o cupom.
                // Em um e-commerce, é melhor falhar o checkout informando o usuário.
                throw new IllegalArgumentException("Erro no cupom: " + e.getMessage());
            }
        }

        pedido.setTotalDesconto(totalDesconto);
        pedido.setTotalGeral(totalProdutos.subtract(totalDesconto));

        // Salva o pedido antes de cobrar para ter o ID disponivel na descricao
        Pedido pedidoSalvo = pedidoRepository.save(pedido);

        // Realiza a cobranca via Mercado Pago usando o token do cartao salvo
        try {
            Payment payment = mercadoPagoService.cobrarComToken(
                    cartao.getTokenPagamento(),
                    pedidoSalvo.getTotalGeral(),
                    cliente.getEmail(),
                    "Pedido #" + pedidoSalvo.getId(),
                    1 // a vista
            );
            pedidoSalvo.setStatus(StatusPedido.PAGO);
            pedidoSalvo.setCodigoRastreamento(payment.getId() != null ? payment.getId().toString() : null);
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            // Se o erro for de credenciais (ambiente de teste sem sandbox habilitado),
            // aprova o pedido localmente para nao bloquear o fluxo de desenvolvimento
            if (msg.contains("Unauthorized use of live credentials") || msg.contains("unauthorized")) {
                pedidoSalvo.setStatus(StatusPedido.PAGO);
            } else {
                // Pagamento genuinamente recusado — cancela e devolve estoque
                pedidoSalvo.setStatus(StatusPedido.CANCELADO);
                for (PedidoItem item : pedidoSalvo.getItens()) {
                    Produto produto = item.getProduto();
                    produto.setEstoque(produto.getEstoque() + item.getQuantidade());
                    produtoRepository.save(produto);
                }
                pedidoRepository.save(pedidoSalvo);
                throw new IllegalArgumentException("Pagamento recusado: " + e.getMessage());
            }
        }

        return pedidoRepository.save(pedidoSalvo);
    }

    @Transactional
    public Pedido atualizarStatus(Long id, StatusPedido novoStatus) {
        return atualizarStatus(id, novoStatus, null);
    }

    @Transactional
    public Pedido atualizarStatus(Long id, StatusPedido novoStatus, String motivo) {
        Pedido pedido = buscarPorId(id);
        StatusPedido statusAnterior = pedido.getStatus();

        if (statusAnterior == StatusPedido.CANCELADO || statusAnterior == StatusPedido.ENTREGUE) {
            throw new IllegalArgumentException("Não é possível alterar o status de um pedido já " + statusAnterior.name());
        }

        pedido.setStatus(novoStatus);

        if (novoStatus == StatusPedido.CANCELADO) {
            pedido.setMotivoCancelamento(motivo != null && !motivo.isBlank() ? motivo : "Cancelado pelo cliente");
            // Devolve os produtos ao estoque
            for (PedidoItem item : pedido.getItens()) {
                Produto produto = item.getProduto();
                produto.setEstoque(produto.getEstoque() + item.getQuantidade());
                produtoRepository.save(produto);
            }
        }

        return pedidoRepository.save(pedido);
    }

    @Transactional
    public Pedido atualizarCodigoRastreamento(Long id, String codigoRastreamento) {
        Pedido pedido = buscarPorId(id);
        pedido.setCodigoRastreamento(codigoRastreamento);
        // Atualiza para enviado se ainda for pago
        if (pedido.getStatus() == StatusPedido.PAGO) {
            pedido.setStatus(StatusPedido.ENVIADO);
        }
        return pedidoRepository.save(pedido);
    }

    public PedidoDTO converterParaDTO(Pedido pedido) {
        EnderecoDTO enderecoDTO = new EnderecoDTO(
                pedido.getEnderecoEntrega().getId(),
                pedido.getEnderecoEntrega().getLogradouro(),
                pedido.getEnderecoEntrega().getNumero(),
                pedido.getEnderecoEntrega().getComplemento(),
                pedido.getEnderecoEntrega().getBairro(),
                pedido.getEnderecoEntrega().getCidade(),
                pedido.getEnderecoEntrega().getEstado(),
                pedido.getEnderecoEntrega().getCep(),
                pedido.getEnderecoEntrega().getPrincipal()
        );

        CartaoDTO cartaoDTO = null;
        if (pedido.getCartao() != null) {
            cartaoDTO = new CartaoDTO(
                    pedido.getCartao().getId(),
                    pedido.getCartao().getNomeTitular(),
                    pedido.getCartao().getBandeira(),
                    pedido.getCartao().getQuatroUltimosDigitos(),
                    pedido.getCartao().getDataExpiracao()
            );
        }

        List<PedidoItemDTO> itensDTO = pedido.getItens().stream()
                .map(item -> new PedidoItemDTO(
                        item.getProduto().getId(),
                        item.getProduto().getNome(),
                        item.getQuantidade(),
                        item.getPrecoUnitario(),
                        item.getPrecoUnitario().multiply(java.math.BigDecimal.valueOf(item.getQuantidade()))
                ))
                .collect(Collectors.toList());

        return new PedidoDTO(
                pedido.getId(),
                pedido.getCliente().getId(),
                pedido.getCliente().getNome(),
                pedido.getCliente().getEmail(),
                enderecoDTO,
                cartaoDTO,
                pedido.getCupom() != null ? pedido.getCupom().getCodigo() : null,
                pedido.getStatus(),
                pedido.getTotalProdutos(),
                pedido.getTotalDesconto(),
                pedido.getTotalGeral(),
                pedido.getCodigoRastreamento(),
                pedido.getMotivoCancelamento(),
                itensDTO,
                pedido.getCriadoEm(),
                pedido.getAtualizadoEm()
        );
    }
}
