import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

const mockNavigate = vi.fn();
let mockLocationValue = { pathname: '/login', state: null };

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
    useLocation: () => mockLocationValue,
  };
});

const mockLogin = vi.fn();
vi.mock('../context/AuthContext', () => ({
  useAuth: () => ({ login: mockLogin }),
}));

import Login from '../pages/Login/Login';

beforeEach(() => {
  mockNavigate.mockClear();
  mockLogin.mockReset();
  mockLocationValue = { pathname: '/login', state: null };
});

describe('Login', () => {
  it('deve renderizar os campos de e-mail e senha', () => {
    render(<MemoryRouter><Login /></MemoryRouter>);
    expect(screen.getByPlaceholderText('seu@email.com')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('••••••••')).toBeInTheDocument();
  });

  it('deve atualizar o estado ao digitar', () => {
    render(<MemoryRouter><Login /></MemoryRouter>);
    fireEvent.change(screen.getByPlaceholderText('seu@email.com'), { target: { value: 'a@b.com' } });
    expect(screen.getByPlaceholderText('seu@email.com')).toHaveValue('a@b.com');
  });

  it('login bem-sucedido deve navegar para a rota padrão "/"', async () => {
    mockLogin.mockResolvedValueOnce({ success: true });
    render(<MemoryRouter><Login /></MemoryRouter>);

    fireEvent.change(screen.getByPlaceholderText('seu@email.com'), { target: { value: 'a@b.com' } });
    fireEvent.change(screen.getByPlaceholderText('••••••••'), { target: { value: 'senha123' } });
    fireEvent.click(screen.getByRole('button', { name: 'Entrar' }));

    await waitFor(() => expect(mockNavigate).toHaveBeenCalledWith('/', { replace: true }));
  });

  it('login bem-sucedido vindo de rota protegida deve voltar para ela', async () => {
    mockLocationValue = { pathname: '/login', state: { from: { pathname: '/perfil' } } };
    mockLogin.mockResolvedValueOnce({ success: true });
    render(<MemoryRouter><Login /></MemoryRouter>);

    fireEvent.click(screen.getByRole('button', { name: 'Entrar' }));

    await waitFor(() => expect(mockNavigate).toHaveBeenCalledWith('/perfil', { replace: true }));
  });

  it('login com falha deve exibir mensagem de erro e não navegar', async () => {
    mockLogin.mockResolvedValueOnce({ success: false, message: 'Credenciais inválidas' });
    render(<MemoryRouter><Login /></MemoryRouter>);

    fireEvent.click(screen.getByRole('button', { name: 'Entrar' }));

    await waitFor(() => expect(screen.getByText('Credenciais inválidas')).toBeInTheDocument());
    expect(mockNavigate).not.toHaveBeenCalled();
  });

  it('login com falha sem mensagem deve usar mensagem padrão', async () => {
    mockLogin.mockResolvedValueOnce({ success: false });
    render(<MemoryRouter><Login /></MemoryRouter>);

    fireEvent.click(screen.getByRole('button', { name: 'Entrar' }));

    await waitFor(() => expect(screen.getByText('E-mail ou senha inválidos')).toBeInTheDocument());
  });

  it('digitar novamente após erro deve limpar a mensagem de erro', async () => {
    mockLogin.mockResolvedValueOnce({ success: false, message: 'Erro' });
    render(<MemoryRouter><Login /></MemoryRouter>);

    fireEvent.click(screen.getByRole('button', { name: 'Entrar' }));
    await waitFor(() => expect(screen.getByText('Erro')).toBeInTheDocument());

    fireEvent.change(screen.getByPlaceholderText('seu@email.com'), { target: { value: 'novo@b.com' } });
    expect(screen.queryByText('Erro')).not.toBeInTheDocument();
  });

  it('durante o carregamento o botão deve mostrar "Entrando..." e ficar desabilitado', async () => {
    let resolveLogin;
    mockLogin.mockReturnValueOnce(new Promise((resolve) => { resolveLogin = resolve; }));
    render(<MemoryRouter><Login /></MemoryRouter>);

    fireEvent.click(screen.getByRole('button', { name: 'Entrar' }));

    expect(await screen.findByRole('button', { name: 'Entrando...' })).toBeDisabled();

    // Resolve a promise pendente e aguarda a atualização de estado consequente
    // (setLoading(false) + navigate) dentro do mesmo ciclo do teste, evitando
    // o aviso "not wrapped in act(...)" do React.
    await act(async () => {
      resolveLogin({ success: true });
      await Promise.resolve();
    });
  });
});
