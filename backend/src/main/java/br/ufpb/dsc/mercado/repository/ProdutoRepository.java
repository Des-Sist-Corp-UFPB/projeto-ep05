package br.ufpb.dsc.mercado.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;

import br.ufpb.dsc.mercado.domain.Produto;

@Repository
public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    Page<Produto> findByNomeContainingIgnoreCase(String nome, Pageable pageable);

    Page<Produto> findByAtivoTrue(Pageable pageable);

    Page<Produto> findByAtivoTrueAndNomeContainingIgnoreCase(String nome, Pageable pageable);

    Page<Produto> findByCategoriaIdAndAtivoTrue(Long categoriaId, Pageable pageable);

    Page<Produto> findByCategoriaId(Long categoriaId, Pageable pageable);


    @Query(
            value = "SELECT p FROM Produto p",
            countQuery = "SELECT COUNT(p) FROM Produto p"
    )
     public Page<Produto> findMaisVendidos(Pageable pageable);
}
