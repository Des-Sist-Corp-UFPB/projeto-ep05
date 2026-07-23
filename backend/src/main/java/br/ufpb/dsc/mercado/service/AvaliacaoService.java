package br.ufpb.dsc.mercado.service;

import br.ufpb.dsc.mercado.domain.Avaliacao;
import br.ufpb.dsc.mercado.domain.Produto;
import br.ufpb.dsc.mercado.domain.Usuario;
import br.ufpb.dsc.mercado.dto.AvaliacaoDTO;
import br.ufpb.dsc.mercado.dto.AvaliacaoSalvarDTO;
import br.ufpb.dsc.mercado.repository.AvaliacaoRepository;
import br.ufpb.dsc.mercado.repository.ProdutoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AvaliacaoService {

    private final AvaliacaoRepository avaliacaoRepository;
    private final ProdutoRepository produtoRepository;
    private final UsuarioService usuarioService;

    @SuppressWarnings("EI_EXPOSE_REP2") // Beans Spring são singletons gerenciados pelo container
    public AvaliacaoService(AvaliacaoRepository avaliacaoRepository,
                            ProdutoRepository produtoRepository,
                            UsuarioService usuarioService) {
        this.avaliacaoRepository = avaliacaoRepository;
        this.produtoRepository = produtoRepository;
        this.usuarioService = usuarioService;
    }

    public List<Avaliacao> listarPorProduto(Long produtoId) {
        return avaliacaoRepository.findByProdutoIdOrderByCriadoEmDesc(produtoId);
    }

    @Transactional
    public Avaliacao criarAvaliacao(Long clienteId, AvaliacaoSalvarDTO dto) {
        if (avaliacaoRepository.existsByProdutoIdAndClienteId(dto.produtoId(), clienteId)) {
            throw new IllegalArgumentException("Você já avaliou este produto. Só é permitida uma avaliação por produto.");
        }

        Produto produto = produtoRepository.findById(dto.produtoId())
                .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado com ID: " + dto.produtoId()));

        Usuario cliente = usuarioService.buscarPorId(clienteId);

        Avaliacao avaliacao = new Avaliacao(produto, cliente, dto.nota(), dto.comentario());
        return avaliacaoRepository.save(avaliacao);
    }

    public AvaliacaoDTO converterParaDTO(Avaliacao avaliacao) {
        return new AvaliacaoDTO(
                avaliacao.getId(),
                avaliacao.getCliente().getNome(),
                avaliacao.getNota(),
                avaliacao.getComentario(),
                avaliacao.getCriadoEm()
        );
    }
}
