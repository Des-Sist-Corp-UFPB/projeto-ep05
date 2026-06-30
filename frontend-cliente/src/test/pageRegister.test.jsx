import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return { ...actual, useNavigate: () => mockNavigate };
});

const mockRegister = vi.fn();
vi.mock('../context/AuthContext', () => ({
  useAuth: () => ({ register: mockRegister }),
}));

import Register from '../pages/Register/Register';

beforeEach(() => {
  mockNavigate.mockClear();
  mockRegister.mockReset();
});

function renderRegister() {
  return render(<MemoryRouter><Register /></MemoryRouter>);
}

function preencherFormularioValido() {
  fireEvent.change(screen.getByLabelText('Nome'), { target: { name: 'nome', value: 'Arthur Silva' } });
  fireEvent.change(screen.getByLabelText('Sobrenome'), { target: { name: 'sobrenome', value: 'Pereira' } });
  fireEvent.change(screen.getByLabelText('E-mail'), { target: { name: 'email', value: 'arthur@b.com' } });
  fireEvent.change(screen.getByLabelText('CPF'), { target: { name: 'cpf', value: '123.456.789-00' } });
  fireEvent.change(screen.getByLabelText('Telefone'), { target: { name: 'telefone', value: '(11) 91234-5678' } });
  fireEvent.change(screen.getByLabelText('Data de nascimento'), { target: { name: 'dataNascimento', value: '1990-01-01' } });
  fireEvent.change(screen.getByLabelText('Senha'), { target: { name: 'senha', value: 'Senha@123' } });
  fireEvent.change(screen.getByLabelText('Confirmar senha'), { target: { name: 'confirmarSenha', value: 'Senha@123' } });
}

describe('Register', () => {
  it('deve renderizar todos os campos do formulário', () => {
    renderRegister();
    expect(screen.getByLabelText('Nome')).toBeInTheDocument();
    expect(screen.getByLabelText('E-mail')).toBeInTheDocument();
    expect(screen.getByLabelText('Senha')).toBeInTheDocument();
    expect(screen.getByLabelText('Confirmar senha')).toBeInTheDocument();
  });

  it('submeter formulário vazio deve exibir erros de validação e não chamar register', async () => {
    renderRegister();
    fireEvent.click(screen.getByRole('button', { name: 'Cadastrar' }));

    await waitFor(() => expect(screen.getByText('Nome é obrigatório')).toBeInTheDocument());
    expect(mockRegister).not.toHaveBeenCalled();
  });

  it('digitar um campo deve validar em tempo real', () => {
    renderRegister();
    fireEvent.change(screen.getByLabelText('E-mail'), { target: { name: 'email', value: 'naoeemail' } });
    expect(screen.getByText('Digite um e-mail válido')).toBeInTheDocument();
  });

  it('cadastro bem-sucedido deve navegar para "/"', async () => {
    mockRegister.mockResolvedValueOnce({ success: true });
    renderRegister();
    preencherFormularioValido();

    fireEvent.click(screen.getByRole('button', { name: 'Cadastrar' }));

    await waitFor(() => expect(mockNavigate).toHaveBeenCalledWith('/'));
    expect(mockRegister).toHaveBeenCalledWith(expect.objectContaining({ nome: 'Arthur Silva', email: 'arthur@b.com' }));
  });

  it('cadastro com falha deve exibir erro geral e não navegar', async () => {
    mockRegister.mockResolvedValueOnce({ success: false, message: 'E-mail já cadastrado' });
    renderRegister();
    preencherFormularioValido();

    fireEvent.click(screen.getByRole('button', { name: 'Cadastrar' }));

    await waitFor(() => expect(screen.getByText('E-mail já cadastrado')).toBeInTheDocument());
    expect(mockNavigate).not.toHaveBeenCalled();
  });

  it('editar um campo após erro geral deve limpar a mensagem de erro', async () => {
    mockRegister.mockResolvedValueOnce({ success: false, message: 'E-mail já cadastrado' });
    renderRegister();
    preencherFormularioValido();

    fireEvent.click(screen.getByRole('button', { name: 'Cadastrar' }));
    await waitFor(() => expect(screen.getByText('E-mail já cadastrado')).toBeInTheDocument());

    fireEvent.change(screen.getByLabelText('Nome'), { target: { name: 'nome', value: 'Outro Nome' } });

    expect(screen.queryByText('E-mail já cadastrado')).not.toBeInTheDocument();
  });

  it('cadastro com falha sem mensagem deve usar mensagem padrão', async () => {
    mockRegister.mockResolvedValueOnce({ success: false });
    renderRegister();
    preencherFormularioValido();

    fireEvent.click(screen.getByRole('button', { name: 'Cadastrar' }));

    await waitFor(() => expect(screen.getByText('Erro ao cadastrar. Tente novamente.')).toBeInTheDocument());
  });

  it('durante o carregamento, o botão deve mostrar "Cadastrando..."', async () => {
    let resolveRegister;
    mockRegister.mockReturnValueOnce(new Promise((resolve) => { resolveRegister = resolve; }));
    renderRegister();
    preencherFormularioValido();

    fireEvent.click(screen.getByRole('button', { name: 'Cadastrar' }));

    expect(await screen.findByRole('button', { name: 'Cadastrando...' })).toBeDisabled();

    await act(async () => {
      resolveRegister({ success: true });
      await Promise.resolve();
    });
  });
});
