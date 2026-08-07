import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

const mockAddToCart = vi.fn();
vi.mock('../context/CartContext', () => ({
  useCart: () => ({ addToCart: mockAddToCart }),
}));

const mockGetPromocoes = vi.fn();
vi.mock('../api/productApi', () => ({
  getPromocoes: (...args) => mockGetPromocoes(...args),
}));

import Promotion from '../pages/Promotion/Promotion';

beforeEach(() => {
  mockAddToCart.mockReset();
  mockGetPromocoes.mockReset();
});

function renderPage() {
  return render(<MemoryRouter><Promotion /></MemoryRouter>);
}

describe('Promotion', () => {
  it('deve exibir o estado de carregamento inicialmente', () => {
    mockGetPromocoes.mockReturnValue(new Promise(() => {}));
    renderPage();
    expect(screen.getByText('Preparando suas delícias...')).toBeInTheDocument();
  });

  it('com produtos, deve renderizá-los', async () => {
    mockGetPromocoes.mockResolvedValueOnce([{ id: 1, name: 'Brownie Promo', price: 7, image: 'b.png' }]);
    renderPage();
    expect(await screen.findByText('Brownie Promo')).toBeInTheDocument();
  });

  it('sem produtos, deve exibir mensagem de "fique atento"', async () => {
    mockGetPromocoes.mockResolvedValueOnce([]);
    renderPage();
    expect(await screen.findByText(/Fique atento/)).toBeInTheDocument();
  });

  it('em caso de erro na API, deve exibir o ErrorState', async () => {
    mockGetPromocoes.mockRejectedValueOnce(new Error('falhou'));
    renderPage();
    expect(await screen.findByText('Erro ao carregar promoções')).toBeInTheDocument();
  });
});
