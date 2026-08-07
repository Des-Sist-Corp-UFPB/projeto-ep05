import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return { ...actual, useNavigate: () => mockNavigate };
});

const mockRemoveFromCart = vi.fn();
const mockUpdateQuantity = vi.fn();
const mockClearCart = vi.fn();
let mockCartValue = { cart: [], removeFromCart: mockRemoveFromCart, updateQuantity: mockUpdateQuantity, total: 0, clearCart: mockClearCart };

vi.mock('../context/CartContext', () => ({
  useCart: () => mockCartValue,
}));

import CartProduct from '../pages/CartProduct/CartProduct';

beforeEach(() => {
  mockNavigate.mockClear();
  mockRemoveFromCart.mockReset();
  mockUpdateQuantity.mockReset();
  mockClearCart.mockReset();
  mockCartValue = { cart: [], removeFromCart: mockRemoveFromCart, updateQuantity: mockUpdateQuantity, total: 0, clearCart: mockClearCart };
});

function renderPage() {
  return render(<MemoryRouter><CartProduct /></MemoryRouter>);
}

describe('CartProduct', () => {
  it('carrinho vazio deve exibir mensagem correspondente', () => {
    renderPage();
    expect(screen.getByText('Carrinho está vazio 🧁')).toBeInTheDocument();
  });

  it('com itens, deve renderizar nome, preço e total', () => {
    mockCartValue = {
      ...mockCartValue,
      cart: [{ id: 1, name: 'Brownie', price: 10, quantity: 2, image: 'b.png' }],
      total: 20,
    };
    renderPage();

    expect(screen.getByText('Brownie')).toBeInTheDocument();
    expect(screen.getByText('Total: R$ 20.00')).toBeInTheDocument();
  });

  it('clicar em Remover deve chamar removeFromCart com o id correto', () => {
    mockCartValue = {
      ...mockCartValue,
      cart: [{ id: 7, name: 'Cookie', price: 5, quantity: 1, image: 'c.png' }],
      total: 5,
    };
    renderPage();

    fireEvent.click(screen.getByText('Remover'));
    expect(mockRemoveFromCart).toHaveBeenCalledWith(7);
  });

  it('clicar em Limpar Carrinho deve chamar clearCart', () => {
    mockCartValue = {
      ...mockCartValue,
      cart: [{ id: 1, name: 'Brownie', price: 10, quantity: 1, image: 'b.png' }],
      total: 10,
    };
    renderPage();

    fireEvent.click(screen.getByText('Limpar Carrinho'));
    expect(mockClearCart).toHaveBeenCalled();
  });

  it('clicar em Finalizar Pedido deve navegar para /address', () => {
    mockCartValue = {
      ...mockCartValue,
      cart: [{ id: 1, name: 'Brownie', price: 10, quantity: 1, image: 'b.png' }],
      total: 10,
    };
    renderPage();

    fireEvent.click(screen.getByText('Finalizar Pedido'));
    expect(mockNavigate).toHaveBeenCalledWith('/address');
  });

  it('alterar a quantidade deve chamar updateQuantity com o novo valor', () => {
    mockCartValue = {
      ...mockCartValue,
      cart: [{ id: 1, name: 'Brownie', price: 10, quantity: 2, image: 'b.png' }],
      total: 20,
    };
    renderPage();

    fireEvent.click(screen.getAllByRole('button')[2]); // botão "+" do QuantityControl
    expect(mockUpdateQuantity).toHaveBeenCalledWith(1, 3);
  });
});
