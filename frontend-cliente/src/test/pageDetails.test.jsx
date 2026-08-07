import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

let mockParams = { id: '1' };
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return { ...actual, useParams: () => mockParams };
});

const mockAddToCart = vi.fn();
vi.mock('../context/CartContext', () => ({
  useCart: () => ({ addToCart: mockAddToCart }),
}));

const mockGetProdutoById = vi.fn();
vi.mock('../api/productApi', () => ({
  getProdutoById: (...args) => mockGetProdutoById(...args),
}));

import Details from '../pages/DetailsProduct/Details';

beforeEach(() => {
  mockAddToCart.mockReset();
  mockGetProdutoById.mockReset();
  mockParams = { id: '1' };
});

function renderPage() {
  return render(<MemoryRouter><Details /></MemoryRouter>);
}

const produtoCompleto = {
  id: 1, name: 'Brownie', description: 'Delicioso', price: 12.5,
  rating: 4.5, stock: 3, category: 'Doces', image: 'brownie.png',
};

describe('Details', () => {
  it('deve exibir o estado de carregamento inicialmente', () => {
    mockGetProdutoById.mockReturnValue(new Promise(() => {}));
    renderPage();
    expect(screen.getByText('Preparando suas delícias...')).toBeInTheDocument();
  });

  it('com sucesso, deve renderizar os dados do produto', async () => {
    mockGetProdutoById.mockResolvedValueOnce(produtoCompleto);
    renderPage();

    expect(await screen.findByRole('heading', { name: 'Brownie' })).toBeInTheDocument();
    expect(screen.getByText('Delicioso')).toBeInTheDocument();
    expect(screen.getByText('R$ 12.50')).toBeInTheDocument();
    expect(mockGetProdutoById).toHaveBeenCalledWith('1');
  });

  it('produto sem avaliação deve exibir "Sem avaliações"', async () => {
    mockGetProdutoById.mockResolvedValueOnce({ ...produtoCompleto, rating: 0 });
    renderPage();
    expect(await screen.findByText(/Sem avaliações/)).toBeInTheDocument();
  });

  it('produto sem imagem deve exibir o fallback de emoji', async () => {
    mockGetProdutoById.mockResolvedValueOnce({ ...produtoCompleto, image: null });
    renderPage();
    expect(await screen.findByText('🧁')).toBeInTheDocument();
  });

  it('produto com estoque deve permitir adicionar ao carrinho', async () => {
    mockGetProdutoById.mockResolvedValueOnce(produtoCompleto);
    renderPage();

    const botao = await screen.findByRole('button', { name: 'Adicionar ao carrinho' });
    expect(botao).not.toBeDisabled();
    fireEvent.click(botao);
    expect(mockAddToCart).toHaveBeenCalledWith(produtoCompleto);
  });

  it('produto sem estoque deve desabilitar o botão', async () => {
    mockGetProdutoById.mockResolvedValueOnce({ ...produtoCompleto, stock: 0 });
    renderPage();
    expect(await screen.findByRole('button', { name: 'Fora de estoque' })).toBeDisabled();
  });

  it('em caso de erro na API, deve exibir o ErrorState', async () => {
    mockGetProdutoById.mockRejectedValueOnce(new Error('falhou'));
    renderPage();
    expect(await screen.findByText('Erro ao carregar o produto')).toBeInTheDocument();
  });
});
