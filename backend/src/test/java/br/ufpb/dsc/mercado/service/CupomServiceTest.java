package br.ufpb.dsc.mercado.service;

import br.ufpb.dsc.mercado.domain.Cupom;
import br.ufpb.dsc.mercado.domain.TipoCupom;
import br.ufpb.dsc.mercado.repository.CupomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CupomService — Testes Unitários")
class CupomServiceTest {

    @Mock CupomRepository cupomRepository;

    private CupomService service;

    @BeforeEach
    void setUp() {
        service = new CupomService(cupomRepository);
    }

    private Cupom cupomComId(Long id, String codigo, Instant expiracao) {
        Cupom c = new Cupom(codigo, BigDecimal.TEN, TipoCupom.PORCENTAGEM, expiracao);
        c.setId(id);
        return c;
    }

    @Test
    @DisplayName("listarTodos deve retornar todos os cupons")
    void listarTodos_deveRetornarTodos() {
        when(cupomRepository.findAll()).thenReturn(List.of(cupomComId(1L, "PROMO10", Instant.now().plusSeconds(3600))));

        List<Cupom> resultado = service.listarTodos();

        assertThat(resultado).hasSize(1);
    }

    @Test
    @DisplayName("listar (paginado) deve delegar ao repositório")
    void listar_devePassarPageableAdiante() {
        Pageable pageable = Pageable.ofSize(10);
        Page<Cupom> page = new PageImpl<>(List.of(cupomComId(1L, "PROMO10", Instant.now().plusSeconds(3600))));
        when(cupomRepository.findAll(pageable)).thenReturn(page);

        Page<Cupom> resultado = service.listar(pageable);

        assertThat(resultado.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("buscarPorId deve retornar o cupom quando existe")
    void buscarPorId_existente_deveRetornar() {
        Cupom cupom = cupomComId(1L, "PROMO10", Instant.now().plusSeconds(3600));
        when(cupomRepository.findById(1L)).thenReturn(Optional.of(cupom));

        assertThat(service.buscarPorId(1L).getCodigo()).isEqualTo("PROMO10");
    }

    @Test
    @DisplayName("buscarPorId deve lançar exceção quando não existe")
    void buscarPorId_inexistente_deveLancarExcecao() {
        when(cupomRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cupom não encontrado com ID: 99");
    }

    @Test
    @DisplayName("buscarPorCodigo deve retornar o cupom quando existe")
    void buscarPorCodigo_existente_deveRetornar() {
        Cupom cupom = cupomComId(1L, "PROMO10", Instant.now().plusSeconds(3600));
        when(cupomRepository.findByCodigoIgnoreCase("PROMO10")).thenReturn(Optional.of(cupom));

        assertThat(service.buscarPorCodigo("PROMO10").getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("buscarPorCodigo deve lançar exceção quando o código não existe")
    void buscarPorCodigo_inexistente_deveLancarExcecao() {
        when(cupomRepository.findByCodigoIgnoreCase("INEXISTENTE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorCodigo("INEXISTENTE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cupom não encontrado com código: INEXISTENTE");
    }

    @Test
    @DisplayName("validarCupom deve retornar o cupom quando ativo e não expirado")
    void validarCupom_ativoEValido_deveRetornar() {
        Cupom cupom = cupomComId(1L, "PROMO10", Instant.now().plus(1, ChronoUnit.DAYS));
        when(cupomRepository.findByCodigoIgnoreCaseAndAtivoTrue("PROMO10")).thenReturn(Optional.of(cupom));

        assertThat(service.validarCupom("PROMO10")).isEqualTo(cupom);
    }

    @Test
    @DisplayName("validarCupom deve lançar exceção quando o cupom não existe ou está inativo")
    void validarCupom_inexistenteOuInativo_deveLancarExcecao() {
        when(cupomRepository.findByCodigoIgnoreCaseAndAtivoTrue("X")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.validarCupom("X"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cupom inválido ou inativo");
    }

    @Test
    @DisplayName("validarCupom deve lançar exceção quando o cupom está expirado")
    void validarCupom_expirado_deveLancarExcecao() {
        Cupom cupom = cupomComId(1L, "VENCIDO", Instant.now().minus(1, ChronoUnit.DAYS));
        when(cupomRepository.findByCodigoIgnoreCaseAndAtivoTrue("VENCIDO")).thenReturn(Optional.of(cupom));

        assertThatThrownBy(() -> service.validarCupom("VENCIDO"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Este cupom já está expirado");
    }

    @Test
    @DisplayName("criar deve salvar e ativar o cupom quando o código está disponível")
    void criar_codigoDisponivel_deveSalvarAtivo() {
        Cupom novo = new Cupom("NOVO10", BigDecimal.TEN, TipoCupom.PORCENTAGEM, Instant.now().plusSeconds(3600));
        novo.setAtivo(false);
        when(cupomRepository.existsByCodigoIgnoreCase("NOVO10")).thenReturn(false);
        when(cupomRepository.save(any(Cupom.class))).thenAnswer(inv -> inv.getArgument(0));

        Cupom resultado = service.criar(novo);

        assertThat(resultado.getAtivo()).isTrue();
        verify(cupomRepository).save(novo);
    }

    @Test
    @DisplayName("criar deve lançar exceção quando o código já existe")
    void criar_codigoDuplicado_deveLancarExcecao() {
        Cupom novo = new Cupom("DUPLICADO", BigDecimal.TEN, TipoCupom.PORCENTAGEM, Instant.now().plusSeconds(3600));
        when(cupomRepository.existsByCodigoIgnoreCase("DUPLICADO")).thenReturn(true);

        assertThatThrownBy(() -> service.criar(novo))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Já existe um cupom com este código");

        verify(cupomRepository, never()).save(any());
    }

    @Test
    @DisplayName("atualizar deve sobrescrever todos os campos quando o novo código está disponível")
    void atualizar_dadosValidos_deveAtualizarTodosOsCampos() {
        Cupom existente = cupomComId(1L, "ANTIGO", Instant.now().plusSeconds(3600));
        Instant novaExpiracao = Instant.now().plus(2, ChronoUnit.DAYS);
        Cupom dados = new Cupom("NOVO", BigDecimal.valueOf(20), TipoCupom.VALOR_FIXO, novaExpiracao);
        dados.setAtivo(false);

        when(cupomRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(cupomRepository.existsByCodigoIgnoreCase("NOVO")).thenReturn(false);
        when(cupomRepository.save(any(Cupom.class))).thenAnswer(inv -> inv.getArgument(0));

        Cupom resultado = service.atualizar(1L, dados);

        assertThat(resultado.getCodigo()).isEqualTo("NOVO");
        assertThat(resultado.getDesconto()).isEqualByComparingTo(BigDecimal.valueOf(20));
        assertThat(resultado.getTipo()).isEqualTo(TipoCupom.VALOR_FIXO);
        assertThat(resultado.getDataExpiracao()).isEqualTo(novaExpiracao);
        assertThat(resultado.getAtivo()).isFalse();
    }

    @Test
    @DisplayName("atualizar deve lançar exceção quando o novo código já pertence a outro cupom")
    void atualizar_codigoDuplicado_deveLancarExcecao() {
        Cupom existente = cupomComId(1L, "ANTIGO", Instant.now().plusSeconds(3600));
        Cupom dados = new Cupom("OUTRO", BigDecimal.TEN, TipoCupom.PORCENTAGEM, Instant.now().plusSeconds(3600));

        when(cupomRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(cupomRepository.existsByCodigoIgnoreCase("OUTRO")).thenReturn(true);

        assertThatThrownBy(() -> service.atualizar(1L, dados))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Já existe outro cupom com este código");

        verify(cupomRepository, never()).save(any());
    }

    @Test
    @DisplayName("alternarAtivo deve inverter o status atual do cupom")
    void alternarAtivo_deveInverterStatus() {
        Cupom cupom = cupomComId(1L, "PROMO10", Instant.now().plusSeconds(3600));
        cupom.setAtivo(true);
        when(cupomRepository.findById(1L)).thenReturn(Optional.of(cupom));
        when(cupomRepository.save(any(Cupom.class))).thenAnswer(inv -> inv.getArgument(0));

        service.alternarAtivo(1L);

        assertThat(cupom.getAtivo()).isFalse();
        verify(cupomRepository).save(cupom);
    }

    @Test
    @DisplayName("excluir deve remover o cupom quando ele existe")
    void excluir_existente_deveRemover() {
        when(cupomRepository.existsById(1L)).thenReturn(true);

        service.excluir(1L);

        verify(cupomRepository).deleteById(1L);
    }

    @Test
    @DisplayName("excluir deve lançar exceção quando o cupom não existe")
    void excluir_inexistente_deveLancarExcecao() {
        when(cupomRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.excluir(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cupom não encontrado com ID: 99");

        verify(cupomRepository, never()).deleteById(any());
    }
}
