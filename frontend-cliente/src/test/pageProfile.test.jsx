import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return { ...actual, useNavigate: () => mockNavigate };
});

const mockUpdateUser = vi.fn();
const mockDeleteUser = vi.fn();
let mockUserValue = { nome: 'Arthur', email: 'arthur@b.com' };

vi.mock('../context/AuthContext', () => ({
  useAuth: () => ({ user: mockUserValue, updateUser: mockUpdateUser, deleteUser: mockDeleteUser }),
}));

import Profile from '../pages/Profile/Profile';

beforeEach(() => {
  mockNavigate.mockClear();
  mockUpdateUser.mockReset();
  mockDeleteUser.mockReset();
  mockUserValue = { nome: 'Arthur', email: 'arthur@b.com' };
  vi.useRealTimers();
});

function renderPage() {
  return render(<MemoryRouter><Profile /></MemoryRouter>);
}

function campo(container, name) {
  return container.querySelector(`input[name="${name}"]`);
}

describe('Profile', () => {
  it('deve exibir saudação e dados do usuário', () => {
    renderPage();
    expect(screen.getByText('Olá, Arthur!')).toBeInTheDocument();
    expect(screen.getByText('arthur@b.com')).toBeInTheDocument();
  });

  it('avatar deve exibir a inicial do nome em maiúscula', () => {
    renderPage();
    expect(screen.getByText('A')).toBeInTheDocument();
  });

  it('inicialmente, os campos devem estar desabilitados (modo leitura)', () => {
    const { container } = renderPage();
    expect(campo(container, 'nome')).toBeDisabled();
  });

  it('clicar em "Editar Dados" deve habilitar os campos e exibir campos de senha', () => {
    const { container } = renderPage();
    fireEvent.click(screen.getByText('Editar Dados'));

    expect(campo(container, 'nome')).not.toBeDisabled();
    expect(campo(container, 'senhaAtual')).toBeInTheDocument();
    expect(screen.getByText('Editando Perfil')).toBeInTheDocument();
  });

  it('clicar em "Cancelar" deve sair do modo de edição e restaurar os dados', () => {
    const { container } = renderPage();
    fireEvent.click(screen.getByText('Editar Dados'));
    fireEvent.change(campo(container, 'nome'), { target: { name: 'nome', value: 'Outro Nome' } });

    fireEvent.click(screen.getByText('Cancelar'));

    expect(screen.getByText('Olá, Arthur!')).toBeInTheDocument();
    expect(screen.queryByPlaceholderText('Obrigatório para alterar a senha')).not.toBeInTheDocument();
  });

  it('salvar sem nome deve mostrar erro de validação e não chamar updateUser', async () => {
    const { container } = renderPage();
    fireEvent.click(screen.getByText('Editar Dados'));
    fireEvent.change(campo(container, 'nome'), { target: { name: 'nome', value: '' } });

    fireEvent.click(screen.getByRole('button', { name: 'Salvar' }));

    expect(await screen.findByText('Nome é obrigatório')).toBeInTheDocument();
    expect(mockUpdateUser).not.toHaveBeenCalled();
  });

  it('alterar senha sem informar senha atual deve mostrar erro', async () => {
    const { container } = renderPage();
    fireEvent.click(screen.getByText('Editar Dados'));
    fireEvent.change(campo(container, 'novaSenha'), { target: { name: 'novaSenha', value: 'NovaSenha123' } });

    fireEvent.click(screen.getByRole('button', { name: 'Salvar' }));

    expect(await screen.findByText('Informe sua senha atual para alterar a senha')).toBeInTheDocument();
  });

  it('confirmar nova senha diferente deve mostrar erro', async () => {
    const { container } = renderPage();
    fireEvent.click(screen.getByText('Editar Dados'));
    fireEvent.change(campo(container, 'senhaAtual'), { target: { name: 'senhaAtual', value: 'antiga123' } });
    fireEvent.change(campo(container, 'novaSenha'), { target: { name: 'novaSenha', value: 'NovaSenha123' } });
    fireEvent.change(campo(container, 'confirmarNovaSenha'), { target: { name: 'confirmarNovaSenha', value: 'Diferente123' } });

    fireEvent.click(screen.getByRole('button', { name: 'Salvar' }));

    expect(await screen.findByText('As senhas não coincidem')).toBeInTheDocument();
  });

  it('salvar com sucesso deve sair do modo de edição e exibir mensagem de sucesso', async () => {
    mockUpdateUser.mockResolvedValueOnce({ success: true, message: 'Perfil atualizado com sucesso!' });
    const { container } = renderPage();
    fireEvent.click(screen.getByText('Editar Dados'));
    fireEvent.change(campo(container, 'nome'), { target: { name: 'nome', value: 'Arthur Silva' } });

    fireEvent.click(screen.getByRole('button', { name: 'Salvar' }));

    expect(await screen.findByText('Perfil atualizado com sucesso!')).toBeInTheDocument();
    expect(mockUpdateUser).toHaveBeenCalledWith(expect.objectContaining({ nome: 'Arthur Silva' }));
  });

  it('salvar com falha na API deve exibir mensagem de erro e continuar editando', async () => {
    mockUpdateUser.mockResolvedValueOnce({ success: false, message: 'E-mail já em uso' });
    renderPage();
    fireEvent.click(screen.getByText('Editar Dados'));

    fireEvent.click(screen.getByRole('button', { name: 'Salvar' }));

    expect(await screen.findByText('E-mail já em uso')).toBeInTheDocument();
  });

  it('clicar em "Excluir minha conta" deve mostrar a confirmação inline', () => {
    renderPage();
    fireEvent.click(screen.getByText('Excluir minha conta'));
    expect(screen.getByText(/Tem certeza/)).toBeInTheDocument();
  });

  it('cancelar a exclusão deve esconder a confirmação', () => {
    renderPage();
    fireEvent.click(screen.getByText('Excluir minha conta'));
    fireEvent.click(screen.getByText('Cancelar', { selector: '.btn-cancel-delete' }));
    expect(screen.queryByText(/Tem certeza/)).not.toBeInTheDocument();
  });

  it('confirmar a exclusão deve chamar deleteUser e navegar para "/"', async () => {
    mockDeleteUser.mockResolvedValueOnce();
    renderPage();
    fireEvent.click(screen.getByText('Excluir minha conta'));
    fireEvent.click(screen.getByText('Sim, excluir minha conta'));

    await waitFor(() => expect(mockDeleteUser).toHaveBeenCalled());
    expect(mockNavigate).toHaveBeenCalledWith('/');
  });
});
