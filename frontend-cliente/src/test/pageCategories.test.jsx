import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return { ...actual, useNavigate: () => mockNavigate };
});

const mockGetCategorias = vi.fn();
vi.mock('../api/productApi', () => ({
  getCategorias: (...args) => mockGetCategorias(...args),
}));

import Categories from '../pages/Categories/Categories';

beforeEach(() => {
  mockNavigate.mockClear();
  mockGetCategorias.mockReset();
});

function renderPage() {
  return render(<MemoryRouter><Categories /></MemoryRouter>);
}

describe('Categories', () => {
  it('deve exibir o estado de carregamento inicialmente', () => {
    mockGetCategorias.mockReturnValue(new Promise(() => {}));
    renderPage();
    expect(screen.getByText('Preparando suas delícias...')).toBeInTheDocument();
  });

  it('com sucesso, deve renderizar as categorias retornadas', async () => {
    mockGetCategorias.mockResolvedValueOnce([
      { id: 1, nome: 'Brownie', imagem: null },
      { id: 2, nome: 'Cookie', imagem: 'cookie.png' },
    ]);
    renderPage();

    expect(await screen.findByText('Brownie')).toBeInTheDocument();
    expect(screen.getByText('Cookie')).toBeInTheDocument();
  });

  it('categoria sem imagem deve usar emoji de fallback conhecido', async () => {
    mockGetCategorias.mockResolvedValueOnce([{ id: 1, nome: 'Brownie de chocolate', imagem: null }]);
    renderPage();

    await screen.findByText('Brownie de chocolate');
    expect(screen.getByText('🍫')).toBeInTheDocument();
  });

  it('categoria com nome desconhecido deve usar emoji padrão', async () => {
    mockGetCategorias.mockResolvedValueOnce([{ id: 1, nome: 'Salgadinho', imagem: null }]);
    renderPage();

    await screen.findByText('Salgadinho');
    expect(screen.getByText('🧁')).toBeInTheDocument();
  });

  it('categoria com imagem deve renderizar a tag <img>', async () => {
    mockGetCategorias.mockResolvedValueOnce([{ id: 1, nome: 'Cookie', imagem: 'cookie.png' }]);
    renderPage();

    const img = await screen.findByAltText('Cookie');
    expect(img).toHaveAttribute('src', 'cookie.png');
  });

  it('clicar em uma categoria deve navegar para /loja/:categoria', async () => {
    mockGetCategorias.mockResolvedValueOnce([{ id: 1, nome: 'Brownie', imagem: null }]);
    renderPage();

    fireEvent.click(await screen.findByText('Brownie'));
    expect(mockNavigate).toHaveBeenCalledWith('/loja/Brownie');
  });

  it('em caso de erro na API, deve exibir o ErrorState', async () => {
    mockGetCategorias.mockRejectedValueOnce(new Error('falhou'));
    renderPage();

    expect(await screen.findByText('Erro ao carregar os doces')).toBeInTheDocument();
  });
});
