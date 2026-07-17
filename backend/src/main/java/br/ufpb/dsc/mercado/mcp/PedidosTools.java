package br.ufpb.dsc.mercado.mcp;

import java.util.List;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import br.ufpb.dsc.mercado.audit.AuditoriaService;
import br.ufpb.dsc.mercado.domain.Pedido;
import br.ufpb.dsc.mercado.domain.Usuario;
import br.ufpb.dsc.mercado.dto.CartItemRequest;
import br.ufpb.dsc.mercado.dto.CheckoutRequest;
import br.ufpb.dsc.mercado.dto.PedidoDTO;
import br.ufpb.dsc.mercado.dto.ProdutoDTO;
import br.ufpb.dsc.mercado.service.PedidoService;
import br.ufpb.dsc.mercado.service.ProdutoService;
import br.ufpb.dsc.mercado.service.UsuarioService;

/**
 * Camada fina de "tools" MCP: cada método aqui apenas chama um service que já
 * existe no projeto. Nenhuma regra de negócio nova é escrita aqui.
 *
 * <p>O cliente é sempre identificado explicitamente pelo e-mail informado ao
 * assistente de IA (não existe sessão/cookie no transporte MCP). Toda tool que
 * expõe ou altera dados de um pedido confere que o pedido pertence a esse
 * cliente antes de responder, e toda tool de escrita registra um evento no
 * log de auditoria (categoria {@code PEDIDO}, ator = e-mail do cliente).
 */
@Service
public class PedidosTools {

    private final ProdutoService produtoService;
    private final PedidoService pedidoService;
    private final UsuarioService usuarioService;
    private final AuditoriaService auditoriaService;

    public PedidosTools(ProdutoService produtoService,
                         PedidoService pedidoService,
                         UsuarioService usuarioService,
                         AuditoriaService auditoriaService) {
        this.produtoService = produtoService;
        this.pedidoService = pedidoService;
        this.usuarioService = usuarioService;
        this.auditoriaService = auditoriaService;
    }

    @Tool(description = "Lista os produtos ativos do cardápio da confeitaria. "
            + "Use quando o usuário pedir para ver o catálogo, cardápio ou buscar um produto pelo nome.")
    public List<ProdutoDTO> catalogo(
            @ToolParam(description = "Termo de busca pelo nome do produto (opcional, deixe vazio para listar tudo)", required = false)
            String busca) {
        return produtoService.buscarAtivos(busca, PageRequest.of(0, 20))
                .map(produtoService::converterParaDTO)
                .getContent();
    }

    @Tool(description = "Consulta a situação atual de um pedido (status, código de rastreamento, itens e totais) "
            + "de um cliente específico. Use quando o usuário perguntar sobre o andamento, status ou rastreio de um pedido.")
    public PedidoDTO rastrearPedido(
            @ToolParam(description = "E-mail do cliente dono do pedido, usado para confirmar que ele tem permissão de vê-lo")
            String clienteEmail,
            @ToolParam(description = "ID numérico do pedido a consultar")
            Long pedidoId) {
        Pedido pedido = pedidoService.buscarPorId(pedidoId);
        verificarPropriedade(pedido, clienteEmail);
        return pedidoService.converterParaDTO(pedido);
    }

    @Tool(description = "Monta e finaliza um novo pedido para um cliente já cadastrado, cobrando com um cartão "
            + "e endereço que ele já cadastrou previamente na loja. Use quando o usuário pedir para montar, "
            + "fechar ou finalizar uma compra/pedido.")
    public PedidoDTO montarPedido(
            @ToolParam(description = "E-mail do cliente que está fazendo o pedido")
            String clienteEmail,
            @ToolParam(description = "Itens do pedido: cada item traz o ID do produto e a quantidade desejada")
            List<CartItemRequest> itens,
            @ToolParam(description = "ID do endereço de entrega já cadastrado do cliente")
            Long enderecoId,
            @ToolParam(description = "ID do cartão de pagamento já cadastrado do cliente")
            Long cartaoId,
            @ToolParam(description = "Código de um cupom de desconto a aplicar (opcional)", required = false)
            String codigoCupom) {
        Usuario cliente = usuarioService.buscarPorEmail(clienteEmail);

        CheckoutRequest request = new CheckoutRequest(enderecoId, cartaoId, codigoCupom, itens);
        Pedido pedido = pedidoService.criarPedido(cliente.getId(), request);

        auditoriaService.registrarCliente(
                cliente.getEmail(),
                "PEDIDO",
                "Pedido #" + pedido.getId() + " criado via assistente de IA (MCP)",
                pedido.getId());

        return pedidoService.converterParaDTO(pedido);
    }

    private void verificarPropriedade(Pedido pedido, String clienteEmail) {
        String donoDoPedido = pedido.getCliente().getEmail();
        if (clienteEmail == null || !donoDoPedido.equalsIgnoreCase(clienteEmail.trim())) {
            throw new IllegalArgumentException("Este pedido não pertence ao cliente informado");
        }
    }
}