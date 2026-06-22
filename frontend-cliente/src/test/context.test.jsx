import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { renderHook, act, waitFor } from '@testing-library/react';

// ── AuthContext ────────────────────────────────────────────────────────────

const mockApiFetch = vi.fn();
vi.mock('../api/api', () => ({
  apiFetch: (...args) => mockApiFetch(...args),
}));

import { AuthProvider, useAuth } from '../context/AuthContext';

describe('AuthContext', () => {
  beforeEach(() => {
    localStorage.clear();
    mockApiFetch.mockReset();
  });

  it('useAuth fora do AuthProvider deve lançar erro', () => {
    expect(() => renderHook(() => useAuth())).toThrow(
      'useAuth deve ser usado dentro de AuthProvider'
    );
  });

  it('sem usuário salvo, deve iniciar não autenticado', async () => {
    const { result } = renderHook(() => useAuth(), { wrapper: AuthProvider });
    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(result.current.isAuthenticated).toBe(false);
    expect(result.current.user).toBeNull();
  });

  it('com usuário salvo no localStorage, deve hidratar o estado', async () => {
    localStorage.setItem('user', JSON.stringify({ nome: 'Arthur', email: 'a@b.com', token: 'tok' }));
    const { result } = renderHook(() => useAuth(), { wrapper: AuthProvider });
    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(result.current.isAuthenticated).toBe(true);
    expect(result.current.user.nome).toBe('Arthur');
  });

  it('com localStorage corrompido, deve limpar e seguir deslogado', async () => {
    localStorage.setItem('user', '{ json invalido');
    const { result } = renderHook(() => useAuth(), { wrapper: AuthProvider });
    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(result.current.isAuthenticated).toBe(false);
    expect(localStorage.getItem('user')).toBeNull();
  });

  it('login bem-sucedido deve persistir o usuário', async () => {
    mockApiFetch.mockResolvedValueOnce({ token: 'tok', email: 'a@b.com', nome: 'Arthur', papel: 'CLIENTE' });
    const { result } = renderHook(() => useAuth(), { wrapper: AuthProvider });
    await waitFor(() => expect(result.current.loading).toBe(false));

    let response;
    await act(async () => {
      response = await result.current.login('a@b.com', 'senha123');
    });

    expect(response.success).toBe(true);
    expect(result.current.isAuthenticated).toBe(true);
    expect(JSON.parse(localStorage.getItem('user')).nome).toBe('Arthur');
  });

  it('login com falha deve retornar mensagem de erro', async () => {
    mockApiFetch.mockRejectedValueOnce({ mensagem: 'Credenciais inválidas' });
    const { result } = renderHook(() => useAuth(), { wrapper: AuthProvider });
    await waitFor(() => expect(result.current.loading).toBe(false));

    let response;
    await act(async () => {
      response = await result.current.login('a@b.com', 'errada');
    });

    expect(response.success).toBe(false);
    expect(response.message).toBe('Credenciais inválidas');
    expect(result.current.isAuthenticated).toBe(false);
  });

  it('login com falha sem mensagem deve usar mensagem padrão', async () => {
    mockApiFetch.mockRejectedValueOnce({});
    const { result } = renderHook(() => useAuth(), { wrapper: AuthProvider });
    await waitFor(() => expect(result.current.loading).toBe(false));

    let response;
    await act(async () => {
      response = await result.current.login('a@b.com', 'errada');
    });
    expect(response.message).toBe('E-mail ou senha inválidos');
  });

  it('register bem-sucedido deve persistir o usuário', async () => {
    mockApiFetch.mockResolvedValueOnce({ token: 'tok', email: 'novo@b.com', nome: 'Novo', papel: 'CLIENTE' });
    const { result } = renderHook(() => useAuth(), { wrapper: AuthProvider });
    await waitFor(() => expect(result.current.loading).toBe(false));

    let response;
    await act(async () => {
      response = await result.current.register({ nome: 'Novo', email: 'novo@b.com', senha: 'senha123' });
    });

    expect(response.success).toBe(true);
    expect(result.current.user.email).toBe('novo@b.com');
  });

  it('register com falha deve retornar mensagem de erro', async () => {
    mockApiFetch.mockRejectedValueOnce({ mensagem: 'E-mail já cadastrado' });
    const { result } = renderHook(() => useAuth(), { wrapper: AuthProvider });
    await waitFor(() => expect(result.current.loading).toBe(false));

    let response;
    await act(async () => {
      response = await result.current.register({ nome: 'Novo', email: 'dup@b.com', senha: 'senha123' });
    });
    expect(response.success).toBe(false);
    expect(response.message).toBe('E-mail já cadastrado');
  });

  it('logout deve limpar o usuário', async () => {
    localStorage.setItem('user', JSON.stringify({ nome: 'Arthur', token: 'tok' }));
    const { result } = renderHook(() => useAuth(), { wrapper: AuthProvider });
    await waitFor(() => expect(result.current.loading).toBe(false));

    act(() => result.current.logout());

    expect(result.current.isAuthenticated).toBe(false);
    expect(localStorage.getItem('user')).toBeNull();
  });

  it('updateUser bem-sucedido deve atualizar nome/email', async () => {
    localStorage.setItem('user', JSON.stringify({ nome: 'Arthur', email: 'a@b.com', token: 'tok' }));
    mockApiFetch.mockResolvedValueOnce({ nome: 'Arthur Silva', email: 'novo@b.com' });
    const { result } = renderHook(() => useAuth(), { wrapper: AuthProvider });
    await waitFor(() => expect(result.current.loading).toBe(false));

    let response;
    await act(async () => {
      response = await result.current.updateUser({ nome: 'Arthur Silva', email: 'novo@b.com' });
    });

    expect(response.success).toBe(true);
    expect(result.current.user.nome).toBe('Arthur Silva');
  });

  it('updateUser com falha deve retornar mensagem de erro', async () => {
    localStorage.setItem('user', JSON.stringify({ nome: 'Arthur', email: 'a@b.com', token: 'tok' }));
    mockApiFetch.mockRejectedValueOnce({ mensagem: 'Senha atual incorreta' });
    const { result } = renderHook(() => useAuth(), { wrapper: AuthProvider });
    await waitFor(() => expect(result.current.loading).toBe(false));

    let response;
    await act(async () => {
      response = await result.current.updateUser({ nome: 'X', email: 'a@b.com', senhaAtual: 'errada', novaSenha: 'nova123' });
    });
    expect(response.success).toBe(false);
    expect(response.message).toBe('Senha atual incorreta');
  });

  it('deleteUser deve limpar o usuário mesmo com sucesso na API', async () => {
    localStorage.setItem('user', JSON.stringify({ nome: 'Arthur', token: 'tok' }));
    mockApiFetch.mockResolvedValueOnce(null);
    const { result } = renderHook(() => useAuth(), { wrapper: AuthProvider });
    await waitFor(() => expect(result.current.loading).toBe(false));

    await act(async () => {
      await result.current.deleteUser();
    });

    expect(result.current.isAuthenticated).toBe(false);
    expect(localStorage.getItem('user')).toBeNull();
  });

  it('deleteUser deve limpar o usuário mesmo se a API falhar', async () => {
    localStorage.setItem('user', JSON.stringify({ nome: 'Arthur', token: 'tok' }));
    mockApiFetch.mockRejectedValueOnce(new Error('falhou'));
    const { result } = renderHook(() => useAuth(), { wrapper: AuthProvider });
    await waitFor(() => expect(result.current.loading).toBe(false));

    await act(async () => {
      await result.current.deleteUser();
    });

    expect(result.current.isAuthenticated).toBe(false);
  });

  it('solicitarRecuperacao bem-sucedida deve retornar token (dev)', async () => {
    mockApiFetch.mockResolvedValueOnce({ token: 'abc123' });
    const { result } = renderHook(() => useAuth(), { wrapper: AuthProvider });
    await waitFor(() => expect(result.current.loading).toBe(false));

    let response;
    await act(async () => {
      response = await result.current.solicitarRecuperacao('a@b.com');
    });
    expect(response.success).toBe(true);
    expect(response.token).toBe('abc123');
  });

  it('solicitarRecuperacao com falha deve retornar mensagem padrão', async () => {
    mockApiFetch.mockRejectedValueOnce({});
    const { result } = renderHook(() => useAuth(), { wrapper: AuthProvider });
    await waitFor(() => expect(result.current.loading).toBe(false));

    let response;
    await act(async () => {
      response = await result.current.solicitarRecuperacao('a@b.com');
    });
    expect(response.success).toBe(false);
    expect(response.message).toBe('Erro ao solicitar recuperação');
  });

  it('redefinirSenha bem-sucedida deve retornar success true', async () => {
    mockApiFetch.mockResolvedValueOnce({});
    const { result } = renderHook(() => useAuth(), { wrapper: AuthProvider });
    await waitFor(() => expect(result.current.loading).toBe(false));

    let response;
    await act(async () => {
      response = await result.current.redefinirSenha('token123', 'novaSenha123');
    });
    expect(response.success).toBe(true);
  });

  it('redefinirSenha com falha deve retornar mensagem padrão', async () => {
    mockApiFetch.mockRejectedValueOnce({});
    const { result } = renderHook(() => useAuth(), { wrapper: AuthProvider });
    await waitFor(() => expect(result.current.loading).toBe(false));

    let response;
    await act(async () => {
      response = await result.current.redefinirSenha('token-invalido', 'novaSenha123');
    });
    expect(response.success).toBe(false);
    expect(response.message).toBe('Token inválido ou expirado');
  });
});

// ── CartContext ────────────────────────────────────────────────────────────

import { CartProvider, useCart } from '../context/CartContext';

describe('CartContext', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('useCart fora do CartProvider deve lançar erro', () => {
    expect(() => renderHook(() => useCart())).toThrow(
      'useCart deve ser usado dentro de CartProvider'
    );
  });

  it('deve iniciar vazio quando não há carrinho salvo', () => {
    const { result } = renderHook(() => useCart(), { wrapper: CartProvider });
    expect(result.current.cart).toEqual([]);
    expect(result.current.total).toBe(0);
    expect(result.current.totalItems).toBe(0);
  });

  it('deve hidratar o carrinho a partir do localStorage', () => {
    localStorage.setItem('cart', JSON.stringify([{ id: 1, name: 'Brownie', price: 10, quantity: 2 }]));
    const { result } = renderHook(() => useCart(), { wrapper: CartProvider });
    expect(result.current.cart).toHaveLength(1);
    expect(result.current.totalItems).toBe(2);
    expect(result.current.total).toBe(20);
  });

  it('addToCart deve adicionar um novo produto', () => {
    const { result } = renderHook(() => useCart(), { wrapper: CartProvider });
    act(() => result.current.addToCart({ id: 1, name: 'Brownie', price: 10 }));
    expect(result.current.cart).toHaveLength(1);
    expect(result.current.cart[0].quantity).toBe(1);
  });

  it('addToCart de produto já existente deve incrementar a quantidade', () => {
    const { result } = renderHook(() => useCart(), { wrapper: CartProvider });
    act(() => result.current.addToCart({ id: 1, name: 'Brownie', price: 10 }));
    act(() => result.current.addToCart({ id: 1, name: 'Brownie', price: 10 }));
    expect(result.current.cart).toHaveLength(1);
    expect(result.current.cart[0].quantity).toBe(2);
  });

  it('removeFromCart deve remover o item', () => {
    const { result } = renderHook(() => useCart(), { wrapper: CartProvider });
    act(() => result.current.addToCart({ id: 1, name: 'Brownie', price: 10 }));
    act(() => result.current.removeFromCart(1));
    expect(result.current.cart).toHaveLength(0);
  });

  it('updateQuantity deve atualizar a quantidade do item', () => {
    const { result } = renderHook(() => useCart(), { wrapper: CartProvider });
    act(() => result.current.addToCart({ id: 1, name: 'Brownie', price: 10 }));
    act(() => result.current.updateQuantity(1, 5));
    expect(result.current.cart[0].quantity).toBe(5);
  });

  it('updateQuantity com valor <= 0 deve remover o item', () => {
    const { result } = renderHook(() => useCart(), { wrapper: CartProvider });
    act(() => result.current.addToCart({ id: 1, name: 'Brownie', price: 10 }));
    act(() => result.current.updateQuantity(1, 0));
    expect(result.current.cart).toHaveLength(0);
  });

  it('clearCart deve esvaziar o carrinho', () => {
    const { result } = renderHook(() => useCart(), { wrapper: CartProvider });
    act(() => result.current.addToCart({ id: 1, name: 'Brownie', price: 10 }));
    act(() => result.current.clearCart());
    expect(result.current.cart).toHaveLength(0);
  });

  it('total e totalItems devem refletir múltiplos produtos', () => {
    const { result } = renderHook(() => useCart(), { wrapper: CartProvider });
    act(() => result.current.addToCart({ id: 1, name: 'Brownie', price: 10 }));
    act(() => result.current.addToCart({ id: 2, name: 'Cookie', price: 5 }));
    act(() => result.current.updateQuantity(2, 3));
    expect(result.current.totalItems).toBe(4);
    expect(result.current.total).toBe(25);
  });
});
