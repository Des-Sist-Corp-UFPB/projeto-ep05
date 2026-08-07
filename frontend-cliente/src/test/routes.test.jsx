import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';

// AppRoutes monta seu próprio <BrowserRouter>, então não há como injetar
// um MemoryRouter por fora. Em vez disso, navegamos manipulando o
// histórico real do jsdom (window.history) antes de renderizar.

vi.mock('../api/productApi', () => ({
  getProdutos: vi.fn().mockResolvedValue([]),
  getMaisVendidos: vi.fn().mockResolvedValue([]),
  getCategorias: vi.fn().mockResolvedValue([]),
  getProdutoById: vi.fn().mockResolvedValue({
    id: 1, name: 'Brownie', description: 'Delicioso', price: 10, stock: 5, image: null, rating: 0,
  }),
  getPromocoes: vi.fn().mockResolvedValue([]),
}));

vi.mock('../api/api', () => ({
  apiFetch: vi.fn().mockResolvedValue([]),
  BASE_URL: '/api',
}));

import AppRoutes from '../Routes/Routes';

function irPara(path) {
  window.history.pushState({}, '', path);
}

beforeEach(() => {
  localStorage.clear();
  irPara('/');
});

describe('AppRoutes', () => {
  it('renderiza a Home na rota raiz', async () => {
    render(<AppRoutes />);
    expect(await screen.findByText('Sweet Delights')).toBeInTheDocument();
  });

  it('renderiza a página de Login em /login', async () => {
    irPara('/login');
    render(<AppRoutes />);
    expect(await screen.findByRole('button', { name: 'Entrar' })).toBeInTheDocument();
  });

  it('renderiza a página de Cadastro em /cadastro', async () => {
    irPara('/cadastro');
    render(<AppRoutes />);
    expect(await screen.findByRole('button', { name: 'Cadastrar' })).toBeInTheDocument();
  });

  it('renderiza Categorias dentro do layout em /categorias', async () => {
    irPara('/categorias');
    render(<AppRoutes />);
    expect(await screen.findByRole('heading', { name: 'Categorias' })).toBeInTheDocument();
  });

  it('rota privada (/perfil) sem usuário autenticado redireciona para /login', async () => {
    irPara('/perfil');
    render(<AppRoutes />);
    expect(await screen.findByRole('button', { name: 'Entrar' })).toBeInTheDocument();
  });

  it('rota privada (/checkout) sem usuário autenticado redireciona para /login', async () => {
    irPara('/checkout');
    render(<AppRoutes />);
    expect(await screen.findByRole('button', { name: 'Entrar' })).toBeInTheDocument();
  });

  it('com usuário autenticado no localStorage, /perfil é acessível', async () => {
    localStorage.setItem('user', JSON.stringify({ token: 't', email: 'a@a.com', nome: 'Ana', papel: 'CLIENTE' }));
    irPara('/perfil');
    render(<AppRoutes />);
    expect(await screen.findByText('Olá, Ana!')).toBeInTheDocument();
  });
});
