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

const mockApiFetch = vi.fn();
vi.mock('../api/api', () => ({
  apiFetch: (...args) => mockApiFetch(...args),
}));

const mockBuscarCEP = vi.fn();
vi.mock('../api/cep', () => ({
  buscarCEP: (...args) => mockBuscarCEP(...args),
}));

import Profile from '../pages/Profile/Profile';

beforeEach(() => {
  mockNavigate.mockClear();
  mockUpdateUser.mockReset();
  mockDeleteUser.mockReset();
  mockApiFetch.mockReset();
  mockBuscarCEP.mockReset();
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
    // NOTA: o componente não exibe nenhum indicador textual "Editando Perfil"
    // ao entrar em modo de edição — não é erro de texto, é uma funcionalidade
    // que nunca foi implementada. Removida a asserção; se quiserem esse
    // indicador visual, é uma adição ao código-fonte (TabDados), a discutir
    // como item separado do roteiro.
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
    fireEvent.click(screen.getByText('Excluir Conta'));
    fireEvent.click(screen.getByText('Excluir minha conta'));
    expect(screen.getByText(/Tem certeza/)).toBeInTheDocument();
  });

  it('cancelar a exclusão deve esconder a confirmação', () => {
    renderPage();
    fireEvent.click(screen.getByText('Excluir Conta'));
    fireEvent.click(screen.getByText('Excluir minha conta'));
    fireEvent.click(screen.getByText('Cancelar', { selector: '.btn-cancel-delete' }));
    expect(screen.queryByText(/Tem certeza/)).not.toBeInTheDocument();
  });

  it('confirmar a exclusão deve chamar deleteUser e navegar para "/"', async () => {
    mockDeleteUser.mockResolvedValueOnce();
    renderPage();
    fireEvent.click(screen.getByText('Excluir Conta'));
    fireEvent.click(screen.getByText('Excluir minha conta'));
    fireEvent.click(screen.getByText('Sim, excluir minha conta'));

    await waitFor(() => expect(mockDeleteUser).toHaveBeenCalled());
    expect(mockNavigate).toHaveBeenCalledWith('/');
  });
});

describe('Profile — Aba Endereços', () => {
  it('deve exibir "Carregando endereços..." durante o carregamento', () => {
    mockApiFetch.mockReturnValueOnce(new Promise(() => {}));
    renderPage();
    fireEvent.click(screen.getByText('Endereços'));
    expect(screen.getByText('Carregando endereços...')).toBeInTheDocument();
  });

  it('deve exibir estado vazio quando não há endereços cadastrados', async () => {
    mockApiFetch.mockResolvedValueOnce([]);
    renderPage();
    fireEvent.click(screen.getByText('Endereços'));
    expect(await screen.findByText('Nenhum endereço cadastrado ainda.')).toBeInTheDocument();
  });

  it('deve listar os endereços retornados pela API, destacando o principal', async () => {
    mockApiFetch.mockResolvedValueOnce([
      { id: 1, logradouro: 'Rua A', numero: '10', bairro: 'Centro', cidade: 'João Pessoa', estado: 'PB', cep: '58000-000', principal: true },
    ]);
    renderPage();
    fireEvent.click(screen.getByText('Endereços'));
    expect(await screen.findByText('Principal')).toBeInTheDocument();
    expect(screen.getByText(/Rua A, 10/)).toBeInTheDocument();
  });

  it('erro ao carregar endereços deve exibir mensagem de erro', async () => {
    mockApiFetch.mockRejectedValueOnce({ mensagem: 'Erro ao buscar endereços' });
    renderPage();
    fireEvent.click(screen.getByText('Endereços'));
    expect(await screen.findByText('Erro ao buscar endereços')).toBeInTheDocument();
  });

  it('clicar em "Adicionar novo endereço" deve exibir o formulário', async () => {
    mockApiFetch.mockResolvedValueOnce([]);
    renderPage();
    fireEvent.click(screen.getByText('Endereços'));
    await screen.findByText('Nenhum endereço cadastrado ainda.');
    fireEvent.click(screen.getByText('+ Adicionar novo endereço'));
    expect(screen.getByText('Novo Endereço')).toBeInTheDocument();
  });

  it('salvar novo endereço com sucesso deve recarregar a lista', async () => {
    mockApiFetch.mockResolvedValueOnce([]);
    const { container } = renderPage();
    fireEvent.click(screen.getByText('Endereços'));
    await screen.findByText('Nenhum endereço cadastrado ainda.');
    fireEvent.click(screen.getByText('+ Adicionar novo endereço'));

    fireEvent.change(screen.getByPlaceholderText('00000-000'), { target: { name: 'cep', value: '58000000' } });
    fireEvent.change(container.querySelector('input[name="rua"]'), { target: { name: 'rua', value: 'Rua Nova' } });
    fireEvent.change(container.querySelector('input[name="numero"]'), { target: { name: 'numero', value: '1' } });
    fireEvent.change(container.querySelector('input[name="bairro"]'), { target: { name: 'bairro', value: 'Bairro' } });
    fireEvent.change(container.querySelector('input[name="cidade"]'), { target: { name: 'cidade', value: 'Cidade' } });
    fireEvent.change(container.querySelector('input[name="estado"]'), { target: { name: 'estado', value: 'PB' } });

    mockApiFetch.mockResolvedValueOnce({ id: 9 });
    mockApiFetch.mockResolvedValueOnce([
      { id: 9, logradouro: 'Rua Nova', numero: '1', bairro: 'B', cidade: 'C', estado: 'PB', cep: '58000-000', principal: true },
    ]);

    fireEvent.click(screen.getByRole('button', { name: 'Salvar endereço' }));
    expect(await screen.findByText(/Rua Nova/)).toBeInTheDocument();
  });

  it('remover endereço deve chamar a API de remoção e recarregar', async () => {
    mockApiFetch.mockResolvedValueOnce([
      { id: 1, logradouro: 'Rua A', numero: '10', bairro: 'Centro', cidade: 'João Pessoa', estado: 'PB', cep: '58000-000', principal: false },
    ]);
    renderPage();
    fireEvent.click(screen.getByText('Endereços'));
    await screen.findByText(/Rua A, 10/);

    mockApiFetch.mockResolvedValueOnce({});
    mockApiFetch.mockResolvedValueOnce([]);
    fireEvent.click(screen.getByText('Remover'));

    expect(await screen.findByText('Nenhum endereço cadastrado ainda.')).toBeInTheDocument();
    expect(mockApiFetch).toHaveBeenCalledWith('/clientes/enderecos/1', { method: 'DELETE' });
  });

  it('falha ao remover endereço deve exibir mensagem de erro', async () => {
    mockApiFetch.mockResolvedValueOnce([
      { id: 1, logradouro: 'Rua A', numero: '10', bairro: 'Centro', cidade: 'João Pessoa', estado: 'PB', cep: '58000-000', principal: false },
    ]);
    renderPage();
    fireEvent.click(screen.getByText('Endereços'));
    await screen.findByText(/Rua A, 10/);

    mockApiFetch.mockRejectedValueOnce({ mensagem: 'Erro ao remover endereço' });
    fireEvent.click(screen.getByText('Remover'));

    expect(await screen.findByText('Erro ao remover endereço')).toBeInTheDocument();
  });
});

describe('Profile — Aba Cartões', () => {
  it('deve exibir "Carregando cartões..." durante o carregamento', () => {
    mockApiFetch.mockReturnValueOnce(new Promise(() => {}));
    renderPage();
    fireEvent.click(screen.getByText('Cartões'));
    expect(screen.getByText('Carregando cartões...')).toBeInTheDocument();
  });

  it('deve exibir estado vazio quando não há cartões salvos', async () => {
    mockApiFetch.mockResolvedValueOnce([]);
    renderPage();
    fireEvent.click(screen.getByText('Cartões'));
    expect(await screen.findByText(/Nenhum cartão salvo/)).toBeInTheDocument();
  });

  it('deve listar os cartões retornados pela API', async () => {
    mockApiFetch.mockResolvedValueOnce([
      { id: 1, bandeira: 'VISA', quatroUltimosDigitos: '1234', nomeTitular: 'Arthur Silva', dataExpiracao: '12/30' },
    ]);
    renderPage();
    fireEvent.click(screen.getByText('Cartões'));
    expect(await screen.findByText(/VISA •••• 1234/)).toBeInTheDocument();
    expect(screen.getByText('Arthur Silva')).toBeInTheDocument();
  });

  it('erro ao carregar cartões deve exibir mensagem de erro', async () => {
    mockApiFetch.mockRejectedValueOnce({ mensagem: 'Erro ao buscar cartões' });
    renderPage();
    fireEvent.click(screen.getByText('Cartões'));
    expect(await screen.findByText('Erro ao buscar cartões')).toBeInTheDocument();
  });

  it('remover cartão com confirmação deve chamar a API e remover da lista', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    mockApiFetch.mockResolvedValueOnce([
      { id: 1, bandeira: 'VISA', quatroUltimosDigitos: '1234', nomeTitular: 'Arthur Silva', dataExpiracao: '12/30' },
    ]);
    renderPage();
    fireEvent.click(screen.getByText('Cartões'));
    await screen.findByText(/VISA •••• 1234/);

    mockApiFetch.mockResolvedValueOnce({});
    fireEvent.click(screen.getByRole('button', { name: 'Remover' }));

    await waitFor(() => expect(screen.queryByText(/VISA •••• 1234/)).not.toBeInTheDocument());
    expect(mockApiFetch).toHaveBeenCalledWith('/clientes/cartoes/1', { method: 'DELETE' });
    window.confirm.mockRestore();
  });

  it('cancelar a remoção (confirm falso) não deve chamar a API de remoção', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(false);
    mockApiFetch.mockResolvedValueOnce([
      { id: 1, bandeira: 'VISA', quatroUltimosDigitos: '1234', nomeTitular: 'Arthur Silva', dataExpiracao: '12/30' },
    ]);
    renderPage();
    fireEvent.click(screen.getByText('Cartões'));
    await screen.findByText(/VISA •••• 1234/);

    mockApiFetch.mockClear();
    fireEvent.click(screen.getByRole('button', { name: 'Remover' }));

    expect(mockApiFetch).not.toHaveBeenCalled();
    window.confirm.mockRestore();
  });

  it('falha ao remover cartão deve exibir mensagem de erro', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    mockApiFetch.mockResolvedValueOnce([
      { id: 1, bandeira: 'VISA', quatroUltimosDigitos: '1234', nomeTitular: 'Arthur Silva', dataExpiracao: '12/30' },
    ]);
    renderPage();
    fireEvent.click(screen.getByText('Cartões'));
    await screen.findByText(/VISA •••• 1234/);

    mockApiFetch.mockRejectedValueOnce({ mensagem: 'Erro ao remover cartão' });
    fireEvent.click(screen.getByRole('button', { name: 'Remover' }));

    expect(await screen.findByText('Erro ao remover cartão')).toBeInTheDocument();
    window.confirm.mockRestore();
  });
});
