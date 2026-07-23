package br.ufpb.dsc.mercado.repository;

import br.ufpb.dsc.mercado.domain.Papel;
import br.ufpb.dsc.mercado.domain.Usuario;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    boolean existsByCpf(String cpf);

    List<Usuario> findByPapel(Papel papel);

    Page<Usuario> findByPapel(Papel papel, Pageable pageable);

    Page<Usuario> findByPapelAndNomeContainingIgnoreCase(Papel papel, String nome, Pageable pageable);
}
