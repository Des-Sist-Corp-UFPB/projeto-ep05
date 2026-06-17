package br.ufpb.dsc.mercado.repository;

import br.ufpb.dsc.mercado.domain.RecuperacaoSenha;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface RecuperacaoSenhaRepository extends JpaRepository<RecuperacaoSenha, Long> {

    Optional<RecuperacaoSenha> findByToken(String token);

    // Invalida tokens anteriores do mesmo usuário antes de gerar um novo
    @Modifying
    @Query("UPDATE RecuperacaoSenha r SET r.usado = true WHERE r.usuario.id = :usuarioId AND r.usado = false")
    void invalidarTokensDoUsuario(Long usuarioId);
}