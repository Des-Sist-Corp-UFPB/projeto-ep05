package br.ufpb.dsc.mercado.controller;

import br.ufpb.dsc.mercado.domain.Avaliacao;
import br.ufpb.dsc.mercado.domain.Categoria;
import br.ufpb.dsc.mercado.domain.Produto;
import br.ufpb.dsc.mercado.domain.Usuario;
import br.ufpb.dsc.mercado.dto.*;
import br.ufpb.dsc.mercado.service.AvaliacaoService;
import br.ufpb.dsc.mercado.service.CategoriaService;
import br.ufpb.dsc.mercado.service.ProdutoService;
import br.ufpb.dsc.mercado.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/produtos")
public class ProdutoRestController {

    private final ProdutoService produtoService;
    private final CategoriaService categoriaService;
    private final AvaliacaoService avaliacaoService;
    private final UsuarioService usuarioService;

    @SuppressWarnings("EI_EXPOSE_REP2") // Beans Spring são singletons gerenciados pelo container
    @SuppressWarnings("EI_EXPOSE_REP2")
    public ProdutoRestController(ProdutoService produtoService,
                                 CategoriaService categoriaService,
                                 AvaliacaoService avaliacaoService,
                                 UsuarioService usuarioService) {
        this.produtoService = produtoService;
        this.categoriaService = categoriaService;
        this.avaliacaoService = avaliacaoService;
        this.usuarioService = usuarioService;
    }

    private Long obterClienteIdLogado() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            Usuario u = usuarioService.buscarPorEmail(email);
            return u.getId();
        } catch (Exception e) {
            return null;
        }
    }

    @GetMapping
    public ResponseEntity<Page<ProdutoDTO>> listarAtivos(
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) Long categoriaId,
            @PageableDefault(size = 12) Pageable pageable) {

        Page<Produto> produtos;
        if (categoriaId != null) {
            produtos = produtoService.buscarPorCategoriaEAtivos(categoriaId, pageable);
        } else {
            produtos = produtoService.buscarAtivos(busca, pageable);
        }

        Page<ProdutoDTO> dtos = produtos.map(produtoService::converterParaDTO);
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProdutoDTO> buscarPorId(@PathVariable Long id) {
        Produto produto = produtoService.buscarPorId(id);
        if (!produto.getAtivo()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(produtoService.converterParaDTO(produto));
    }

    @GetMapping("/categorias")
    public ResponseEntity<List<CategoriaDTO>> listarCategorias() {
        List<CategoriaDTO> dtos = categoriaService.listarTodas().stream()
                .map(c -> new CategoriaDTO(c.getId(), c.getNome(), c.getDescricao()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    // === AVALIAÇÕES ===
    @GetMapping("/{id}/avaliacoes")
    public ResponseEntity<List<AvaliacaoDTO>> listarAvaliacoes(@PathVariable Long id) {
        List<AvaliacaoDTO> dtos = avaliacaoService.listarPorProduto(id).stream()
                .map(avaliacaoService::converterParaDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/{id}/avaliacoes")
    public ResponseEntity<?> avaliarProduto(
            @PathVariable Long id,
            @Valid @RequestBody AvaliacaoSalvarDTO request) {
        
        Long clienteId = obterClienteIdLogado();
        if (clienteId == null) {
            return ResponseEntity.status(401).body("Faça login para avaliar este produto");
        }

        if (!id.equals(request.produtoId())) {
            return ResponseEntity.badRequest().body("ID do produto no caminho não coincide com o corpo");
        }

        try {
            Avaliacao a = avaliacaoService.criarAvaliacao(clienteId, request);
            return ResponseEntity.ok(avaliacaoService.converterParaDTO(a));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
