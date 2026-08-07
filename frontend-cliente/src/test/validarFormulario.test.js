import { describe, it, expect } from 'vitest';
import { validarFormulario, validarCampo } from '../utils/validarFormulario';

// ── Dados auxiliares ──────────────────────────────────────────────────────────

const dadosValidos = {
  nome: 'Maria Silva',
  sobrenome: 'Souza',
  email: 'maria@email.com',
  cpf: '123.456.789-00',
  telefone: '(11) 91234-5678',
  dataNascimento: '1990-01-01',
  senha: 'Senha@123',
  confirmarSenha: 'Senha@123',
};

// ── validarFormulario ─────────────────────────────────────────────────────────

describe('validarFormulario', () => {
  it('deve retornar objeto vazio para dados válidos', () => {
    expect(validarFormulario(dadosValidos)).toEqual({});
  });

  it('deve retornar erro quando nome está vazio', () => {
    const erros = validarFormulario({ ...dadosValidos, nome: '' });
    expect(erros.nome).toBe('Nome é obrigatório');
  });

  it('deve retornar erro quando nome tem menos de 2 letras', () => {
    const erros = validarFormulario({ ...dadosValidos, nome: 'A' });
    expect(erros.nome).toBe('Nome precisa ter pelo menos 2 letras');
  });

  it('deve retornar erro quando sobrenome tem menos de 2 letras', () => {
    const erros = validarFormulario({ ...dadosValidos, sobrenome: 'A' });
    expect(erros.sobrenome).toBe('Sobrenome precisa ter pelo menos 2 letras');
  });

  it('deve retornar erro quando CPF está em formato inválido', () => {
    const erros = validarFormulario({ ...dadosValidos, cpf: '12345678900' });
    expect(erros.cpf).toBe('CPF inválido. Use o formato 000.000.000-00');
  });

  it('deve retornar erro quando telefone está em formato inválido', () => {
    const erros = validarFormulario({ ...dadosValidos, telefone: '11912345678' });
    expect(erros.telefone).toBe('Telefone inválido. Use (00) 00000-0000');
  });

  it('deve retornar erro quando a data de nascimento está no futuro', () => {
    const anoFuturo = new Date().getFullYear() + 1;
    const erros = validarFormulario({ ...dadosValidos, dataNascimento: `${anoFuturo}-01-01` });
    expect(erros.dataNascimento).toBe('A data de nascimento deve ser no passado');
  });

  it('deve retornar erro quando email está vazio', () => {
    const erros = validarFormulario({ ...dadosValidos, email: '' });
    expect(erros.email).toBe('E-mail é obrigatório');
  });

  it('deve retornar erro para email sem arroba', () => {
    const erros = validarFormulario({ ...dadosValidos, email: 'naoeemail.com' });
    expect(erros.email).toBe('Digite um e-mail válido');
  });

  it('deve retornar erro quando senha está vazia', () => {
    const erros = validarFormulario({ ...dadosValidos, senha: '', confirmarSenha: '' });
    expect(erros.senha).toBe('Senha é obrigatória');
  });

  it('deve retornar erro quando senha tem menos de 8 caracteres', () => {
    const erros = validarFormulario({ ...dadosValidos, senha: 'Ab@1' });
    expect(erros.senha).toBe('A senha precisa ter pelo menos 8 caracteres');
  });

  it('deve retornar erro quando senha não tem letra maiúscula', () => {
    const erros = validarFormulario({ ...dadosValidos, senha: 'senha@123' });
    expect(erros.senha).toBe('A senha precisa ter uma letra maiúscula');
  });

  it('deve retornar erro quando senha não tem letra minúscula', () => {
    const erros = validarFormulario({ ...dadosValidos, senha: 'SENHA@123' });
    expect(erros.senha).toBe('A senha precisa ter uma letra minúscula');
  });

  it('deve retornar erro quando senha não tem número', () => {
    const erros = validarFormulario({ ...dadosValidos, senha: 'Senha@abc' });
    expect(erros.senha).toBe('A senha precisa ter um número');
  });

  // NOTA: o código-fonte não valida caractere especial na senha — não é um
  // problema de texto, é uma regra que simplesmente não existe ainda.
  // O teste abaixo documenta o comportamento atual (sem erro). Se a regra de
  // negócio for exigida, isso precisa ser implementado em validarFormulario.js
  // como mudança de funcionalidade, não como ajuste deste arquivo de teste.
  it('atualmente NÃO valida caractere especial na senha (comportamento real do código)', () => {
    const erros = validarFormulario({ ...dadosValidos, senha: 'Senha1234', confirmarSenha: 'Senha1234' });
    expect(erros.senha).toBeUndefined();
  });

  it('deve retornar erro quando confirmação de senha está vazia', () => {
    const erros = validarFormulario({ ...dadosValidos, confirmarSenha: '' });
    expect(erros.confirmarSenha).toBe('Confirme sua senha');
  });

  it('deve retornar erro quando senhas não coincidem', () => {
    const erros = validarFormulario({ ...dadosValidos, confirmarSenha: 'Outra@123' });
    expect(erros.confirmarSenha).toBe('As senhas não coincidem');
  });
});

// ── validarCampo ──────────────────────────────────────────────────────────────

describe('validarCampo', () => {
  it('nome válido deve retornar undefined (sem erro)', () => {
    expect(validarCampo('nome', { nome: 'João' })).toBeUndefined();
  });

  it('nome inválido deve retornar mensagem de erro', () => {
    expect(validarCampo('nome', { nome: '' })).toBe('Nome é obrigatório');
  });

  it('email válido deve retornar undefined', () => {
    expect(validarCampo('email', { email: 'a@b.com' })).toBeUndefined();
  });

  it('campo desconhecido deve retornar undefined sem errar', () => {
    expect(validarCampo('campoInexistente', {})).toBeUndefined();
  });
});
