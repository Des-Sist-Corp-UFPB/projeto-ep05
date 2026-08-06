package br.ufpb.dsc.mercado.repository;

import br.ufpb.dsc.mercado.domain.ConfiguracaoSistema;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfiguracaoSistemaRepository extends JpaRepository<ConfiguracaoSistema, String> {
}
