import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

const mockNavigate = vi.fn();
let mockLocationValue = { pathname: '/home', hash: '' };
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return { ...actual, useNavigate: () => mockNavigate, useLocation: () => mockLocationValue };
});

const mockAddToCart = vi.fn();
vi.mock('../context/CartContext', () => ({
  useCart: () => ({ addToCart: mockAddToCart }),
}));

const mockGetProdutos = vi.fn();
const mockGetMaisVendidos = vi.fn();
const mockGetCategorias = vi.fn();
vi.mock('../api/productApi', () => ({
  getProdutos: (...args) => mockGetProdutos(...args),
  getMaisVendidos: (...args) => mockGetMaisVendidos(...args),
  getCategorias: (...args) => mockGetCategorias(...args),
}));

import Home from '../pages/Home/Home';

beforeEach(() => {
  mockNavigate.mockClear();
  mockAddToCart.mockReset();
  mockGetProdutos.mockReset();
  mockGetMaisVendidos.mockReset();
  mockGetCategorias.mockReset();
  mockLocationValue = { pathname: '/home', hash: '' };
});

function renderHome() {
  return render(<MemoryRouter><Home /></MemoryRouter>);
}

describe('Home', () => {
  it('deve exibir o estado de carregamento inicialmente', () => {
    mockGetProdutos.mockReturnValue(new Promise(() => {}));
    mockGetMaisVendidos.mockReturnValue(new Promise(() => {}));
    mockGetCategorias.mockReturnValue(new Promise(() => {}));
    renderHome();
    expect(screen.getByText('Preparando suas delícias...')).toBeInTheDocument();
  });

  it('com sucesso, deve renderizar os produtos populares', async () => {
    mockGetProdutos.mockResolvedValueOnce([{ id: 1, name: 'Brownie', price: 10, image: 'img.png' }]);
    mockGetMaisVendidos.mockResolvedValueOnce([]);
    mockGetCategorias.mockResolvedValueOnce([]);
    renderHome();

    expect(await screen.findByText('Brownie')).toBeInTheDocument();
  });

  it('com categorias, deve renderizar até 3 categorias e um link "Mais..."', async () => {
    mockGetProdutos.mockResolvedValueOnce([]);
    mockGetMaisVendidos.mockResolvedValueOnce([]);
    mockGetCategorias.mockResolvedValueOnce([
      { id: 1, nome: 'Brownie' }, { id: 2, nome: 'Cookie' }, { id: 3, nome: 'Trufa' }, { id: 4, nome: 'Bolo' },
    ]);
    renderHome();

    expect(await screen.findByText('Brownie')).toBeInTheDocument();
    expect(screen.getByText('Cookie')).toBeInTheDocument();
    expect(screen.getByText('Trufa')).toBeInTheDocument();
    expect(screen.queryByText('Bolo')).not.toBeInTheDocument();
    expect(screen.getByText('Mais...')).toBeInTheDocument();
  });

  it('com mais vendidos, deve renderizar a seção correspondente', async () => {
    mockGetProdutos.mockResolvedValueOnce([]);
    mockGetMaisVendidos.mockResolvedValueOnce([{ id: 9, name: 'Cookie Premium', price: 8, image: 'c.png' }]);
    mockGetCategorias.mockResolvedValueOnce([]);
    renderHome();

    expect(await screen.findByText('Cookie Premium')).toBeInTheDocument();
    expect(screen.getByText('Mais Vendidos da Semana')).toBeInTheDocument();
  });

  it('em caso de erro na API, deve exibir o ErrorState', async () => {
    mockGetProdutos.mockRejectedValueOnce(new Error('falhou'));
    mockGetMaisVendidos.mockResolvedValueOnce([]);
    mockGetCategorias.mockResolvedValueOnce([]);
    renderHome();

    expect(await screen.findByText('Erro ao carregar os produtos')).toBeInTheDocument();
  });

  it('sem hash #destaques, não deve tentar rolar a tela', async () => {
    mockGetProdutos.mockResolvedValueOnce([]);
    mockGetMaisVendidos.mockResolvedValueOnce([]);
    mockGetCategorias.mockResolvedValueOnce([]);
    renderHome();

    await waitFor(() => expect(screen.queryByText('Preparando suas delícias...')).not.toBeInTheDocument());
    expect(screen.getByText('Sweet Delights')).toBeInTheDocument();
  });
});
