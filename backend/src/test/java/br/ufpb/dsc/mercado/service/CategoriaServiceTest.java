package br.ufpb.dsc.mercado.service;

import br.ufpb.dsc.mercado.domain.Categoria;
import br.ufpb.dsc.mercado.dto.CategoriaDTO;
import br.ufpb.dsc.mercado.repository.CategoriaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoriaService — Testes Unitários")
class CategoriaServiceTest {

    @Mock CategoriaRepository categoriaRepository;

    private CategoriaService service;

    @BeforeEach
    void setUp() {
        service = new CategoriaService(categoriaRepository);
    }

    // Helper: cria uma categoria já com id, como viria do banco.
    private Categoria categoriaComId(Long id, String nome, String descricao) {
        Categoria c = new Categoria(nome, descricao);
        c.setId(id);
        return c;
    }

    @Test
    @DisplayName("listarTodas deve retornar todas as categorias do repositório")
    void listarTodas_deveRetornarTodas() {
        List<Categoria> categorias = List.of(categoriaComId(1L, "Brownie", "desc"));
        when(categoriaRepository.findAll()).thenReturn(categorias);

        List<Categoria> resultado = service.listarTodas();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getNome()).isEqualTo("Brownie");
    }

    @Test
    @DisplayName("listar (paginado) deve delegar ao repositório com o Pageable recebido")
    void listar_devePassarPageableAdiante() {
        Pageable pageable = Pageable.ofSize(10);
        Page<Categoria> page = new PageImpl<>(List.of(categoriaComId(1L, "Cookie", null)));
        when(categoriaRepository.findAll(pageable)).thenReturn(page);

        Page<Categoria> resultado = service.listar(pageable);

        assertThat(resultado.getContent()).hasSize(1);
        verify(categoriaRepository).findAll(pageable);
    }

    @Test
    @DisplayName("buscarPorId deve retornar a categoria quando ela existe")
    void buscarPorId_existente_deveRetornar() {
        Categoria categoria = categoriaComId(1L, "Trufa", "desc");
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));

        Categoria resultado = service.buscarPorId(1L);

        assertThat(resultado.getNome()).isEqualTo("Trufa");
    }

    @Test
    @DisplayName("buscarPorId deve lançar IllegalArgumentException quando não existe")
    void buscarPorId_inexistente_deveLancarExcecao() {
        when(categoriaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Categoria não encontrada com ID: 99");
    }

    @Test
    @DisplayName("criar deve salvar a categoria quando o nome ainda não existe")
    void criar_nomeDisponivel_deveSalvar() {
        CategoriaDTO dto = new CategoriaDTO(null, "Bolo", "Bolos diversos");
        when(categoriaRepository.existsByNomeIgnoreCase("Bolo")).thenReturn(false);
        when(categoriaRepository.save(any(Categoria.class))).thenAnswer(inv -> inv.getArgument(0));

        Categoria resultado = service.criar(dto);

        assertThat(resultado.getNome()).isEqualTo("Bolo");
        assertThat(resultado.getDescricao()).isEqualTo("Bolos diversos");
        verify(categoriaRepository).save(any(Categoria.class));
    }

    @Test
    @DisplayName("criar deve lançar exceção quando já existe categoria com o mesmo nome")
    void criar_nomeDuplicado_deveLancarExcecao() {
        CategoriaDTO dto = new CategoriaDTO(null, "Bolo", null);
        when(categoriaRepository.existsByNomeIgnoreCase("Bolo")).thenReturn(true);

        assertThatThrownBy(() -> service.criar(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Já existe uma categoria com este nome");

        verify(categoriaRepository, never()).save(any());
    }

    @Test
    @DisplayName("atualizar deve alterar nome e descrição quando o novo nome está disponível")
    void atualizar_dadosValidos_deveAtualizar() {
        Categoria existente = categoriaComId(1L, "Bolo", "antiga");
        CategoriaDTO dto = new CategoriaDTO(1L, "Bolo Premium", "nova descrição");

        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(categoriaRepository.existsByNomeIgnoreCase("Bolo Premium")).thenReturn(false);
        when(categoriaRepository.save(any(Categoria.class))).thenAnswer(inv -> inv.getArgument(0));

        Categoria resultado = service.atualizar(1L, dto);

        assertThat(resultado.getNome()).isEqualTo("Bolo Premium");
        assertThat(resultado.getDescricao()).isEqualTo("nova descrição");
    }

    @Test
    @DisplayName("atualizar deve permitir manter o mesmo nome (case-insensitive) sem disparar duplicidade")
    void atualizar_mesmoNomeMesmaCaixa_naoDeveValidarDuplicidade() {
        Categoria existente = categoriaComId(1L, "Bolo", "antiga");
        CategoriaDTO dto = new CategoriaDTO(1L, "BOLO", "nova descrição");

        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(categoriaRepository.save(any(Categoria.class))).thenAnswer(inv -> inv.getArgument(0));

        Categoria resultado = service.atualizar(1L, dto);

        assertThat(resultado.getNome()).isEqualTo("BOLO");
        verify(categoriaRepository, never()).existsByNomeIgnoreCase(anyString());
    }

    @Test
    @DisplayName("atualizar deve lançar exceção quando o novo nome já pertence a outra categoria")
    void atualizar_nomeDuplicado_deveLancarExcecao() {
        Categoria existente = categoriaComId(1L, "Bolo", "antiga");
        CategoriaDTO dto = new CategoriaDTO(1L, "Cookie", "nova descrição");

        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(categoriaRepository.existsByNomeIgnoreCase("Cookie")).thenReturn(true);

        assertThatThrownBy(() -> service.atualizar(1L, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Já existe outra categoria com este nome");

        verify(categoriaRepository, never()).save(any());
    }

    @Test
    @DisplayName("atualizar deve lançar exceção quando a categoria não existe")
    void atualizar_inexistente_deveLancarExcecao() {
        when(categoriaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.atualizar(99L, new CategoriaDTO(99L, "X", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Categoria não encontrada com ID: 99");
    }

    @Test
    @DisplayName("excluir deve remover a categoria quando ela existe")
    void excluir_existente_deveRemover() {
        when(categoriaRepository.existsById(1L)).thenReturn(true);

        service.excluir(1L);

        verify(categoriaRepository).deleteById(1L);
    }

    @Test
    @DisplayName("excluir deve lançar exceção quando a categoria não existe")
    void excluir_inexistente_deveLancarExcecao() {
        when(categoriaRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.excluir(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Categoria não encontrada com ID: 99");

        verify(categoriaRepository, never()).deleteById(any());
    }
}
