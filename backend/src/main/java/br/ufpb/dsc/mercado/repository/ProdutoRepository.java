package br.ufpb.dsc.mercado.repository;

import br.ufpb.dsc.mercado.domain.Produto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    Page<Produto> findByNomeContainingIgnoreCase(String nome, Pageable pageable);

    Page<Produto> findByAtivoTrue(Pageable pageable);

    Page<Produto> findByAtivoTrueAndNomeContainingIgnoreCase(String nome, Pageable pageable);

    Page<Produto> findByCategoriaIdAndAtivoTrue(Long categoriaId, Pageable pageable);

    Page<Produto> findByCategoriaId(Long categoriaId, Pageable pageable);
}
