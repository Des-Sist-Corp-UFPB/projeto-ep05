import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

// ── Mocks de react-router-dom ──────────────────────────────────────────────
const mockNavigate = vi.fn();
let mockLocationValue = { pathname: '/', state: null };

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
    useLocation: () => mockLocationValue,
  };
});

// ── Mocks de contexto ───────────────────────────────────────────────────────
const mockUseAuth = vi.fn();
vi.mock('../context/AuthContext', () => ({
  useAuth: (...args) => mockUseAuth(...args),
}));

const mockUseCart = vi.fn();
vi.mock('../context/CartContext', () => ({
  useCart: (...args) => mockUseCart(...args),
}));

import BackButton from '../components/BackButton/BackButton';
import ErrorState from '../components/ErrorState/ErrorState';
import LayoutPage from '../components/LayoutPage/LayoutPage';
import LayoutStripes from '../components/LayoutStripes/LayoutStripes';
import Loading from '../components/Loading/Loading';
import MainScrollContainer from '../components/MainScrollContainer/MainScrollContainer';
import PrivateRoute from '../components/PrivateRoute/PrivateRoute';
import ProductCard from '../components/ProductCard/ProductCard';
import QuantityControl from '../components/QuantityControl/QuantityControl';
import ScrollContainer from '../components/ScrollContainer/ScrollContainer';

beforeEach(() => {
  mockNavigate.mockClear();
  mockLocationValue = { pathname: '/', state: null };
  mockUseAuth.mockReset();
  mockUseCart.mockReset();
  mockUseCart.mockReturnValue({ totalItems: 0 });
  mockUseAuth.mockReturnValue({ user: null, logout: vi.fn(), isAuthenticated: false });
});

// ── BackButton ────────────────────────────────────────────────────────────────

describe('BackButton', () => {
  it('deve renderizar o texto "Voltar"', () => {
    render(<BackButton />);
    expect(screen.getByText('Voltar')).toBeInTheDocument();
  });

  it('em rota normal, deve chamar navigate(-1)', () => {
    mockLocationValue = { pathname: '/details/1', state: null };
    render(<BackButton />);
    fireEvent.click(screen.getByRole('button'));
    expect(mockNavigate).toHaveBeenCalledWith(-1);
  });

  it('em /login vindo de outra rota (state.from), deve navegar para /home', () => {
    mockLocationValue = { pathname: '/login', state: { from: { pathname: '/perfil' } } };
    render(<BackButton />);
    fireEvent.click(screen.getByRole('button'));
    expect(mockNavigate).toHaveBeenCalledWith('/home');
  });

  it('em /login sem state.from, deve chamar navigate(-1)', () => {
    mockLocationValue = { pathname: '/login', state: null };
    render(<BackButton />);
    fireEvent.click(screen.getByRole('button'));
    expect(mockNavigate).toHaveBeenCalledWith(-1);
  });

  it('em /cadastro com state.from, deve navegar para /home', () => {
    mockLocationValue = { pathname: '/cadastro', state: { from: { pathname: '/perfil' } } };
    render(<BackButton />);
    fireEvent.click(screen.getByRole('button'));
    expect(mockNavigate).toHaveBeenCalledWith('/home');
  });
});

// ── ErrorState ────────────────────────────────────────────────────────────────

describe('ErrorState', () => {
  it('deve renderizar mensagem padrão quando não fornecida', () => {
    render(<ErrorState />);
    expect(screen.getByText('Ops! Algo deu errado.')).toBeInTheDocument();
  });

  it('deve renderizar mensagem customizada', () => {
    render(<ErrorState mensagem="Falha ao carregar" />);
    expect(screen.getByText('Falha ao carregar')).toBeInTheDocument();
  });

  it('deve chamar onRetry ao clicar no botão', () => {
    const onRetry = vi.fn();
    render(<ErrorState onRetry={onRetry} />);
    fireEvent.click(screen.getByText('Tentar Novamente'));
    expect(onRetry).toHaveBeenCalledTimes(1);
  });
});

// ── Loading ───────────────────────────────────────────────────────────────────

describe('Loading', () => {
  it('deve renderizar mensagem padrão', () => {
    render(<Loading />);
    expect(screen.getByText('Preparando suas delícias...')).toBeInTheDocument();
  });

  it('deve renderizar mensagem customizada', () => {
    render(<Loading mensagem="Carregando produto..." />);
    expect(screen.getByText('Carregando produto...')).toBeInTheDocument();
  });
});

// ── MainScrollContainer ────────────────────────────────────────────────────────

describe('MainScrollContainer', () => {
  it('deve renderizar os filhos', () => {
    render(<MainScrollContainer><p>Conteúdo</p></MainScrollContainer>);
    expect(screen.getByText('Conteúdo')).toBeInTheDocument();
  });

  it('deve aplicar a altura customizada', () => {
    const { container } = render(<MainScrollContainer height="50vh"><p>X</p></MainScrollContainer>);
    expect(container.querySelector('.main-scroll-viewport')).toHaveStyle({ height: '50vh' });
  });
});

// ── LayoutStripes ──────────────────────────────────────────────────────────────

describe('LayoutStripes', () => {
  it('deve renderizar título e filhos quando fornecidos', () => {
    render(<LayoutStripes title="Login" image="logo.png"><p>form</p></LayoutStripes>);
    expect(screen.getByText('Login')).toBeInTheDocument();
    expect(screen.getByText('form')).toBeInTheDocument();
  });

  it('não deve renderizar título nem imagem quando ausentes', () => {
    const { container } = render(<LayoutStripes><p>form</p></LayoutStripes>);
    expect(container.querySelector('h1')).not.toBeInTheDocument();
    expect(container.querySelector('img.layout-logo-stripes')).not.toBeInTheDocument();
  });
});

// ── PrivateRoute ────────────────────────────────────────────────────────────────

describe('PrivateRoute', () => {
  it('quando autenticado, deve renderizar os filhos', () => {
    mockUseAuth.mockReturnValue({ isAuthenticated: true });
    render(<PrivateRoute><p>Conteúdo privado</p></PrivateRoute>);
    expect(screen.getByText('Conteúdo privado')).toBeInTheDocument();
  });

  it('quando não autenticado, não deve renderizar os filhos (redireciona)', () => {
    mockUseAuth.mockReturnValue({ isAuthenticated: false });
    render(
      <MemoryRouter>
        <PrivateRoute><p>Conteúdo privado</p></PrivateRoute>
      </MemoryRouter>
    );
    expect(screen.queryByText('Conteúdo privado')).not.toBeInTheDocument();
  });
});

// ── ProductCard ────────────────────────────────────────────────────────────────

describe('ProductCard', () => {
  const baseProps = { id: 1, name: 'Brownie', price: 12.5 };

  it('deve renderizar nome e preço formatado', () => {
    render(<ProductCard {...baseProps} />);
    expect(screen.getByText('Brownie')).toBeInTheDocument();
    expect(screen.getByText('12,50')).toBeInTheDocument();
  });

  it('deve renderizar preço antigo quando fornecido', () => {
    render(<ProductCard {...baseProps} oldPrice={20} />);
    expect(screen.getByText('R$ 20,00')).toBeInTheDocument();
  });

  it('não deve renderizar preço antigo quando ausente', () => {
    render(<ProductCard {...baseProps} />);
    expect(screen.queryByText(/R\$ 20,00/)).not.toBeInTheDocument();
  });

  it('clicar no card deve navegar para os detalhes do produto', () => {
    const { container } = render(<ProductCard {...baseProps} />);
    fireEvent.click(container.querySelector('.product-card'));
    expect(mockNavigate).toHaveBeenCalledWith('/details/1');
  });

  it('clicar em adicionar deve chamar onAdd sem navegar', () => {
    const onAdd = vi.fn();
    render(<ProductCard {...baseProps} onAdd={onAdd} />);
    fireEvent.click(screen.getByRole('button'));
    expect(onAdd).toHaveBeenCalledTimes(1);
    expect(mockNavigate).not.toHaveBeenCalled();
  });

  it('clicar em adicionar sem onAdd não deve quebrar', () => {
    render(<ProductCard {...baseProps} />);
    expect(() => fireEvent.click(screen.getByRole('button'))).not.toThrow();
  });
});

// ── QuantityControl ──────────────────────────────────────────────────────────

describe('QuantityControl', () => {
  it('deve exibir o valor atual', () => {
    render(<QuantityControl value={3} onChange={() => {}} />);
    expect(screen.getByText('3')).toBeInTheDocument();
  });

  it('deve chamar onChange com valor incrementado', () => {
    const onChange = vi.fn();
    render(<QuantityControl value={3} max={10} onChange={onChange} />);
    fireEvent.click(screen.getAllByRole('button')[1]);
    expect(onChange).toHaveBeenCalledWith(4);
  });

  it('deve chamar onChange com valor decrementado', () => {
    const onChange = vi.fn();
    render(<QuantityControl value={3} min={0} onChange={onChange} />);
    fireEvent.click(screen.getAllByRole('button')[0]);
    expect(onChange).toHaveBeenCalledWith(2);
  });

  it('botão de diminuir deve ficar desabilitado no mínimo', () => {
    const onChange = vi.fn();
    render(<QuantityControl value={0} min={0} onChange={onChange} />);
    fireEvent.click(screen.getAllByRole('button')[0]);
    expect(onChange).not.toHaveBeenCalled();
    expect(screen.getAllByRole('button')[0]).toBeDisabled();
  });

  it('botão de aumentar deve ficar desabilitado no máximo', () => {
    const onChange = vi.fn();
    render(<QuantityControl value={10} max={10} onChange={onChange} />);
    fireEvent.click(screen.getAllByRole('button')[1]);
    expect(onChange).not.toHaveBeenCalled();
    expect(screen.getAllByRole('button')[1]).toBeDisabled();
  });
});

// ── ScrollContainer ────────────────────────────────────────────────────────────

describe('ScrollContainer', () => {
  it('deve renderizar os filhos', () => {
    render(<ScrollContainer><p>Item 1</p></ScrollContainer>);
    expect(screen.getByText('Item 1')).toBeInTheDocument();
  });

  it('arrastar com o mouse deve aplicar a classe "active"', () => {
    const { container } = render(<ScrollContainer><p>Item</p></ScrollContainer>);
    const wrapper = container.querySelector('.scroll-wrapper');
    fireEvent.mouseDown(wrapper, { pageX: 100 });
    expect(wrapper.className).toMatch(/active/);
    fireEvent.mouseMove(wrapper, { pageX: 150 });
    fireEvent.mouseUp(wrapper);
    expect(wrapper.className).not.toMatch(/active/);
  });

  it('mouseLeave deve interromper o arraste', () => {
    const { container } = render(<ScrollContainer><p>Item</p></ScrollContainer>);
    const wrapper = container.querySelector('.scroll-wrapper');
    fireEvent.mouseDown(wrapper, { pageX: 100 });
    fireEvent.mouseLeave(wrapper);
    expect(wrapper.className).not.toMatch(/active/);
  });

  it('mover o mouse sem arrastar não deve quebrar', () => {
    const { container } = render(<ScrollContainer><p>Item</p></ScrollContainer>);
    const wrapper = container.querySelector('.scroll-wrapper');
    expect(() => fireEvent.mouseMove(wrapper, { pageX: 150 })).not.toThrow();
  });
});

// ── LayoutPage ────────────────────────────────────────────────────────────────

describe('LayoutPage', () => {
  it('quando não autenticado, deve exibir botão "Login"', () => {
    mockUseAuth.mockReturnValue({ user: null, isAuthenticated: false, logout: vi.fn() });
    mockUseCart.mockReturnValue({ totalItems: 0 });
    render(<MemoryRouter><LayoutPage /></MemoryRouter>);
    expect(screen.getByText('Login')).toBeInTheDocument();
  });

  it('quando autenticado, deve exibir nome do usuário e ícone de perfil', () => {
    mockUseAuth.mockReturnValue({ user: { nome: 'Arthur' }, isAuthenticated: true, logout: vi.fn() });
    mockUseCart.mockReturnValue({ totalItems: 2 });
    render(<MemoryRouter><LayoutPage /></MemoryRouter>);
    expect(screen.getByText('Sair (Arthur)')).toBeInTheDocument();
    expect(screen.getByText(/2 Itens/)).toBeInTheDocument();
  });

  it('clicar no botão de usuário autenticado deve fazer logout e navegar', () => {
    const logout = vi.fn();
    mockUseAuth.mockReturnValue({ user: { nome: 'Arthur' }, isAuthenticated: true, logout });
    mockUseCart.mockReturnValue({ totalItems: 0 });
    render(<MemoryRouter><LayoutPage /></MemoryRouter>);
    fireEvent.click(screen.getByText('Sair (Arthur)'));
    expect(logout).toHaveBeenCalled();
    expect(mockNavigate).toHaveBeenCalledWith('/');
  });

  it('clicar no botão de login deve navegar para /login', () => {
    mockUseAuth.mockReturnValue({ user: null, isAuthenticated: false, logout: vi.fn() });
    mockUseCart.mockReturnValue({ totalItems: 0 });
    render(<MemoryRouter><LayoutPage /></MemoryRouter>);
    fireEvent.click(screen.getByText('Login'));
    expect(mockNavigate).toHaveBeenCalledWith('/login');
  });

  it('clicar na logo deve navegar para /home', () => {
    render(<MemoryRouter><LayoutPage /></MemoryRouter>);
    fireEvent.click(screen.getByText('Sweet Delights'));
    expect(mockNavigate).toHaveBeenCalledWith('/home');
  });

  it('clicar no carrinho deve navegar para /carrinho', () => {
    render(<MemoryRouter><LayoutPage /></MemoryRouter>);
    fireEvent.click(screen.getByText(/Itens/));
    expect(mockNavigate).toHaveBeenCalledWith('/carrinho');
  });
});
