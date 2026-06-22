import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

let mockParams = { categoria: 'Brownie' };
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return { ...actual, useParams: () => mockParams };
});

const mockAddToCart = vi.fn();
vi.mock('../context/CartContext', () => ({
  useCart: () => ({ addToCart: mockAddToCart }),
}));

const mockGetProdutos = vi.fn();
const mockGetCategorias = vi.fn();
vi.mock('../api/productApi', () => ({
  getProdutos: (...args) => mockGetProdutos(...args),
  getCategorias: (...args) => mockGetCategorias(...args),
}));

import Loja from '../pages/Loja/Loja';

beforeEach(() => {
  mockAddToCart.mockReset();
  mockGetProdutos.mockReset();
  mockGetCategorias.mockReset();
  mockParams = { categoria: 'Brownie' };
});

function renderPage() {
  return render(<MemoryRouter><Loja /></MemoryRouter>);
}

describe('Loja', () => {
  it('deve exibir o estado de carregamento inicialmente', () => {
    mockGetCategorias.mockReturnValue(new Promise(() => {}));
    renderPage();
    expect(screen.getByText('Preparando suas delícias...')).toBeInTheDocument();
  });

  it('quando a categoria existe na API, deve buscar produtos pelo id da categoria', async () => {
    mockGetCategorias.mockResolvedValueOnce([{ id: 5, nome: 'Brownie' }]);
    mockGetProdutos.mockResolvedValueOnce([{ id: 1, name: 'Brownie Tradicional', price: 10, image: 'x.png', category: 'Brownie' }]);
    renderPage();

    expect(await screen.findByText('Brownie Tradicional')).toBeInTheDocument();
    expect(mockGetProdutos).toHaveBeenCalledWith(null, 5, 0, 50);
  });

  it('quando a categoria não existe na API, deve buscar tudo e filtrar localmente', async () => {
    mockParams = { categoria: 'Exotico' };
    mockGetCategorias.mockResolvedValueOnce([{ id: 5, nome: 'Brownie' }]);
    mockGetProdutos.mockResolvedValueOnce([
      { id: 1, name: 'Doce Exótico', price: 10, image: 'x.png', category: 'Exotico' },
      { id: 2, name: 'Brownie', price: 8, image: 'y.png', category: 'Brownie' },
    ]);
    renderPage();

    expect(await screen.findByText('Doce Exótico')).toBeInTheDocument();
    expect(screen.queryByText('Brownie')).not.toBeInTheDocument();
    expect(mockGetProdutos).toHaveBeenCalledWith(null, null, 0, 100);
  });

  it('sem produtos, deve exibir mensagem de lista vazia', async () => {
    mockGetCategorias.mockResolvedValueOnce([{ id: 5, nome: 'Brownie' }]);
    mockGetProdutos.mockResolvedValueOnce([]);
    renderPage();

    expect(await screen.findByText(/Nenhum produto encontrado/)).toBeInTheDocument();
  });

  it('deve exibir o nome da categoria como título', async () => {
    mockGetCategorias.mockResolvedValueOnce([{ id: 5, nome: 'Brownie' }]);
    mockGetProdutos.mockResolvedValueOnce([]);
    renderPage();

    expect(await screen.findByRole('heading', { name: 'Brownie' })).toBeInTheDocument();
  });

  it('em caso de erro na API, deve exibir o ErrorState', async () => {
    mockGetCategorias.mockRejectedValueOnce(new Error('falhou'));
    renderPage();

    expect(await screen.findByText('Erro ao carregar os produtos')).toBeInTheDocument();
  });
});
