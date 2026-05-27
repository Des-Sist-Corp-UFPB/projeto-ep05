package br.ufpb.dsc.mercado.service;

import br.ufpb.dsc.mercado.domain.*;
import br.ufpb.dsc.mercado.dto.*;
import br.ufpb.dsc.mercado.repository.PedidoRepository;
import br.ufpb.dsc.mercado.repository.ProdutoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ProdutoRepository produtoRepository;
    private final UsuarioService usuarioService;
    private final CupomService cupomService;

    public PedidoService(PedidoRepository pedidoRepository,
                         ProdutoRepository produtoRepository,
                         UsuarioService usuarioService,
                         CupomService cupomService) {
        this.pedidoRepository = pedidoRepository;
        this.produtoRepository = produtoRepository;
        this.usuarioService = usuarioService;
        this.cupomService = cupomService;
    }

    public Page<Pedido> listarTodos(Pageable pageable) {
        return pedidoRepository.findAllByOrderByCriadoEmDesc(pageable);
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
        pedido.setStatus(StatusPedido.PAGO); // Simula que o pagamento foi autorizado com sucesso imediatamente

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

        return pedidoRepository.save(pedido);
    }

    @Transactional
    public Pedido atualizarStatus(Long id, StatusPedido novoStatus) {
        Pedido pedido = buscarPorId(id);
        StatusPedido statusAnterior = pedido.getStatus();

        if (statusAnterior == StatusPedido.CANCELADO || statusAnterior == StatusPedido.ENTREGUE) {
            throw new IllegalArgumentException("Não é possível alterar o status de um pedido já " + statusAnterior.name());
        }

        pedido.setStatus(novoStatus);

        // Se o pedido for CANCELADO, devolvemos os produtos ao estoque
        if (novoStatus == StatusPedido.CANCELADO) {
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
                        item.getPrecoUnitario()
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
                itensDTO,
                pedido.getCriadoEm(),
                pedido.getAtualizadoEm()
        );
    }
}
