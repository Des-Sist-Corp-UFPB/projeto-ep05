package br.ufpb.dsc.mercado.service;

import br.ufpb.dsc.mercado.domain.Cupom;
import br.ufpb.dsc.mercado.repository.CupomRepository;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class CupomService {

    private final CupomRepository cupomRepository;

    public CupomService(CupomRepository cupomRepository) {
        this.cupomRepository = cupomRepository;
    }

    public List<Cupom> listarTodos() {
        return cupomRepository.findAll();
    }

    public Page<Cupom> listar(Pageable pageable) {
        return cupomRepository.findAll(pageable);
    }

    public Cupom buscarPorId(Long id) {
        return cupomRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cupom não encontrado com ID: " + id));
    }

    public Cupom buscarPorCodigo(String codigo) {
        return cupomRepository.findByCodigoIgnoreCase(codigo)
                .orElseThrow(() -> new IllegalArgumentException("Cupom não encontrado com código: " + codigo));
    }

    @WithSpan("aplicar-cupom")
    public Cupom validarCupom(@SpanAttribute("cupom.codigo") String codigo) {
        Cupom cupom = cupomRepository.findByCodigoIgnoreCaseAndAtivoTrue(codigo)
                .orElseThrow(() -> new IllegalArgumentException("Cupom inválido ou inativo"));

        if (cupom.isExpirado()) {
            throw new IllegalArgumentException("Este cupom já está expirado");
        }

        // Atributos só conhecidos após a validação — setados no span atual.
        Span.current().setAttribute("cupom.tipo", cupom.getTipo().name());
        Span.current().setAttribute("cupom.desconto", cupom.getDesconto().toString());

        return cupom;
    }

    @Transactional
    public Cupom criar(Cupom cupom) {
        if (cupomRepository.existsByCodigoIgnoreCase(cupom.getCodigo())) {
            throw new IllegalArgumentException("Já existe um cupom com este código");
        }
        cupom.setAtivo(true);
        return cupomRepository.save(cupom);
    }

    @Transactional
    public Cupom atualizar(Long id, Cupom dados) {
        Cupom cupom = buscarPorId(id);
        if (!cupom.getCodigo().equalsIgnoreCase(dados.getCodigo()) && cupomRepository.existsByCodigoIgnoreCase(dados.getCodigo())) {
            throw new IllegalArgumentException("Já existe outro cupom com este código");
        }
        cupom.setCodigo(dados.getCodigo());
        cupom.setDesconto(dados.getDesconto());
        cupom.setTipo(dados.getTipo());
        cupom.setDataExpiracao(dados.getDataExpiracao());
        cupom.setAtivo(dados.getAtivo());
        return cupomRepository.save(cupom);
    }

    @Transactional
    public void alternarAtivo(Long id) {
        Cupom cupom = buscarPorId(id);
        cupom.setAtivo(!cupom.getAtivo());
        cupomRepository.save(cupom);
    }

    @Transactional
    public void excluir(Long id) {
        if (!cupomRepository.existsById(id)) {
            throw new IllegalArgumentException("Cupom não encontrado com ID: " + id);
        }
        cupomRepository.deleteById(id);
    }
}
