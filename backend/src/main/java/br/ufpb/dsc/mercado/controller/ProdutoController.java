package br.ufpb.dsc.mercado.controller;

import br.ufpb.dsc.mercado.audit.AuditoriaService;
import br.ufpb.dsc.mercado.domain.Produto;
import br.ufpb.dsc.mercado.domain.ProdutoImagem;
import br.ufpb.dsc.mercado.dto.ProdutoForm;
import br.ufpb.dsc.mercado.exception.ProdutoNaoEncontradoException;
import br.ufpb.dsc.mercado.service.CategoriaService;
import br.ufpb.dsc.mercado.service.ProdutoService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@Controller
@RequestMapping("/produtos")
public class ProdutoController {

    private static final int TAMANHO_PAGINA = 10;
    private static final String HEADER_HTMX = "HX-Request";

    private final ProdutoService produtoService;
    private final CategoriaService categoriaService;
    private final AuditoriaService auditoriaService;

    public ProdutoController(ProdutoService produtoService,
                             CategoriaService categoriaService,
                             AuditoriaService auditoriaService) {
        this.produtoService = produtoService;
        this.categoriaService = categoriaService;
        this.auditoriaService = auditoriaService;
    }

    @GetMapping
    public String listar(
            @RequestParam(name = "busca", required = false, defaultValue = "") String busca,
            @RequestParam(name = "pagina", defaultValue = "0") int pagina,
            @RequestHeader(value = HEADER_HTMX, required = false) String htmx,
            Model model) {

        PageRequest pageRequest = PageRequest.of(pagina, TAMANHO_PAGINA, Sort.by("nome").ascending());
        Page<Produto> produtos = produtoService.buscar(busca, pageRequest);

        model.addAttribute("produtos", produtos);
        model.addAttribute("busca", busca);
        model.addAttribute("paginaAtual", pagina);

        if (htmx != null) {
            return "produtos/fragments/tabela :: tabela";
        }

        return "produtos/lista";
    }

    @GetMapping("/fragmento-tabela")
    public String fragmentoTabela(
            @RequestParam(name = "busca", required = false, defaultValue = "") String busca,
            @RequestParam(name = "pagina", defaultValue = "0") int pagina,
            Model model) {

        PageRequest pageRequest = PageRequest.of(pagina, TAMANHO_PAGINA, Sort.by("nome").ascending());
        Page<Produto> produtos = produtoService.buscar(busca, pageRequest);

        model.addAttribute("produtos", produtos);
        model.addAttribute("busca", busca);
        model.addAttribute("paginaAtual", pagina);

        return "produtos/fragments/tabela :: tabela";
    }

    @GetMapping("/novo")
    public String novoForm(Model model) {
        model.addAttribute("form", new ProdutoForm(null, null, null, null, 0, true, null));
        model.addAttribute("produto", null);
        model.addAttribute("categorias", categoriaService.listarTodas());
        return "produtos/fragments/form :: modal";
    }

    @GetMapping("/{id}/editar")
    public String editarForm(@PathVariable Long id, Model model) {
        Produto produto = produtoService.buscarPorId(id);

        String urls = produto.getImagens().stream()
                .map(ProdutoImagem::getUrl)
                .collect(Collectors.joining(", "));

        ProdutoForm form = new ProdutoForm(
                produto.getNome(),
                produto.getDescricao(),
                produto.getPreco(),
                produto.getCategoria() != null ? produto.getCategoria().getId() : null,
                produto.getEstoque(),
                produto.getAtivo(),
                urls
        );

        model.addAttribute("form", form);
        model.addAttribute("produto", produto);
        model.addAttribute("categorias", categoriaService.listarTodas());
        return "produtos/fragments/form :: modal";
    }

    @PostMapping
    public String criar(
            @Valid @ModelAttribute("form") ProdutoForm form,
            BindingResult bindingResult,
            Authentication auth,
            Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("produto", null);
            model.addAttribute("categorias", categoriaService.listarTodas());
            return "produtos/fragments/form :: modal";
        }

        Produto novoProduto = produtoService.criar(form);
        registrarProduto(auth, "PRODUTO", "Criou produto: " + novoProduto.getNome(), novoProduto.getId());
        model.addAttribute("produto", novoProduto);

        return "produtos/fragments/linha :: linha";
    }

    @PutMapping("/{id}")
    public String atualizar(
            @PathVariable Long id,
            @Valid @ModelAttribute("form") ProdutoForm form,
            BindingResult bindingResult,
            Authentication auth,
            Model model) {

        if (bindingResult.hasErrors()) {
            Produto produto = produtoService.buscarPorId(id);
            model.addAttribute("produto", produto);
            model.addAttribute("categorias", categoriaService.listarTodas());
            return "produtos/fragments/form :: modal";
        }

        Produto produtoAtualizado = produtoService.atualizar(id, form);
        registrarProduto(auth, "PRODUTO", "Atualizou produto ID " + id + ": " + produtoAtualizado.getNome(), id);
        model.addAttribute("produto", produtoAtualizado);

        return "produtos/fragments/linha :: linha";
    }

    @DeleteMapping("/{id}")
    @ResponseBody
    public ResponseEntity<Void> excluir(@PathVariable Long id, Authentication auth) {
        try {
            Produto produto = produtoService.buscarPorId(id);
            String nome = produto.getNome();
            produtoService.excluir(id);
            registrarProduto(auth, "PRODUTO", "Excluiu produto: " + nome, id);
            return ResponseEntity.ok().build();
        } catch (ProdutoNaoEncontradoException e) {
            return ResponseEntity.notFound().build();
        }
    }

    private String atorEmail(Authentication auth) {
        return (auth != null) ? auth.getName() : "desconhecido";
    }

    private boolean isSysAdmin(Authentication auth) {
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SYSADMIN"));
    }

    private void registrarProduto(Authentication auth, String categoria, String descricao, Long recursoId) {
        String email = atorEmail(auth);
        if (isSysAdmin(auth)) {
            auditoriaService.registrarSysAdmin(email, categoria, descricao, recursoId);
        } else {
            auditoriaService.registrarAdmin(email, categoria, descricao, recursoId);
        }
    }
}
