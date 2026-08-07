package br.ufpb.dsc.mercado.service;

import br.ufpb.dsc.mercado.domain.Categoria;
import br.ufpb.dsc.mercado.dto.CategoriaDTO;
import br.ufpb.dsc.mercado.repository.CategoriaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    public CategoriaService(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    public List<Categoria> listarTodas() {
        return categoriaRepository.findAll();
    }

    public Page<Categoria> listar(Pageable pageable) {
        return categoriaRepository.findAll(pageable);
    }

    public Categoria buscarPorId(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Categoria não encontrada com ID: " + id));
    }

    @Transactional
    public Categoria criar(CategoriaDTO dto) {
        if (categoriaRepository.existsByNomeIgnoreCase(dto.nome())) {
            throw new IllegalArgumentException("Já existe uma categoria com este nome");
        }
        Categoria categoria = new Categoria(dto.nome(), dto.descricao());
        return categoriaRepository.save(categoria);
    }

    @Transactional
    public Categoria atualizar(Long id, CategoriaDTO dto) {
        Categoria categoria = buscarPorId(id);
        if (!categoria.getNome().equalsIgnoreCase(dto.nome()) && categoriaRepository.existsByNomeIgnoreCase(dto.nome())) {
            throw new IllegalArgumentException("Já existe outra categoria com este nome");
        }
        categoria.setNome(dto.nome());
        categoria.setDescricao(dto.descricao());
        return categoriaRepository.save(categoria);
    }

    @Transactional
    public void excluir(Long id) {
        if (!categoriaRepository.existsById(id)) {
            throw new IllegalArgumentException("Categoria não encontrada com ID: " + id);
        }
        categoriaRepository.deleteById(id);
    }
}
