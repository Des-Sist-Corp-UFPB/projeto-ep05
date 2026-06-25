import { describe, it, expect } from 'vitest';
import { validarFormularioCard, validarCampoCard } from '../utils/validarCartao/validarCartao';

const dadosValidos = {
  cardNumber: '4111 1111 1111 1111',
  expiry: '12/26',
  cvv: '123',
  name: 'João Silva',
};

// ── validarFormularioCard ─────────────────────────────────────────────────────

describe('validarFormularioCard', () => {
  it('deve retornar objeto vazio para dados válidos', () => {
    expect(validarFormularioCard(dadosValidos)).toEqual({});
  });

  it('deve retornar erro quando número do cartão está vazio', () => {
    const erros = validarFormularioCard({ ...dadosValidos, cardNumber: '' });
    expect(erros.cardNumber).toBe('Número do cartão é obrigatório');
  });

  it('deve retornar erro quando cartão tem menos de 16 dígitos', () => {
    const erros = validarFormularioCard({ ...dadosValidos, cardNumber: '1234 5678 9012' });
    expect(erros.cardNumber).toBe('Cartão deve ter 16 dígitos');
  });

  it('deve aceitar número sem espaços com 16 dígitos', () => {
    const erros = validarFormularioCard({ ...dadosValidos, cardNumber: '4111111111111111' });
    expect(erros.cardNumber).toBeUndefined();
  });

  it('deve retornar erro quando validade está vazia', () => {
    const erros = validarFormularioCard({ ...dadosValidos, expiry: '' });
    expect(erros.expiry).toBe('Validade é obrigatória');
  });

  it('deve retornar erro para formato de validade inválido', () => {
    const erros = validarFormularioCard({ ...dadosValidos, expiry: '1226' });
    expect(erros.expiry).toBe('Formato inválido (MM/AA)');
  });

  it('deve retornar erro quando mês é 0', () => {
    const erros = validarFormularioCard({ ...dadosValidos, expiry: '00/26' });
    expect(erros.expiry).toBe('Mês inválido');
  });

  it('deve retornar erro quando mês é 13', () => {
    const erros = validarFormularioCard({ ...dadosValidos, expiry: '13/26' });
    expect(erros.expiry).toBe('Mês inválido');
  });

  it('deve retornar erro quando CVV está vazio', () => {
    const erros = validarFormularioCard({ ...dadosValidos, cvv: '' });
    expect(erros.cvv).toBe('CVV obrigatório');
  });

  it('deve retornar erro quando CVV não tem 3 dígitos', () => {
    const erros = validarFormularioCard({ ...dadosValidos, cvv: '12' });
    expect(erros.cvv).toBe('CVV inválido');
  });

  it('deve retornar erro quando nome do titular está vazio', () => {
    const erros = validarFormularioCard({ ...dadosValidos, name: '' });
    expect(erros.name).toBe('Nome é obrigatório');
  });

  it('deve retornar erro quando nome tem menos de 3 caracteres', () => {
    const erros = validarFormularioCard({ ...dadosValidos, name: 'Jo' });
    expect(erros.name).toBe('Nome muito curto');
  });
});

// ── validarCampoCard ──────────────────────────────────────────────────────────

describe('validarCampoCard', () => {
  it('número de cartão válido deve retornar undefined', () => {
    expect(validarCampoCard('cardNumber', { cardNumber: '4111 1111 1111 1111' })).toBeUndefined();
  });

  it('número inválido deve retornar mensagem', () => {
    expect(validarCampoCard('cardNumber', { cardNumber: '' }))
        .toBe('Número do cartão é obrigatório');
  });

  it('campo desconhecido deve retornar undefined', () => {
    expect(validarCampoCard('inexistente', {})).toBeUndefined();
  });
});
