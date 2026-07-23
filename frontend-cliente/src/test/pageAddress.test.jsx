import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, act } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return { ...actual, useNavigate: () => mockNavigate };
});

const mockBuscarCEP = vi.fn();
vi.mock('../api/cep', () => ({
  buscarCEP: (...args) => mockBuscarCEP(...args),
}));

const mockApiFetch = vi.fn();
vi.mock('../api/api', () => ({
  apiFetch: (...args) => mockApiFetch(...args),
}));

import Address from '../pages/Addresses/Address';

beforeEach(() => {
  // Timers falsos isolam cada teste: o debounce de 500ms do CEP nunca
  // "vaza" para o proximo teste (o que antes causava poluicao do mock
  // e falhas intermitentes), pois a fila de timers e descartada ao
  // restaurar os timers reais no afterEach.
  vi.useFakeTimers();
  mockNavigate.mockClear();
  mockBuscarCEP.mockReset();
  mockApiFetch.mockReset();
  // Address.jsx sempre dispara um apiFetch('/clientes/enderecos') no mount
  // (useEffect inicial). Sem um valor padrão resolvido aqui, essa chamada
  // recebe undefined e o .then() quebra antes mesmo da tela renderizar.
  // Os mockResolvedValueOnce/mockRejectedValueOnce de cada teste (para o
  // submit do formulário) continuam funcionando normalmente, pois só
  // sobrescrevem a *próxima* chamada — a chamada do mount já terá sido
  // consumida antes deles serem configurados.
  mockApiFetch.mockResolvedValue([]);
});

afterEach(() => {
  vi.useRealTimers();
});

async function renderPage() {
  const utils = render(<MemoryRouter><Address /></MemoryRouter>);
  // Aguarda o apiFetch('/clientes/enderecos') do mount resolver (microtask)
  // antes que o teste tente interagir com o formulário — senão a tela ainda
  // está em "Carregando endereços..." e os campos não existem no DOM.
  await act(async () => {
    await Promise.resolve();
  });
  return utils;
}

function preencherFormularioValido() {
  fireEvent.change(screen.getByPlaceholderText('CEP'), { target: { name: 'cep', value: '11111111' } });
  fireEvent.change(screen.getByPlaceholderText('Rua / Logradouro'), { target: { name: 'rua', value: 'Rua A' } });
  fireEvent.change(screen.getByPlaceholderText('Número'), { target: { name: 'numero', value: '100' } });
  fireEvent.change(screen.getByPlaceholderText('Bairro'), { target: { name: 'bairro', value: 'Centro' } });
  fireEvent.change(screen.getByPlaceholderText('Cidade'), { target: { name: 'cidade', value: 'Joao Pessoa' } });
  fireEvent.change(screen.getByPlaceholderText('Estado'), { target: { name: 'estado', value: 'PB' } });
}

describe('Address', () => {
  it('deve formatar o CEP ao digitar', async () => {
    await renderPage();
    const input = screen.getByPlaceholderText('CEP');
    fireEvent.change(input, { target: { name: 'cep', value: '58000000' } });
    expect(input).toHaveValue('58000-000');
  });

  it('submeter formulario vazio deve mostrar erros e nao chamar a API', async () => {
    await renderPage();
    // Limpa a chamada de mount (busca de enderecos) para isolar a checagem:
    // o que importa aqui e que o SUBMIT do formulario vazio nao dispara API.
    mockApiFetch.mockClear();
    fireEvent.click(screen.getByRole('button', { name: 'Salvar e usar este endereço' }));

    expect(screen.getByText('CEP obrigatório')).toBeInTheDocument();
    expect(mockApiFetch).not.toHaveBeenCalled();
  });

  it('salvar com sucesso deve exibir a tela de confirmacao', async () => {
    await renderPage();
    preencherFormularioValido();
    mockApiFetch.mockResolvedValueOnce({ id: 1 });

    fireEvent.click(screen.getByRole('button', { name: 'Salvar e usar este endereço' }));
    // Resolve a Promise do apiFetch (microtask) sem avancar o debounce do CEP.
    await act(async () => {
      await Promise.resolve();
    });

    expect(screen.getByText('✅ Endereço salvo!')).toBeInTheDocument();
  });

  it('falha ao salvar deve exibir a mensagem de erro da API', async () => {
    await renderPage();
    preencherFormularioValido();
    mockApiFetch.mockRejectedValueOnce({ mensagem: 'CEP inválido para entrega' });

    fireEvent.click(screen.getByRole('button', { name: 'Salvar e usar este endereço' }));
    await act(async () => {
      await Promise.resolve();
    });

    expect(screen.getByText('CEP inválido para entrega')).toBeInTheDocument();
  });

  it('falha ao salvar sem mensagem deve usar mensagem padrao', async () => {
    await renderPage();
    preencherFormularioValido();
    mockApiFetch.mockRejectedValueOnce({});

    fireEvent.click(screen.getByRole('button', { name: 'Salvar e usar este endereço' }));
    await act(async () => {
      await Promise.resolve();
    });

    expect(screen.getByText('Erro ao salvar endereço')).toBeInTheDocument();
  });

  it('CEP valido (8 digitos) deve disparar busca automatica e preencher os campos', async () => {
    mockBuscarCEP.mockResolvedValueOnce({
      logradouro: 'Rua Encontrada', bairro: 'Bairro X', localidade: 'Cidade Y', uf: 'PB',
    });
    await renderPage();

    fireEvent.change(screen.getByPlaceholderText('CEP'), { target: { name: 'cep', value: '58000000' } });

    // Avanca o debounce (500ms) de forma deterministica e libera a Promise de buscarCEP.
    await act(async () => {
      await vi.advanceTimersByTimeAsync(500);
    });

    expect(screen.getByPlaceholderText('Rua / Logradouro')).toHaveValue('Rua Encontrada');
    expect(screen.getByPlaceholderText('Cidade')).toHaveValue('Cidade Y');
    expect(mockBuscarCEP).toHaveBeenCalledWith('58000000');
  });

  it('CEP nao encontrado deve mostrar erro especifico de CEP', async () => {
    mockBuscarCEP.mockResolvedValueOnce(null);
    await renderPage();

    fireEvent.change(screen.getByPlaceholderText('CEP'), { target: { name: 'cep', value: '99999999' } });

    await act(async () => {
      await vi.advanceTimersByTimeAsync(500);
    });

    expect(screen.getByText('CEP não encontrado')).toBeInTheDocument();
  });
});

describe('Address — com endereços existentes', () => {
  const enderecoSalvo = {
    id: 1, logradouro: 'Rua A', numero: '100', complemento: 'Apto 1',
    bairro: 'Centro', cidade: 'João Pessoa', estado: 'PB', cep: '58000-000',
  };

  it('deve listar os endereços salvos e selecionar o primeiro por padrão', async () => {
    mockApiFetch.mockReset();
    mockApiFetch.mockResolvedValueOnce([enderecoSalvo]);
    await renderPage();

    expect(screen.getByText('Escolha um endereço salvo:')).toBeInTheDocument();
    expect(screen.getByText(/Rua A, 100/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Usar este endereço' })).not.toBeDisabled();
  });

  it('clicar em "Usar este endereço" deve navegar para o checkout', async () => {
    mockApiFetch.mockReset();
    mockApiFetch.mockResolvedValueOnce([enderecoSalvo]);
    await renderPage();

    fireEvent.click(screen.getByRole('button', { name: 'Usar este endereço' }));
    expect(mockNavigate).toHaveBeenCalledWith('/checkout');
  });

  it('selecionar outro endereço da lista deve marcá-lo como ativo', async () => {
    const outroEndereco = { ...enderecoSalvo, id: 2, logradouro: 'Rua B', numero: '200' };
    mockApiFetch.mockReset();
    mockApiFetch.mockResolvedValueOnce([enderecoSalvo, outroEndereco]);
    await renderPage();

    fireEvent.click(screen.getByText(/Rua B, 200/));
    expect(screen.getByRole('button', { name: 'Usar este endereço' })).not.toBeDisabled();
  });

  it('clicar em "Adicionar novo endereço" deve exibir o formulário e o link de voltar', async () => {
    mockApiFetch.mockReset();
    mockApiFetch.mockResolvedValueOnce([enderecoSalvo]);
    await renderPage();

    fireEvent.click(screen.getByText('+ Adicionar novo endereço'));

    expect(screen.getByText('Novo endereço:')).toBeInTheDocument();
    expect(screen.getByText('← Voltar para endereços salvos')).toBeInTheDocument();
  });

  it('clicar em "Voltar para endereços salvos" deve esconder o formulário e reexibir a lista', async () => {
    mockApiFetch.mockReset();
    mockApiFetch.mockResolvedValueOnce([enderecoSalvo]);
    await renderPage();

    fireEvent.click(screen.getByText('+ Adicionar novo endereço'));
    fireEvent.click(screen.getByText('← Voltar para endereços salvos'));

    expect(screen.getByRole('button', { name: 'Usar este endereço' })).toBeInTheDocument();
    expect(screen.queryByText('Novo endereço:')).not.toBeInTheDocument();
  });

  it('falha ao buscar endereços deve cair no formulário de cadastro vazio', async () => {
    mockApiFetch.mockReset();
    mockApiFetch.mockRejectedValueOnce(new Error('falhou'));
    await renderPage();

    expect(screen.getByText('Cadastre seu endereço de entrega:')).toBeInTheDocument();
  });
});
