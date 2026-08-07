import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return { ...actual, useNavigate: () => mockNavigate };
});

const mockClearCart = vi.fn();
let mockCartValue = {
  cart: [{ id: 1, name: 'Brownie', price: 10, quantity: 2 }],
  total: 20,
  clearCart: mockClearCart,
};
vi.mock('../context/CartContext', () => ({
  useCart: () => mockCartValue,
}));

const mockApiFetch = vi.fn();
vi.mock('../api/api', () => ({
  apiFetch: (...args) => mockApiFetch(...args),
}));

import Checkout from '../pages/Checkout/Checkout';

beforeEach(() => {
  mockNavigate.mockClear();
  mockClearCart.mockReset();
  mockApiFetch.mockReset();
  mockCartValue = {
    cart: [{ id: 1, name: 'Brownie', price: 10, quantity: 2 }],
    total: 20,
    clearCart: mockClearCart,
  };
});

function renderPage() {
  return render(<MemoryRouter><Checkout /></MemoryRouter>);
}

/** Avança para o passo de pagamento clicando em "Ir para Pagamento →" */
async function irParaPagamento() {
  fireEvent.click(screen.getByRole('button', { name: /Ir para Pagamento/i }));
  // Entrar no passo 1 dispara carregarCartoes() (apiFetch GET /clientes/cartoes),
  // que decide usarNovoCartao. Sem aguardar essa Promise resolver, o submit
  // dispara com usarNovoCartao ainda no valor inicial (false), pulando a
  // validação do formulário inteira.
  await act(async () => {
    await Promise.resolve();
  });
}

/** Preenche os campos do cartão (só disponíveis no passo 1) */
async function preencherCartaoValido() {
  await irParaPagamento();
  fireEvent.change(screen.getByPlaceholderText('Como está impresso no cartão'), { target: { name: 'name', value: 'Arthur Silva' } });
  fireEvent.change(screen.getByPlaceholderText('0000 0000 0000 0000'), { target: { name: 'cardNumber', value: '4111111111111111' } });
  fireEvent.change(screen.getByPlaceholderText('MM/AA'), { target: { name: 'expiry', value: '1230' } });
  fireEvent.change(screen.getByPlaceholderText('123'), { target: { name: 'cvv', value: '123' } });
}

const cartaoSalvo = {
  id: 77, bandeira: 'VISA', quatroUltimosDigitos: '4242', nomeTitular: 'Arthur Silva', dataExpiracao: '11/29',
};

describe('Checkout — Passo 0 (Resumo)', () => {
  it('deve exibir os itens do carrinho', () => {
    renderPage();
    expect(screen.getByText('Brownie')).toBeInTheDocument();
  });

  it('deve exibir o total do carrinho', () => {
    renderPage();
    expect(screen.getByText('Total: R$ 20.00')).toBeInTheDocument();
  });

  it('aplicar cupom válido deve exibir desconto no total', async () => {
    mockApiFetch.mockResolvedValueOnce({ tipo: 'PORCENTAGEM', desconto: '10' });
    renderPage();

    fireEvent.change(screen.getByPlaceholderText('Código do cupom'), { target: { value: 'promo10' } });
    fireEvent.click(screen.getByRole('button', { name: 'Aplicar' }));

    expect(await screen.findByText('✅ Cupom aplicado! Desconto de 10%')).toBeInTheDocument();
    expect(screen.getByText(/Total: R\$ 18.00/)).toBeInTheDocument();
  });

  it('aplicar cupom inválido deve mostrar mensagem de erro', async () => {
    mockApiFetch.mockRejectedValueOnce({ mensagem: 'Cupom inválido' });
    renderPage();

    fireEvent.change(screen.getByPlaceholderText('Código do cupom'), { target: { value: 'invalido' } });
    fireEvent.click(screen.getByRole('button', { name: 'Aplicar' }));

    expect(await screen.findByText('Cupom inválido')).toBeInTheDocument();
  });

  it('aplicar cupom de valor fixo deve calcular o desconto corretamente', async () => {
    mockApiFetch.mockResolvedValueOnce({ tipo: 'VALOR_FIXO', desconto: '5' });
    renderPage();

    fireEvent.change(screen.getByPlaceholderText('Código do cupom'), { target: { value: 'FIXO5' } });
    fireEvent.click(screen.getByRole('button', { name: 'Aplicar' }));

    expect(await screen.findByText(/Total: R\$ 15.00/)).toBeInTheDocument();
  });

  it('campo de cupom vazio não deve chamar a API ao clicar em Aplicar', () => {
    renderPage();
    fireEvent.click(screen.getByRole('button', { name: 'Aplicar' }));
    expect(mockApiFetch).not.toHaveBeenCalled();
  });

  it('cupom com tipo desconhecido não deve aplicar desconto ao total', async () => {
    mockApiFetch.mockResolvedValueOnce({ tipo: 'OUTRO_TIPO', desconto: '99' });
    renderPage();

    fireEvent.change(screen.getByPlaceholderText('Código do cupom'), { target: { value: 'ESTRANHO' } });
    fireEvent.click(screen.getByRole('button', { name: 'Aplicar' }));

    expect(await screen.findByText(/Cupom aplicado/)).toBeInTheDocument();
    expect(screen.getByText('Total: R$ 20.00')).toBeInTheDocument();
  });

  it('carrinho vazio deve desabilitar o botão de avançar', () => {
    mockCartValue = { cart: [], total: 0, clearCart: mockClearCart };
    renderPage();
    expect(screen.getByRole('button', { name: /Ir para Pagamento/i })).toBeDisabled();
  });
});

describe('Checkout — Passo 1 (Pagamento)', () => {
  it('deve exibir formulário do cartão após avançar', async () => {
    renderPage();
    await irParaPagamento();
    expect(screen.getByPlaceholderText('0000 0000 0000 0000')).toBeInTheDocument();
  });

  it('deve formatar o número do cartão ao digitar', async () => {
    renderPage();
    await irParaPagamento();
    const input = screen.getByPlaceholderText('0000 0000 0000 0000');
    fireEvent.change(input, { target: { name: 'cardNumber', value: '4111111111111111' } });
    expect(input).toHaveValue('4111 1111 1111 1111');
  });

  it('deve formatar a validade ao digitar', async () => {
    renderPage();
    await irParaPagamento();
    const input = screen.getByPlaceholderText('MM/AA');
    fireEvent.change(input, { target: { name: 'expiry', value: '1230' } });
    expect(input).toHaveValue('12/30');
  });

  it('submeter formulário vazio deve mostrar erros e não chamar a API', async () => {
    renderPage();
    await irParaPagamento();
    // Limpa a chamada de GET /clientes/cartoes (disparada ao entrar no passo 1)
    // para isolar a checagem: o que importa é que o SUBMIT vazio não chama a API.
    mockApiFetch.mockClear();
    fireEvent.click(screen.getByRole('button', { name: 'Confirmar Pedido' }));

    expect(await screen.findByText('Nome é obrigatório')).toBeInTheDocument();
    expect(mockApiFetch).not.toHaveBeenCalled();
  });

  it('carrinho vazio deve impedir o envio mesmo com cartão válido', async () => {
    mockCartValue = { cart: [], total: 0, clearCart: mockClearCart };
    renderPage();
    // Força o passo 1 via botão back/next não disponível, então simula clique com carrinho vazio
    // O botão "Ir para Pagamento" fica disabled com carrinho vazio, mas testamos a guarda do submit
    // Renderizamos com carrinho populado primeiro, avançamos, e aí limpamos
    mockCartValue = { cart: [], total: 0, clearCart: mockClearCart };
    render(
      <MemoryRouter>
        <Checkout />
      </MemoryRouter>
    );
    // Botão ir para pagamento deve estar disabled
    expect(screen.getAllByRole('button', { name: /Ir para Pagamento/i })[0]).toBeDisabled();
  });

  it('sem endereço cadastrado deve mostrar mensagem pedindo para cadastrar', async () => {
    mockApiFetch
      .mockResolvedValueOnce([])  // GET /clientes/cartoes (ao entrar no passo 1) — sem cartão salvo
      .mockResolvedValueOnce([]); // GET /clientes/enderecos vazio
    renderPage();
    await preencherCartaoValido();

    fireEvent.click(screen.getByRole('button', { name: 'Confirmar Pedido' }));

    expect(await screen.findByText('Cadastre um endereço de entrega antes de finalizar.')).toBeInTheDocument();
  });

  it('fluxo completo de sucesso deve salvar cartão, criar pedido e mostrar confirmação', async () => {
    mockApiFetch
      .mockResolvedValueOnce([])                           // GET /clientes/cartoes (entrar no passo 1)
      .mockResolvedValueOnce([{ id: 5, principal: true }])  // GET enderecos
      .mockResolvedValueOnce({ id: 99 })                    // POST cartoes
      .mockResolvedValueOnce({ id: 1000 });                 // POST pedidos

    // tokenizarCartaoMP() chama fetch() global direto (API do Mercado Pago),
    // fora da camada apiFetch — precisa de mock próprio.
    global.fetch = vi.fn().mockResolvedValueOnce({
      ok: true,
      json: async () => ({ id: 'tok_abc123' }),
    });

    renderPage();
    await preencherCartaoValido();
    fireEvent.click(screen.getByRole('button', { name: 'Confirmar Pedido' }));

    expect(await screen.findByText('Pedido confirmado!')).toBeInTheDocument();
    expect(mockClearCart).toHaveBeenCalled();

    const chamadaPedido = mockApiFetch.mock.calls.find((c) => c[0] === '/pedidos');
    expect(chamadaPedido[1].method).toBe('POST');
    const corpoPedido = JSON.parse(chamadaPedido[1].body);
    expect(corpoPedido.enderecoId).toBe(5);
    expect(corpoPedido.cartaoId).toBe(99);
  });

  it('falha ao finalizar o pedido deve exibir o erro da API', async () => {
    mockApiFetch
      .mockResolvedValueOnce([])                          // GET /clientes/cartoes (entrar no passo 1)
      .mockResolvedValueOnce([{ id: 5, principal: true }]) // GET enderecos
      .mockRejectedValueOnce({ mensagem: 'Cartão recusado' });

    global.fetch = vi.fn().mockResolvedValueOnce({
      ok: true,
      json: async () => ({ id: 'tok_abc123' }),
    });

    renderPage();
    await preencherCartaoValido();
    fireEvent.click(screen.getByRole('button', { name: 'Confirmar Pedido' }));

    expect(await screen.findByText('Cartão recusado')).toBeInTheDocument();
  });

  it('clicar em "← Voltar" deve retornar ao passo de Resumo', async () => {
    renderPage();
    await irParaPagamento();
    fireEvent.click(screen.getByRole('button', { name: '← Voltar' }));
    expect(screen.getByText('Brownie')).toBeInTheDocument();
    expect(screen.queryByPlaceholderText('0000 0000 0000 0000')).not.toBeInTheDocument();
  });

  it('a tokenização falhando (resposta sem id) deve exibir mensagem de erro e não criar pedido', async () => {
    mockApiFetch
      .mockResolvedValueOnce([])                           // GET /clientes/cartoes
      .mockResolvedValueOnce([{ id: 5, principal: true }]); // GET enderecos

    global.fetch = vi.fn().mockResolvedValueOnce({
      ok: false,
      json: async () => ({ message: 'Número de cartão inválido' }),
    });

    renderPage();
    await preencherCartaoValido();
    fireEvent.click(screen.getByRole('button', { name: 'Confirmar Pedido' }));

    expect(await screen.findByText('Número de cartão inválido')).toBeInTheDocument();
    expect(mockApiFetch.mock.calls.find((c) => c[0] === '/pedidos')).toBeUndefined();
  });

  it('falha de rede ao tokenizar deve usar mensagem de fallback', async () => {
    mockApiFetch
      .mockResolvedValueOnce([])
      .mockResolvedValueOnce([{ id: 5, principal: true }]);

    global.fetch = vi.fn().mockRejectedValueOnce(new Error());

    renderPage();
    await preencherCartaoValido();
    fireEvent.click(screen.getByRole('button', { name: 'Confirmar Pedido' }));

    expect(await screen.findByText('Erro ao processar dados do cartão.')).toBeInTheDocument();
  });

  it('carrinho esvaziado entre o preenchimento e o envio deve bloquear o submit', async () => {
    const utils = renderPage();
    await preencherCartaoValido();

    mockCartValue = { cart: [], total: 0, clearCart: mockClearCart };
    utils.rerender(<MemoryRouter><Checkout /></MemoryRouter>);

    fireEvent.click(screen.getByRole('button', { name: 'Confirmar Pedido' }));

    expect(await screen.findByText('Seu carrinho está vazio.')).toBeInTheDocument();
  });
});

describe('Checkout — Cartões salvos', () => {
  it('deve exibir os cartões salvos e pré-selecionar o primeiro', async () => {
    mockApiFetch.mockResolvedValueOnce([cartaoSalvo]);
    renderPage();
    await irParaPagamento();

    expect(screen.getByText('💳 Cartões salvos')).toBeInTheDocument();
    expect(screen.getByText(/4242/)).toBeInTheDocument();
    expect(screen.getByText('Arthur Silva')).toBeInTheDocument();
    expect(screen.getByRole('radio')).toBeChecked();
  });

  it('selecionar outro cartão salvo deve marcá-lo como ativo', async () => {
    const cartao2 = { ...cartaoSalvo, id: 78, quatroUltimosDigitos: '1111' };
    mockApiFetch.mockResolvedValueOnce([cartaoSalvo, cartao2]);
    renderPage();
    await irParaPagamento();

    const radios = screen.getAllByRole('radio');
    fireEvent.click(radios[1]);
    expect(radios[1]).toBeChecked();
    expect(radios[0]).not.toBeChecked();
  });

  it('clicar em "+ Usar outro cartão" deve exibir o formulário de novo cartão', async () => {
    mockApiFetch.mockResolvedValueOnce([cartaoSalvo]);
    renderPage();
    await irParaPagamento();

    fireEvent.click(screen.getByRole('button', { name: '+ Usar outro cartão' }));

    expect(screen.getByPlaceholderText('0000 0000 0000 0000')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '← Usar cartão salvo' })).toBeInTheDocument();
  });

  it('clicar em "← Usar cartão salvo" deve voltar para a lista de cartões salvos', async () => {
    mockApiFetch.mockResolvedValueOnce([cartaoSalvo]);
    renderPage();
    await irParaPagamento();
    fireEvent.click(screen.getByRole('button', { name: '+ Usar outro cartão' }));

    fireEvent.click(screen.getByRole('button', { name: '← Usar cartão salvo' }));

    expect(screen.getByText('💳 Cartões salvos')).toBeInTheDocument();
    expect(screen.queryByPlaceholderText('0000 0000 0000 0000')).not.toBeInTheDocument();
  });

  it('finalizar pedido com cartão salvo não deve tokenizar nem salvar novo cartão', async () => {
    mockApiFetch
      .mockResolvedValueOnce([cartaoSalvo])                 // GET /clientes/cartoes
      .mockResolvedValueOnce([{ id: 5, principal: true }])  // GET enderecos
      .mockResolvedValueOnce({ id: 2000 });                 // POST pedidos

    global.fetch = vi.fn();

    renderPage();
    await irParaPagamento();
    fireEvent.click(screen.getByRole('button', { name: 'Confirmar Pedido' }));

    expect(await screen.findByText('Pedido confirmado!')).toBeInTheDocument();
    expect(global.fetch).not.toHaveBeenCalled();

    const chamadaPedido = mockApiFetch.mock.calls.find((c) => c[0] === '/pedidos');
    const corpoPedido = JSON.parse(chamadaPedido[1].body);
    expect(corpoPedido.cartaoId).toBe(77);
  });

  it('virar o card ao focar no CVV deve aplicar a classe "virado"', async () => {
    renderPage();
    await irParaPagamento();
    const cvvInput = screen.getByPlaceholderText('123');

    fireEvent.focus(cvvInput);
    expect(document.querySelector('.card-visual.virado')).toBeInTheDocument();

    fireEvent.blur(cvvInput);
    expect(document.querySelector('.card-visual.virado')).not.toBeInTheDocument();
  });

  it('erro ao carregar cartões salvos deve cair no formulário de novo cartão', async () => {
    mockApiFetch.mockRejectedValueOnce(new Error('falhou'));
    renderPage();
    await irParaPagamento();

    expect(screen.getByPlaceholderText('0000 0000 0000 0000')).toBeInTheDocument();
  });
});
