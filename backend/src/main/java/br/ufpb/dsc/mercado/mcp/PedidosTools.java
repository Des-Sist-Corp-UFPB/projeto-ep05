package br.ufpb.dsc.mercado.mcp;

import java.util.List;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import br.ufpb.dsc.mercado.dto.PedidoDTO;
import br.ufpb.dsc.mercado.dto.ProdutoDTO;
import br.ufpb.dsc.mercado.service.PedidoService;
import br.ufpb.dsc.mercado.service.ProdutoService;

/**
 * Camada fina de "tools" MCP: cada método aqui apenas chama um service que já
 * existe no projeto. Nenhuma regra de negócio nova é escrita aqui.
 */
@Service
public class PedidosTools {

    private final ProdutoService produtoService;
    private final PedidoService pedidoService;

    public PedidosTools(ProdutoService produtoService, PedidoService pedidoService) {
        this.produtoService = produtoService;
        this.pedidoService = pedidoService;
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

    @Tool(description = "Consulta a situação atual de um pedido (status, código de rastreamento, itens e totais). "
            + "Use quando o usuário perguntar sobre o andamento, status ou rastreio de um pedido.")
    public PedidoDTO rastrearPedido(
            @ToolParam(description = "ID numérico do pedido a consultar")
            Long pedidoId) {
        return pedidoService.converterParaDTO(pedidoService.buscarPorId(pedidoId));
    }
}