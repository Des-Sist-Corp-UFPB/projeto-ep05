import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

const mockSolicitarRecuperacao = vi.fn();
const mockRedefinirSenha = vi.fn();
vi.mock('../context/AuthContext', () => ({
  useAuth: () => ({
    solicitarRecuperacao: mockSolicitarRecuperacao,
    redefinirSenha: mockRedefinirSenha,
  }),
}));

import ForgotPassword from '../pages/ForgotPassword/ForgotPassword';

beforeEach(() => {
  mockSolicitarRecuperacao.mockReset();
  mockRedefinirSenha.mockReset();
});

function renderPage() {
  return render(<MemoryRouter><ForgotPassword /></MemoryRouter>);
}

describe('ForgotPassword', () => {
  it('passo 1: deve renderizar o campo de e-mail', () => {
    renderPage();
    expect(screen.getByPlaceholderText('seu@email.com')).toBeInTheDocument();
  });

  it('passo 1: submeter sem e-mail deve mostrar erro e não chamar a API', async () => {
    renderPage();
    fireEvent.click(screen.getByRole('button', { name: 'Enviar link de recuperação' }));
    expect(await screen.findByText('Informe seu e-mail')).toBeInTheDocument();
    expect(mockSolicitarRecuperacao).not.toHaveBeenCalled();
  });

  it('passo 1: sucesso deve avançar para o passo 2 (token)', async () => {
    mockSolicitarRecuperacao.mockResolvedValueOnce({ success: true });
    renderPage();
    fireEvent.change(screen.getByPlaceholderText('seu@email.com'), { target: { value: 'a@b.com' } });
    fireEvent.click(screen.getByRole('button', { name: 'Enviar link de recuperação' }));

    expect(await screen.findByPlaceholderText('Cole o token aqui')).toBeInTheDocument();
  });

  it('passo 1: falha na API deve exibir mensagem de erro', async () => {
    mockSolicitarRecuperacao.mockResolvedValueOnce({ success: false, message: 'E-mail não encontrado' });
    renderPage();
    fireEvent.change(screen.getByPlaceholderText('seu@email.com'), { target: { value: 'a@b.com' } });
    fireEvent.click(screen.getByRole('button', { name: 'Enviar link de recuperação' }));

    expect(await screen.findByText('E-mail não encontrado')).toBeInTheDocument();
  });

  async function avancarParaPasso2() {
    mockSolicitarRecuperacao.mockResolvedValueOnce({ success: true });
    fireEvent.change(screen.getByPlaceholderText('seu@email.com'), { target: { value: 'a@b.com' } });
    fireEvent.click(screen.getByRole('button', { name: 'Enviar link de recuperação' }));
    await screen.findByPlaceholderText('Cole o token aqui');
  }

  it('passo 2: token vazio deve mostrar erro', async () => {
    renderPage();
    await avancarParaPasso2();

    fireEvent.click(screen.getByRole('button', { name: 'Redefinir senha' }));
    expect(await screen.findByText('Cole o token recebido por e-mail')).toBeInTheDocument();
  });

  it('passo 2: senha curta deve mostrar erro', async () => {
    renderPage();
    await avancarParaPasso2();

    fireEvent.change(screen.getByPlaceholderText('Cole o token aqui'), { target: { value: 'tok123' } });
    fireEvent.change(screen.getByPlaceholderText('Mínimo 6 caracteres'), { target: { value: '123' } });
    fireEvent.click(screen.getByRole('button', { name: 'Redefinir senha' }));

    expect(await screen.findByText('A senha deve ter pelo menos 6 caracteres')).toBeInTheDocument();
  });

  it('passo 2: senhas diferentes devem mostrar erro', async () => {
    renderPage();
    await avancarParaPasso2();

    fireEvent.change(screen.getByPlaceholderText('Cole o token aqui'), { target: { value: 'tok123' } });
    fireEvent.change(screen.getByPlaceholderText('Mínimo 6 caracteres'), { target: { value: '123456' } });
    fireEvent.change(screen.getByPlaceholderText('Repita a nova senha'), { target: { value: '654321' } });
    fireEvent.click(screen.getByRole('button', { name: 'Redefinir senha' }));

    expect(await screen.findByText('As senhas não coincidem')).toBeInTheDocument();
  });

  it('passo 2: sucesso deve avançar para o passo 3', async () => {
    mockRedefinirSenha.mockResolvedValueOnce({ success: true });
    renderPage();
    await avancarParaPasso2();

    fireEvent.change(screen.getByPlaceholderText('Cole o token aqui'), { target: { value: 'tok123' } });
    fireEvent.change(screen.getByPlaceholderText('Mínimo 6 caracteres'), { target: { value: '123456' } });
    fireEvent.change(screen.getByPlaceholderText('Repita a nova senha'), { target: { value: '123456' } });
    fireEvent.click(screen.getByRole('button', { name: 'Redefinir senha' }));

    expect(await screen.findByText(/Senha redefinida com sucesso/)).toBeInTheDocument();
    expect(screen.getByText('Ir para o login')).toBeInTheDocument();
  });

  it('passo 2: falha da API deve exibir mensagem de erro', async () => {
    mockRedefinirSenha.mockResolvedValueOnce({ success: false, message: 'Token inválido ou expirado' });
    renderPage();
    await avancarParaPasso2();

    fireEvent.change(screen.getByPlaceholderText('Cole o token aqui'), { target: { value: 'tok-invalido' } });
    fireEvent.change(screen.getByPlaceholderText('Mínimo 6 caracteres'), { target: { value: '123456' } });
    fireEvent.change(screen.getByPlaceholderText('Repita a nova senha'), { target: { value: '123456' } });
    fireEvent.click(screen.getByRole('button', { name: 'Redefinir senha' }));

    expect(await screen.findByText('Token inválido ou expirado')).toBeInTheDocument();
  });

  it('deve exibir link "Entrar" enquanto não chegou ao passo 3', () => {
    renderPage();
    expect(screen.getByText('Entrar')).toBeInTheDocument();
  });
});
