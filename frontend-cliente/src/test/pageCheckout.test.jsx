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

function preencherCartaoValido() {
  fireEvent.change(screen.getByPlaceholderText('Como está no cartão'), { target: { name: 'name', value: 'Arthur Silva' } });
  fireEvent.change(screen.getByPlaceholderText('0000 0000 0000 0000'), { target: { name: 'cardNumber', value: '4111111111111111' } });
  fireEvent.change(screen.getByPlaceholderText('MM/AA'), { target: { name: 'expiry', value: '1230' } });
  fireEvent.change(screen.getByPlaceholderText('123'), { target: { name: 'cvv', value: '123' } });
}

describe('Checkout', () => {
  it('deve exibir o total do carrinho', () => {
    renderPage();
    expect(screen.getByText('Total: R$ 20.00')).toBeInTheDocument();
  });

  it('deve formatar o número do cartão ao digitar', () => {
    renderPage();
    const input = screen.getByPlaceholderText('0000 0000 0000 0000');
    fireEvent.change(input, { target: { name: 'cardNumber', value: '4111111111111111' } });
    expect(input).toHaveValue('4111 1111 1111 1111');
  });

  it('deve formatar a validade ao digitar', () => {
    renderPage();
    const input = screen.getByPlaceholderText('MM/AA');
    fireEvent.change(input, { target: { name: 'expiry', value: '1230' } });
    expect(input).toHaveValue('12/30');
  });

  it('submeter formulário vazio deve mostrar erros e não chamar a API', async () => {
    renderPage();
    fireEvent.click(screen.getByRole('button', { name: 'Confirmar Pedido' }));

    expect(await screen.findByText('Nome é obrigatório')).toBeInTheDocument();
    expect(mockApiFetch).not.toHaveBeenCalled();
  });

  it('carrinho vazio deve impedir o envio mesmo com cartão válido', async () => {
    mockCartValue = { cart: [], total: 0, clearCart: mockClearCart };
    renderPage();
    preencherCartaoValido();

    fireEvent.click(screen.getByRole('button', { name: 'Confirmar Pedido' }));

    expect(await screen.findByText('Seu carrinho está vazio.')).toBeInTheDocument();
  });

  it('sem endereço cadastrado deve mostrar mensagem pedindo para cadastrar', async () => {
    mockApiFetch.mockResolvedValueOnce([]); // GET /clientes/enderecos vazio
    renderPage();
    preencherCartaoValido();

    fireEvent.click(screen.getByRole('button', { name: 'Confirmar Pedido' }));

    expect(await screen.findByText('Cadastre um endereço de entrega antes de finalizar o pedido.')).toBeInTheDocument();
  });

  it('fluxo completo de sucesso deve salvar cartão, criar pedido e mostrar confirmação', async () => {
    mockApiFetch
      .mockResolvedValueOnce([{ id: 5, principal: true }]) // GET enderecos
      .mockResolvedValueOnce({ id: 99 }) // POST cartoes
      .mockResolvedValueOnce({ id: 1000 }); // POST pedidos

    renderPage();
    preencherCartaoValido();
    fireEvent.click(screen.getByRole('button', { name: 'Confirmar Pedido' }));

    expect(await screen.findByText('✅ Pedido realizado!')).toBeInTheDocument();
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
    preencherCartaoValido();
    fireEvent.click(screen.getByRole('button', { name: 'Confirmar Pedido' }));

    expect(await screen.findByText('Cartão recusado')).toBeInTheDocument();
  });

  it('aplicar cupom válido deve exibir desconto no total', async () => {
    mockApiFetch.mockResolvedValueOnce({ tipo: 'PORCENTAGEM', desconto: '10' });
    renderPage();

    fireEvent.change(screen.getByPlaceholderText('Código do cupom'), { target: { value: 'promo10' } });
    fireEvent.click(screen.getByRole('button', { name: 'Aplicar' }));

    expect(await screen.findByText('✅ Cupom aplicado!')).toBeInTheDocument();
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
});
