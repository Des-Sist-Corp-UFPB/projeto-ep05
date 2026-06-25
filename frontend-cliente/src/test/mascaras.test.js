import { describe, it, expect } from 'vitest';
import { formatCardNumber, formatExpiry, formatCVV, formatName } from '../utils/validarCartao/mascara';
import { formatCep } from '../utils/validarEndereco/masksAddress';

describe('formatCardNumber', () => {
  it('deve agrupar dígitos em blocos de 4', () => {
    expect(formatCardNumber('1234567812345678')).toBe('1234 5678 1234 5678');
  });

  it('deve remover caracteres não numéricos', () => {
    expect(formatCardNumber('1234-5678-1234-5678')).toBe('1234 5678 1234 5678');
  });

  it('deve truncar em 16 dígitos', () => {
    expect(formatCardNumber('12345678901234567890')).toBe('1234 5678 9012 3456');
  });

  it('string vazia deve retornar vazio', () => {
    expect(formatCardNumber('')).toBe('');
  });
});

describe('formatExpiry', () => {
  it('deve inserir "/" após o mês', () => {
    expect(formatExpiry('1225')).toBe('12/25');
  });

  it('com 2 dígitos ou menos não deve inserir barra', () => {
    expect(formatExpiry('12')).toBe('12');
  });

  it('deve remover caracteres não numéricos', () => {
    expect(formatExpiry('12/25')).toBe('12/25');
  });

  it('deve truncar em 4 dígitos', () => {
    expect(formatExpiry('122599')).toBe('12/25');
  });

  it('string vazia deve retornar vazio', () => {
    expect(formatExpiry('')).toBe('');
  });
});

describe('formatCVV', () => {
  it('deve remover caracteres não numéricos e truncar em 3', () => {
    expect(formatCVV('1a2b3c4d')).toBe('123');
  });

  it('string vazia deve retornar vazio', () => {
    expect(formatCVV('')).toBe('');
  });
});

describe('formatName', () => {
  it('deve remover números e símbolos, mantendo letras e espaços', () => {
    expect(formatName('Jo3o D4 S1lv4!!')).toBe('Joo D Slv');
  });

  it('deve manter nome já válido inalterado', () => {
    expect(formatName('Joao da Silva')).toBe('Joao da Silva');
  });
});

describe('formatCep', () => {
  it('deve inserir hífen após o 5º dígito', () => {
    expect(formatCep('58000000')).toBe('58000-000');
  });

  it('deve remover caracteres não numéricos', () => {
    expect(formatCep('58.000-000')).toBe('58000-000');
  });

  it('deve truncar em 8 dígitos', () => {
    expect(formatCep('580000001234')).toBe('58000-000');
  });

  it('com 5 dígitos ou menos não deve inserir hífen', () => {
    expect(formatCep('580')).toBe('580');
  });

  it('string vazia deve retornar vazio', () => {
    expect(formatCep('')).toBe('');
  });
});
