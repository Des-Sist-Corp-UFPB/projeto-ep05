import { describe, it, expect } from 'vitest';
import {
  validarFormularioAddress,
  validarCampoAddress,
} from '../utils/validarEndereco/addressValidators';

const dadosValidos = {
  cep: '58000-000',
  rua: 'Rua das Flores',
  numero: '123',
  bairro: 'Centro',
  cidade: 'João Pessoa',
  estado: 'PB',
};

describe('validarFormularioAddress', () => {
  it('deve retornar objeto vazio para dados válidos', () => {
    expect(validarFormularioAddress(dadosValidos)).toEqual({});
  });

  it('deve retornar erro quando CEP está vazio', () => {
    const erros = validarFormularioAddress({ ...dadosValidos, cep: '' });
    expect(erros.cep).toBe('CEP obrigatório');
  });

  it('deve retornar erro quando CEP tem dígitos insuficientes', () => {
    const erros = validarFormularioAddress({ ...dadosValidos, cep: '1234-00' });
    expect(erros.cep).toBe('CEP inválido');
  });

  it('deve aceitar CEP com 8 dígitos numéricos', () => {
    const erros = validarFormularioAddress({ ...dadosValidos, cep: '58000000' });
    expect(erros.cep).toBeUndefined();
  });

  it('deve retornar erro quando rua está vazia', () => {
    const erros = validarFormularioAddress({ ...dadosValidos, rua: '' });
    expect(erros.rua).toBe('Rua obrigatória');
  });

  it('deve retornar erro quando número está vazio', () => {
    const erros = validarFormularioAddress({ ...dadosValidos, numero: '' });
    expect(erros.numero).toBe('Número obrigatório');
  });

  it('deve retornar erro quando bairro está vazio', () => {
    const erros = validarFormularioAddress({ ...dadosValidos, bairro: '' });
    expect(erros.bairro).toBe('Bairro obrigatório');
  });

  it('deve retornar erro quando cidade está vazia', () => {
    const erros = validarFormularioAddress({ ...dadosValidos, cidade: '' });
    expect(erros.cidade).toBeDefined();
  });

  it('deve retornar erro quando estado está vazio', () => {
    const erros = validarFormularioAddress({ ...dadosValidos, estado: '' });
    expect(erros.estado).toBeDefined();
  });
});

describe('validarCampoAddress', () => {
  it('CEP válido deve retornar undefined', () => {
    expect(validarCampoAddress('cep', { cep: '58000-000' })).toBeUndefined();
  });

  it('CEP inválido deve retornar mensagem', () => {
    expect(validarCampoAddress('cep', { cep: '' })).toBe('CEP obrigatório');
  });

  it('campo desconhecido deve retornar undefined', () => {
    expect(validarCampoAddress('inexistente', {})).toBeUndefined();
  });
});
