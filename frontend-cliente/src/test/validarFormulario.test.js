import { describe, it, expect } from 'vitest';
import { validarFormulario, validarCampo } from '../utils/validarFormulario';

// ── Dados auxiliares ──────────────────────────────────────────────────────────

const dadosValidos = {
  nome: 'Maria Silva',
  email: 'maria@email.com',
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

  it('deve retornar erro quando nome tem menos de 3 letras', () => {
    const erros = validarFormulario({ ...dadosValidos, nome: 'Ab' });
    expect(erros.nome).toBe('Nome precisa ter pelo menos 3 letras');
  });

  it('deve retornar erro quando email está vazio', () => {
    const erros = validarFormulario({ ...dadosValidos, email: '' });
    expect(erros.email).toBe('Email é obrigatório');
  });

  it('deve retornar erro para email sem arroba', () => {
    const erros = validarFormulario({ ...dadosValidos, email: 'naoeemail.com' });
    expect(erros.email).toBe('Digite um email válido');
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

  it('deve retornar erro quando senha não tem caractere especial', () => {
    const erros = validarFormulario({ ...dadosValidos, senha: 'Senha1234' });
    expect(erros.senha).toBe('A senha precisa ter um caractere especial');
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
