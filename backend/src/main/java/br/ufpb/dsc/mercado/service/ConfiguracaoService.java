package br.ufpb.dsc.mercado.service;

import br.ufpb.dsc.mercado.domain.ConfiguracaoSistema;
import br.ufpb.dsc.mercado.repository.ConfiguracaoSistemaRepository;
import jakarta.validation.constraints.NotBlank;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gerencia os parâmetros globais da plataforma (chave/valor), editáveis
 * pelo SysAdmin na tela de Configurações do Sistema.
 */
@Service
public class ConfiguracaoService {

    public static final String CHAVE_NOME_PLATAFORMA = "nome_plataforma";
    public static final String CHAVE_EMAIL_CONTATO = "email_contato";

    private static final String PADRAO_NOME_PLATAFORMA = "Sweet Delights Manager";
    private static final String PADRAO_EMAIL_CONTATO = "contato@sweetdelights.com";

    private final ConfiguracaoSistemaRepository repository;

    public ConfiguracaoService(ConfiguracaoSistemaRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public String obter(String chave, String valorPadrao) {
        return repository.findById(chave)
                .map(ConfiguracaoSistema::getValor)
                .filter(v -> v != null && !v.isBlank())
                .orElse(valorPadrao);
    }

    @Transactional(readOnly = true)
    public String obterNomePlataforma() {
        return obter(CHAVE_NOME_PLATAFORMA, PADRAO_NOME_PLATAFORMA);
    }

    @Transactional(readOnly = true)
    public String obterEmailContato() {
        return obter(CHAVE_EMAIL_CONTATO, PADRAO_EMAIL_CONTATO);
    }

    @Transactional
    public void salvar(@NotBlank String chave, String valor) {
        ConfiguracaoSistema cfg = repository.findById(chave)
                .orElse(new ConfiguracaoSistema(chave, null));
        cfg.setValor(valor);
        repository.save(cfg);
    }
}
