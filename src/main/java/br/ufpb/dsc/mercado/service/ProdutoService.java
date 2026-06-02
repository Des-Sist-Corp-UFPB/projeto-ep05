package br.ufpb.dsc.mercado.service;

import br.ufpb.dsc.mercado.domain.Categoria;
import br.ufpb.dsc.mercado.domain.Produto;
import br.ufpb.dsc.mercado.domain.ProdutoImagem;
import br.ufpb.dsc.mercado.dto.ProdutoDTO;
import br.ufpb.dsc.mercado.dto.ProdutoForm;
import br.ufpb.dsc.mercado.exception.ProdutoNaoEncontradoException;
import br.ufpb.dsc.mercado.repository.CategoriaRepository;
import br.ufpb.dsc.mercado.repository.ProdutoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ProdutoService {

    private final ProdutoRepository produtoRepository;
    private final CategoriaRepository categoriaRepository;

    public ProdutoService(ProdutoRepository produtoRepository, CategoriaRepository categoriaRepository) {
        this.produtoRepository = produtoRepository;
        this.categoriaRepository = categoriaRepository;
    }

    public Page<Produto> listar(Pageable pageable) {
        return produtoRepository.findAll(pageable);
    }

    public Page<Produto> buscar(String busca, Pageable pageable) {
        if (!StringUtils.hasText(busca)) {
            return produtoRepository.findAll(pageable);
        }
        return produtoRepository.findByNomeContainingIgnoreCase(busca.trim(), pageable);
    }

    public Page<Produto> buscarAtivos(String busca, Pageable pageable) {
        if (!StringUtils.hasText(busca)) {
            return produtoRepository.findByAtivoTrue(pageable);
        }
        return produtoRepository.findByAtivoTrueAndNomeContainingIgnoreCase(busca.trim(), pageable);
    }

    public Page<Produto> buscarPorCategoriaEAtivos(Long categoriaId, Pageable pageable) {
        return produtoRepository.findByCategoriaIdAndAtivoTrue(categoriaId, pageable);
    }

    public Page<Produto> buscarPorCategoria(Long categoriaId, Pageable pageable) {
        return produtoRepository.findByCategoriaId(categoriaId, pageable);
    }

    public Produto buscarPorId(Long id) {
        return produtoRepository.findById(id)
                .orElseThrow(() -> new ProdutoNaoEncontradoException(id));
    }

    @Transactional
    public Produto criar(ProdutoForm form) {
        Categoria categoria = null;
        if (form.categoriaId() != null) {
            categoria = categoriaRepository.findById(form.categoriaId())
                    .orElseThrow(() -> new IllegalArgumentException("Categoria não encontrada com ID: " + form.categoriaId()));
        }

        Produto produto = new Produto(
                form.nome(),
                form.descricao(),
                form.preco(),
                categoria,
                form.estoque()
        );

        if (form.ativo() != null) {
            produto.setAtivo(form.ativo());
        }

        atualizarImagens(produto, form.imagensUrls());

        return produtoRepository.save(produto);
    }

    @Transactional
    public Produto atualizar(Long id, ProdutoForm form) {
        Produto produto = buscarPorId(id);

        Categoria categoria = null;
        if (form.categoriaId() != null) {
            categoria = categoriaRepository.findById(form.categoriaId())
                    .orElseThrow(() -> new IllegalArgumentException("Categoria não encontrada com ID: " + form.categoriaId()));
        }

        produto.setNome(form.nome());
        produto.setDescricao(form.descricao());
        produto.setPreco(form.preco());
        produto.setCategoria(categoria);
        produto.setEstoque(form.estoque());
        
        if (form.ativo() != null) {
            produto.setAtivo(form.ativo());
        }

        atualizarImagens(produto, form.imagensUrls());

        return produtoRepository.save(produto);
    }

    @Transactional
    public void alternarAtivo(Long id) {
        Produto produto = buscarPorId(id);
        produto.setAtivo(!produto.getAtivo());
        produtoRepository.save(produto);
    }

    @Transactional
    public void excluir(Long id) {
        if (!produtoRepository.existsById(id)) {
            throw new ProdutoNaoEncontradoException(id);
        }
        produtoRepository.deleteById(id);
    }

    private void atualizarImagens(Produto produto, String imagensUrlsStr) {
        // Remove as antigas
        produto.getImagens().clear();

        if (StringUtils.hasText(imagensUrlsStr)) {
            // Separa URLs por vírgula ou por linha
            List<String> urls = Arrays.stream(imagensUrlsStr.split("[,\\n]"))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toList());

            for (String url : urls) {
                produto.getImagens().add(new ProdutoImagem(produto, url));
            }
        }
    }

    public ProdutoDTO converterParaDTO(Produto produto) {
        List<String> urls = produto.getImagens().stream()
                .map(ProdutoImagem::getUrl)
                .collect(Collectors.toList());

        Double notaMedia = produto.getAvaliacoes().stream()
                .mapToDouble(a -> a.getNota())
                .average()
                .orElse(0.0);

        // Arredonda nota média para 1 casa decimal
        notaMedia = Math.round(notaMedia * 10.0) / 10.0;

        return new ProdutoDTO(
                produto.getId(),
                produto.getNome(),
                produto.getDescricao(),
                produto.getPreco(),
                produto.getCategoria() != null ? produto.getCategoria().getId() : null,
                produto.getCategoria() != null ? produto.getCategoria().getNome() : null,
                produto.getEstoque(),
                produto.getAtivo(),
                urls,
                notaMedia
        );
    }
}
