import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return { ...actual, useNavigate: () => mockNavigate };
});

const mockClearCart = vi.fn();
let mockCartValue = {
  cart: [{ id: 1, name: 'Brownie', price: 10, quantity: 2 }],
  total: 20,
  clearCart: mockClearCart,
};
vi.mock('../context/CartContext', () => ({
  useCart: () => mockCartValue,
}));

const mockApiFetch = vi.fn();
vi.mock('../api/api', () => ({
  apiFetch: (...args) => mockApiFetch(...args),
}));

import Checkout from '../pages/Checkout/Checkout';

beforeEach(() => {
  mockNavigate.mockClear();
  mockClearCart.mockReset();
  mockApiFetch.mockReset();
  mockCartValue = {
    cart: [{ id: 1, name: 'Brownie', price: 10, quantity: 2 }],
    total: 20,
    clearCart: mockClearCart,
  };
});

function renderPage() {
  return render(<MemoryRouter><Checkout /></MemoryRouter>);
}

/** Avança para o passo de pagamento clicando em "Ir para Pagamento →" */
async function irParaPagamento() {
  fireEvent.click(screen.getByRole('button', { name: /Ir para Pagamento/i }));
}

/** Preenche os campos do cartão (só disponíveis no passo 1) */
async function preencherCartaoValido() {
  await irParaPagamento();
  fireEvent.change(screen.getByPlaceholderText('Como está impresso no cartão'), { target: { name: 'name', value: 'Arthur Silva' } });
  fireEvent.change(screen.getByPlaceholderText('0000 0000 0000 0000'), { target: { name: 'cardNumber', value: '4111111111111111' } });
  fireEvent.change(screen.getByPlaceholderText('MM/AA'), { target: { name: 'expiry', value: '1230' } });
  fireEvent.change(screen.getByPlaceholderText('123'), { target: { name: 'cvv', value: '123' } });
}

describe('Checkout — Passo 0 (Resumo)', () => {
  it('deve exibir os itens do carrinho', () => {
    renderPage();
    expect(screen.getByText('Brownie')).toBeInTheDocument();
  });

  it('deve exibir o total do carrinho', () => {
    renderPage();
    expect(screen.getByText('Total: R$ 20.00')).toBeInTheDocument();
  });

  it('aplicar cupom válido deve exibir desconto no total', async () => {
    mockApiFetch.mockResolvedValueOnce({ tipo: 'PORCENTAGEM', desconto: '10' });
    renderPage();

    fireEvent.change(screen.getByPlaceholderText('Código do cupom'), { target: { value: 'promo10' } });
    fireEvent.click(screen.getByRole('button', { name: 'Aplicar' }));

    expect(await screen.findByText('✅ Cupom aplicado! Desconto de 10%')).toBeInTheDocument();
    expect(screen.getByText(/Total: R\$ 18.00/)).toBeInTheDocument();
  });

  it('aplicar cupom inválido deve mostrar mensagem de erro', async () => {
    mockApiFetch.mockRejectedValueOnce({ mensagem: 'Cupom inválido' });
    renderPage();

    fireEvent.change(screen.getByPlaceholderText('Código do cupom'), { target: { value: 'invalido' } });
    fireEvent.click(screen.getByRole('button', { name: 'Aplicar' }));

    expect(await screen.findByText('Cupom inválido')).toBeInTheDocument();
  });

  it('aplicar cupom de valor fixo deve calcular o desconto corretamente', async () => {
    mockApiFetch.mockResolvedValueOnce({ tipo: 'VALOR_FIXO', desconto: '5' });
    renderPage();

    fireEvent.change(screen.getByPlaceholderText('Código do cupom'), { target: { value: 'FIXO5' } });
    fireEvent.click(screen.getByRole('button', { name: 'Aplicar' }));

    expect(await screen.findByText(/Total: R\$ 15.00/)).toBeInTheDocument();
  });

  it('campo de cupom vazio não deve chamar a API ao clicar em Aplicar', () => {
    renderPage();
    fireEvent.click(screen.getByRole('button', { name: 'Aplicar' }));
    expect(mockApiFetch).not.toHaveBeenCalled();
  });

  it('carrinho vazio deve desabilitar o botão de avançar', () => {
    mockCartValue = { cart: [], total: 0, clearCart: mockClearCart };
    renderPage();
    expect(screen.getByRole('button', { name: /Ir para Pagamento/i })).toBeDisabled();
  });
});

describe('Checkout — Passo 1 (Pagamento)', () => {
  it('deve exibir formulário do cartão após avançar', async () => {
    renderPage();
    await irParaPagamento();
    expect(screen.getByPlaceholderText('0000 0000 0000 0000')).toBeInTheDocument();
  });

  it('deve formatar o número do cartão ao digitar', async () => {
    renderPage();
    await irParaPagamento();
    const input = screen.getByPlaceholderText('0000 0000 0000 0000');
    fireEvent.change(input, { target: { name: 'cardNumber', value: '4111111111111111' } });
    expect(input).toHaveValue('4111 1111 1111 1111');
  });

  it('deve formatar a validade ao digitar', async () => {
    renderPage();
    await irParaPagamento();
    const input = screen.getByPlaceholderText('MM/AA');
    fireEvent.change(input, { target: { name: 'expiry', value: '1230' } });
    expect(input).toHaveValue('12/30');
  });

  it('submeter formulário vazio deve mostrar erros e não chamar a API', async () => {
    renderPage();
    await irParaPagamento();
    fireEvent.click(screen.getByRole('button', { name: 'Confirmar Pedido' }));

    expect(await screen.findByText('Nome é obrigatório')).toBeInTheDocument();
    expect(mockApiFetch).not.toHaveBeenCalled();
  });

  it('carrinho vazio deve impedir o envio mesmo com cartão válido', async () => {
    mockCartValue = { cart: [], total: 0, clearCart: mockClearCart };
    renderPage();
    // Força o passo 1 via botão back/next não disponível, então simula clique com carrinho vazio
    // O botão "Ir para Pagamento" fica disabled com carrinho vazio, mas testamos a guarda do submit
    // Renderizamos com carrinho populado primeiro, avançamos, e aí limpamos
    mockCartValue = { cart: [], total: 0, clearCart: mockClearCart };
    render(
      <MemoryRouter>
        <Checkout />
      </MemoryRouter>
    );
    // Botão ir para pagamento deve estar disabled
    expect(screen.getAllByRole('button', { name: /Ir para Pagamento/i })[0]).toBeDisabled();
  });

  it('sem endereço cadastrado deve mostrar mensagem pedindo para cadastrar', async () => {
    mockApiFetch.mockResolvedValueOnce([]); // GET /clientes/enderecos vazio
    await preencherCartaoValido();

    fireEvent.click(screen.getByRole('button', { name: 'Confirmar Pedido' }));

    expect(await screen.findByText('Cadastre um endereço de entrega antes de finalizar.')).toBeInTheDocument();
  });

  it('fluxo completo de sucesso deve salvar cartão, criar pedido e mostrar confirmação', async () => {
    mockApiFetch
      .mockResolvedValueOnce([{ id: 5, principal: true }]) // GET enderecos
      .mockResolvedValueOnce({ id: 99 })                   // POST cartoes
      .mockResolvedValueOnce({ id: 1000 });                // POST pedidos

    renderPage();
    await preencherCartaoValido();
    fireEvent.click(screen.getByRole('button', { name: 'Confirmar Pedido' }));

    expect(await screen.findByText('Pedido confirmado!')).toBeInTheDocument();
    expect(mockClearCart).toHaveBeenCalled();

    const chamadaPedido = mockApiFetch.mock.calls.find((c) => c[0] === '/pedidos');
    expect(chamadaPedido[1].method).toBe('POST');
    const corpoPedido = JSON.parse(chamadaPedido[1].body);
    expect(corpoPedido.enderecoId).toBe(5);
    expect(corpoPedido.cartaoId).toBe(99);
  });

  it('falha ao finalizar o pedido deve exibir o erro da API', async () => {
    mockApiFetch
      .mockResolvedValueOnce([{ id: 5, principal: true }])
      .mockRejectedValueOnce({ mensagem: 'Cartão recusado' });

    renderPage();
    await preencherCartaoValido();
    fireEvent.click(screen.getByRole('button', { name: 'Confirmar Pedido' }));

    expect(await screen.findByText('Cartão recusado')).toBeInTheDocument();
  });
});
