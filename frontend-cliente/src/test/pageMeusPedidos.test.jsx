import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, act } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return { ...actual, useNavigate: () => mockNavigate };
});

const mockApiFetch = vi.fn();
vi.mock('../api/api', () => ({
  apiFetch: (...args) => mockApiFetch(...args),
}));

import MeusPedidos from '../pages/MeusPedidos/MeusPedidos';

beforeEach(() => {
  mockNavigate.mockClear();
  mockApiFetch.mockReset();
});

function renderPage() {
  return render(<MemoryRouter><MeusPedidos /></MemoryRouter>);
}

async function flush() {
  await act(async () => {
    await Promise.resolve();
  });
}

const pedidoBase = {
  id: 1,
  status: 'AGUARDANDO_PAGAMENTO',
  criadoEm: '2026-06-20T10:00:00',
  totalGeral: 59.9,
  itens: [
    { nomeProduto: 'Brownie', quantidade: 2, subtotal: 20 },
    { produto: { nome: 'Bolo' }, quantidade: 1, precoUnitario: 39.9 },
  ],
  enderecoEntrega: {
    logradouro: 'Rua A', numero: '100', complemento: 'Apto 1',
    bairro: 'Centro', cidade: 'João Pessoa', estado: 'PB', cep: '58000-000',
  },
  codigoRastreamento: 'BR123456789',
};

describe('MeusPedidos — carregamento e estados', () => {
  it('deve exibir o estado de carregamento inicialmente', () => {
    mockApiFetch.mockReturnValueOnce(new Promise(() => {}));
    renderPage();
    expect(screen.getByText('Carregando pedidos...')).toBeInTheDocument();
  });

  it('deve exibir mensagem de erro quando a API falhar', async () => {
    mockApiFetch.mockRejectedValueOnce(new Error('falhou'));
    renderPage();
    await flush();
    expect(screen.getByText('Não foi possível carregar seus pedidos.')).toBeInTheDocument();
  });

  it('deve exibir estado vazio quando não há pedidos', async () => {
    mockApiFetch.mockResolvedValueOnce({ content: [], totalPages: 1 });
    renderPage();
    await flush();
    expect(screen.getByText('🧁 Você ainda não fez nenhum pedido.')).toBeInTheDocument();
  });

  it('clicar em "Explorar produtos" no estado vazio deve navegar para "/"', async () => {
    mockApiFetch.mockResolvedValueOnce({ content: [], totalPages: 1 });
    renderPage();
    await flush();
    fireEvent.click(screen.getByText('Explorar produtos'));
    expect(mockNavigate).toHaveBeenCalledWith('/');
  });

  it('deve listar os pedidos retornados pela API', async () => {
    mockApiFetch.mockResolvedValueOnce({ content: [pedidoBase], totalPages: 1 });
    renderPage();
    await flush();
    expect(screen.getByText('Pedido #1')).toBeInTheDocument();
    expect(screen.getByText('R$ 59.90')).toBeInTheDocument();
  });
});

describe('MeusPedidos — paginação', () => {
  it('deve exibir os controles de paginação quando há mais de uma página', async () => {
    mockApiFetch.mockResolvedValueOnce({ content: [pedidoBase], totalPages: 2 });
    renderPage();
    await flush();
    expect(screen.getByText('1 / 2')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '← Anterior' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Próxima →' })).not.toBeDisabled();
  });

  it('clicar em "Próxima" deve carregar a próxima página', async () => {
    mockApiFetch.mockResolvedValueOnce({ content: [pedidoBase], totalPages: 2 });
    renderPage();
    await flush();

    mockApiFetch.mockResolvedValueOnce({ content: [{ ...pedidoBase, id: 2 }], totalPages: 2 });
    fireEvent.click(screen.getByRole('button', { name: 'Próxima →' }));
    await flush();

    expect(mockApiFetch).toHaveBeenLastCalledWith('/pedidos?page=1&size=5&sort=criadoEm,desc');
    expect(screen.getByText('2 / 2')).toBeInTheDocument();
  });
});

describe('MeusPedidos — card de pedido', () => {
  it('clicar no cabeçalho do card deve abrir e exibir detalhes', async () => {
    mockApiFetch.mockResolvedValueOnce({ content: [pedidoBase], totalPages: 1 });
    renderPage();
    await flush();

    fireEvent.click(screen.getByText('Pedido #1'));

    expect(screen.getByText('Itens')).toBeInTheDocument();
    expect(screen.getByText(/Brownie x2/)).toBeInTheDocument();
    expect(screen.getByText(/Bolo x1/)).toBeInTheDocument();
    expect(screen.getByText('Endereço de entrega')).toBeInTheDocument();
    expect(screen.getByText(/Rua A, 100 — Apto 1/)).toBeInTheDocument();
    expect(screen.getByText(/BR123456789/)).toBeInTheDocument();
  });

  it('clicar novamente no cabeçalho deve fechar o card', async () => {
    mockApiFetch.mockResolvedValueOnce({ content: [pedidoBase], totalPages: 1 });
    renderPage();
    await flush();

    fireEvent.click(screen.getByText('Pedido #1'));
    expect(screen.getByText('Itens')).toBeInTheDocument();

    fireEvent.click(screen.getByText('Pedido #1'));
    expect(screen.queryByText('Itens')).not.toBeInTheDocument();
  });

  it('pedido cancelado deve exibir a mensagem de timeline cancelada e motivo', async () => {
    const pedidoCancelado = {
      ...pedidoBase,
      id: 3,
      status: 'CANCELADO',
      motivoCancelamento: 'Comprei por engano',
    };
    mockApiFetch.mockResolvedValueOnce({ content: [pedidoCancelado], totalPages: 1 });
    renderPage();
    await flush();

    fireEvent.click(screen.getByText('Pedido #3'));

    expect(screen.getByText('❌ Pedido cancelado')).toBeInTheDocument();
    expect(screen.getByText('Motivo do cancelamento:')).toBeInTheDocument();
    expect(screen.getByText('Comprei por engano')).toBeInTheDocument();
  });

  it('pedido entregue deve exibir a timeline com etapas concluídas', async () => {
    const pedidoEntregue = { ...pedidoBase, id: 4, status: 'ENTREGUE' };
    mockApiFetch.mockResolvedValueOnce({ content: [pedidoEntregue], totalPages: 1 });
    renderPage();
    await flush();

    fireEvent.click(screen.getByText('Pedido #4'));
    expect(screen.getAllByText('Entregue').length).toBeGreaterThan(0);
  });

  it('pedido sem itens nem endereço não deve quebrar a renderização', async () => {
    const pedidoMinimo = {
      id: 5, status: 'PAGO', criadoEm: null, totalGeral: 10, itens: [], enderecoEntrega: null,
    };
    mockApiFetch.mockResolvedValueOnce({ content: [pedidoMinimo], totalPages: 1 });
    renderPage();
    await flush();

    fireEvent.click(screen.getByText('Pedido #5'));
    expect(screen.getByText('Itens')).toBeInTheDocument();
    expect(screen.queryByText('Endereço de entrega')).not.toBeInTheDocument();
  });
});

describe('MeusPedidos — cancelamento de pedido', () => {
  async function abrirECancelar() {
    mockApiFetch.mockResolvedValueOnce({ content: [pedidoBase], totalPages: 1 });
    renderPage();
    await flush();
    fireEvent.click(screen.getByText('Pedido #1'));
    fireEvent.click(screen.getByRole('button', { name: 'Cancelar pedido' }));
  }

  it('clicar em "Cancelar pedido" deve exibir o formulário de confirmação', async () => {
    await abrirECancelar();
    expect(screen.getByText('⚠️ Informe o motivo do cancelamento:')).toBeInTheDocument();
  });

  it('clicar em "Não, manter" deve esconder o formulário de confirmação', async () => {
    await abrirECancelar();
    fireEvent.click(screen.getByRole('button', { name: 'Não, manter' }));
    expect(screen.queryByText('⚠️ Informe o motivo do cancelamento:')).not.toBeInTheDocument();
  });

  it('botão "Sim, cancelar" deve permanecer desabilitado enquanto o motivo estiver vazio', async () => {
    await abrirECancelar();
    expect(screen.getByRole('button', { name: 'Sim, cancelar' })).toBeDisabled();
  });

  it('confirmar com motivo deve chamar a API e atualizar o status localmente', async () => {
    await abrirECancelar();
    fireEvent.change(screen.getByPlaceholderText(/Comprei por engano/), {
      target: { value: 'Mudei de ideia' },
    });

    mockApiFetch.mockResolvedValueOnce({});
    fireEvent.click(screen.getByRole('button', { name: 'Sim, cancelar' }));
    await flush();

    expect(mockApiFetch).toHaveBeenLastCalledWith('/pedidos/1/cancelar', {
      method: 'POST',
      body: JSON.stringify({ motivo: 'Mudei de ideia' }),
    });
    expect(screen.getByText('❌ Pedido cancelado')).toBeInTheDocument();
  });

  it('falha ao cancelar deve exibir a mensagem de erro da API', async () => {
    await abrirECancelar();
    fireEvent.change(screen.getByPlaceholderText(/Comprei por engano/), {
      target: { value: 'Mudei de ideia' },
    });

    mockApiFetch.mockRejectedValueOnce({ mensagem: 'Pedido já enviado, não pode ser cancelado' });
    fireEvent.click(screen.getByRole('button', { name: 'Sim, cancelar' }));
    await flush();

    expect(screen.getByText('Pedido já enviado, não pode ser cancelado')).toBeInTheDocument();
  });

  it('falha ao cancelar sem mensagem deve usar mensagem padrão', async () => {
    await abrirECancelar();
    fireEvent.change(screen.getByPlaceholderText(/Comprei por engano/), {
      target: { value: 'Mudei de ideia' },
    });

    mockApiFetch.mockRejectedValueOnce({});
    fireEvent.click(screen.getByRole('button', { name: 'Sim, cancelar' }));
    await flush();

    expect(screen.getByText('Não foi possível cancelar o pedido.')).toBeInTheDocument();
  });

  it('pedido ENVIADO não deve exibir o botão de cancelar', async () => {
    const pedidoEnviado = { ...pedidoBase, id: 6, status: 'ENVIADO' };
    mockApiFetch.mockResolvedValueOnce({ content: [pedidoEnviado], totalPages: 1 });
    renderPage();
    await flush();
    fireEvent.click(screen.getByText('Pedido #6'));
    expect(screen.queryByRole('button', { name: 'Cancelar pedido' })).not.toBeInTheDocument();
  });
});
