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
});

afterEach(() => {
  vi.useRealTimers();
});

function renderPage() {
  return render(<MemoryRouter><Address /></MemoryRouter>);
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
  it('deve formatar o CEP ao digitar', () => {
    renderPage();
    const input = screen.getByPlaceholderText('CEP');
    fireEvent.change(input, { target: { name: 'cep', value: '58000000' } });
    expect(input).toHaveValue('58000-000');
  });

  it('submeter formulario vazio deve mostrar erros e nao chamar a API', async () => {
    renderPage();
    fireEvent.click(screen.getByRole('button', { name: 'Salvar endereço' }));

    expect(screen.getByText('CEP obrigatório')).toBeInTheDocument();
    expect(mockApiFetch).not.toHaveBeenCalled();
  });

  it('salvar com sucesso deve exibir a tela de confirmacao', async () => {
    mockApiFetch.mockResolvedValueOnce({ id: 1 });
    renderPage();
    preencherFormularioValido();

    fireEvent.click(screen.getByRole('button', { name: 'Salvar endereço' }));
    // Resolve a Promise do apiFetch (microtask) sem avancar o debounce do CEP.
    await act(async () => {
      await Promise.resolve();
    });

    expect(screen.getByText('✅ Endereço salvo!')).toBeInTheDocument();
  });

  it('falha ao salvar deve exibir a mensagem de erro da API', async () => {
    mockApiFetch.mockRejectedValueOnce({ mensagem: 'CEP inválido para entrega' });
    renderPage();
    preencherFormularioValido();

    fireEvent.click(screen.getByRole('button', { name: 'Salvar endereço' }));
    await act(async () => {
      await Promise.resolve();
    });

    expect(screen.getByText('CEP inválido para entrega')).toBeInTheDocument();
  });

  it('falha ao salvar sem mensagem deve usar mensagem padrao', async () => {
    mockApiFetch.mockRejectedValueOnce({});
    renderPage();
    preencherFormularioValido();

    fireEvent.click(screen.getByRole('button', { name: 'Salvar endereço' }));
    await act(async () => {
      await Promise.resolve();
    });

    expect(screen.getByText('Erro ao salvar endereço')).toBeInTheDocument();
  });

  it('CEP valido (8 digitos) deve disparar busca automatica e preencher os campos', async () => {
    mockBuscarCEP.mockResolvedValueOnce({
      logradouro: 'Rua Encontrada', bairro: 'Bairro X', localidade: 'Cidade Y', uf: 'PB',
    });
    renderPage();

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
    renderPage();

    fireEvent.change(screen.getByPlaceholderText('CEP'), { target: { name: 'cep', value: '99999999' } });

    await act(async () => {
      await vi.advanceTimersByTimeAsync(500);
    });

    expect(screen.getByText('CEP não encontrado')).toBeInTheDocument();
  });
});
