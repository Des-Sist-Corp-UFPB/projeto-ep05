import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';

// ── api.js (apiFetch) ────────────────────────────────────────────────────────

describe('apiFetch', () => {
  beforeEach(() => {
    localStorage.clear();
    global.fetch = vi.fn();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('deve montar a URL com BASE_URL e retornar o JSON em caso de sucesso', async () => {
    const { apiFetch, BASE_URL } = await import('../api/api');
    global.fetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      headers: { get: () => '50' },
      json: async () => ({ ok: true }),
    });

    const data = await apiFetch('/produtos');

    expect(global.fetch).toHaveBeenCalledWith(`${BASE_URL}/produtos`, expect.objectContaining({
      headers: expect.objectContaining({ 'Content-Type': 'application/json' }),
    }));
    expect(data).toEqual({ ok: true });
  });

  it('deve incluir o token Authorization quando há usuário salvo', async () => {
    localStorage.setItem('user', JSON.stringify({ token: 'meu-token' }));
    const { apiFetch } = await import('../api/api');
    global.fetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      headers: { get: () => '2' },
      json: async () => ({ ok: true }),
    });

    await apiFetch('/clientes/perfil');

    const headers = global.fetch.mock.calls[0][1].headers;
    expect(headers.Authorization).toBe('Bearer meu-token');
  });

  it('com localStorage corrompido, não deve incluir Authorization', async () => {
    localStorage.setItem('user', '{ invalido');
    const { apiFetch } = await import('../api/api');
    global.fetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      headers: { get: () => '2' },
      json: async () => ({ ok: true }),
    });

    await apiFetch('/produtos');

    const headers = global.fetch.mock.calls[0][1].headers;
    expect(headers.Authorization).toBeUndefined();
  });

  it('resposta 204 deve retornar null sem tentar parsear JSON', async () => {
    const { apiFetch } = await import('../api/api');
    global.fetch.mockResolvedValueOnce({
      ok: true,
      status: 204,
      headers: { get: () => null },
      json: async () => { throw new Error('não deveria ser chamado'); },
    });

    const data = await apiFetch('/clientes/cartoes/1', { method: 'DELETE' });
    expect(data).toBeNull();
  });

  it('resposta com content-length 0 deve retornar null', async () => {
    const { apiFetch } = await import('../api/api');
    global.fetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      headers: { get: (h) => (h === 'content-length' ? '0' : null) },
      json: async () => { throw new Error('não deveria ser chamado'); },
    });

    const data = await apiFetch('/algo');
    expect(data).toBeNull();
  });

  it('erro HTTP com corpo JSON deve lançar { status, mensagem }', async () => {
    const { apiFetch } = await import('../api/api');
    global.fetch.mockResolvedValueOnce({
      ok: false,
      status: 409,
      statusText: 'Conflict',
      json: async () => ({ mensagem: 'E-mail já cadastrado' }),
    });

    await expect(apiFetch('/auth/cadastro', { method: 'POST' }))
      .rejects.toEqual({ status: 409, mensagem: 'E-mail já cadastrado' });
  });

  it('erro HTTP sem corpo JSON válido deve usar statusText', async () => {
    const { apiFetch } = await import('../api/api');
    global.fetch.mockResolvedValueOnce({
      ok: false,
      status: 401,
      statusText: 'Unauthorized',
      json: async () => { throw new Error('não é JSON'); },
    });

    await expect(apiFetch('/algo'))
      .rejects.toEqual({ status: 401, mensagem: 'Unauthorized' });
  });

  it('falha de rede (fetch lança) deve virar erro "Sem conexão com o servidor"', async () => {
    const { apiFetch } = await import('../api/api');
    global.fetch.mockRejectedValueOnce(new TypeError('Failed to fetch'));

    await expect(apiFetch('/produtos'))
      .rejects.toEqual({ status: 0, mensagem: 'Sem conexão com o servidor' });
  });
});

// ── cep.js ────────────────────────────────────────────────────────────────────

describe('buscarCEP', () => {
  beforeEach(() => {
    global.fetch = vi.fn();
  });
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('CEP com menos de 8 dígitos deve retornar null sem chamar fetch', async () => {
    const { buscarCEP } = await import('../api/cep');
    const data = await buscarCEP('123');
    expect(data).toBeNull();
    expect(global.fetch).not.toHaveBeenCalled();
  });

  it('CEP válido deve retornar os dados do ViaCEP', async () => {
    const { buscarCEP } = await import('../api/cep');
    global.fetch.mockResolvedValueOnce({
      json: async () => ({ logradouro: 'Rua X', bairro: 'Centro', localidade: 'João Pessoa', uf: 'PB' }),
    });

    const data = await buscarCEP('58000-000');
    expect(data.logradouro).toBe('Rua X');
    expect(global.fetch).toHaveBeenCalledWith('https://viacep.com.br/ws/58000000/json/');
  });

  it('CEP inexistente (data.erro) deve retornar null', async () => {
    const { buscarCEP } = await import('../api/cep');
    global.fetch.mockResolvedValueOnce({ json: async () => ({ erro: true }) });

    const data = await buscarCEP('99999999');
    expect(data).toBeNull();
  });

  it('falha de rede deve retornar null', async () => {
    const { buscarCEP } = await import('../api/cep');
    global.fetch.mockRejectedValueOnce(new Error('offline'));

    const data = await buscarCEP('58000000');
    expect(data).toBeNull();
  });
});

// ── productApi.js ──────────────────────────────────────────────────────────────

describe('productApi', () => {
  beforeEach(() => {
    localStorage.clear();
    global.fetch = vi.fn();
  });
  afterEach(() => {
    vi.restoreAllMocks();
  });

  function mockFetchOnce(body) {
    global.fetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      headers: { get: () => String(JSON.stringify(body).length) },
      json: async () => body,
    });
  }

  const produtoApi = {
    id: 1, nome: 'Brownie', descricao: 'Chocolate', preco: '12.50',
    categoriaNome: 'Doces', categoriaId: 2, estoque: 5, ativo: true,
    imagensUrls: ['img1.png'], notaMedia: 4.5,
  };

  it('mapProduto deve converter os campos da API para o padrão do frontend', async () => {
    const { mapProduto } = await import('../api/productApi');
    const mapped = mapProduto(produtoApi);
    expect(mapped).toEqual({
      id: 1, name: 'Brownie', description: 'Chocolate', price: 12.5,
      category: 'Doces', categoriaId: 2, stock: 5, ativo: true,
      image: 'img1.png', images: ['img1.png'], rating: 4.5,
    });
  });

  it('mapProduto sem imagens nem nota deve usar valores padrão', async () => {
    const { mapProduto } = await import('../api/productApi');
    const mapped = mapProduto({ id: 2, nome: 'Cookie', preco: '5.00' });
    expect(mapped.image).toBeNull();
    expect(mapped.images).toEqual([]);
    expect(mapped.rating).toBe(0);
    expect(mapped.category).toBe('');
  });

  it('getProdutos deve mapear a lista paginada (campo content)', async () => {
    mockFetchOnce({ content: [produtoApi] });
    const { getProdutos } = await import('../api/productApi');
    const lista = await getProdutos();
    expect(lista).toHaveLength(1);
    expect(lista[0].name).toBe('Brownie');
  });

  it('getProdutos deve aceitar busca e categoriaId nos parâmetros', async () => {
    mockFetchOnce({ content: [] });
    const { getProdutos } = await import('../api/productApi');
    await getProdutos('brownie', 3);
    const urlChamada = global.fetch.mock.calls[0][0];
    expect(urlChamada).toContain('busca=brownie');
    expect(urlChamada).toContain('categoriaId=3');
  });

  it('getProdutos com resposta que já é array deve mapear diretamente', async () => {
    mockFetchOnce([produtoApi]);
    const { getProdutos } = await import('../api/productApi');
    const lista = await getProdutos();
    expect(lista).toHaveLength(1);
  });

  it('getProdutos com resposta inesperada deve retornar lista vazia', async () => {
    mockFetchOnce({ algumaCoisa: 'inesperada' });
    const { getProdutos } = await import('../api/productApi');
    const lista = await getProdutos();
    expect(lista).toEqual([]);
  });

  it('getCategorias deve retornar os dados crus da API', async () => {
    mockFetchOnce([{ id: 1, nome: 'Doces' }]);
    const { getCategorias } = await import('../api/productApi');
    const categorias = await getCategorias();
    expect(categorias).toEqual([{ id: 1, nome: 'Doces' }]);
  });

  it('getProdutoById deve retornar um produto mapeado', async () => {
    mockFetchOnce(produtoApi);
    const { getProdutoById } = await import('../api/productApi');
    const produto = await getProdutoById(1);
    expect(produto.name).toBe('Brownie');
  });

  it('getPromocoes deve mapear a lista de produtos', async () => {
    mockFetchOnce({ content: [produtoApi] });
    const { getPromocoes } = await import('../api/productApi');
    const lista = await getPromocoes();
    expect(lista).toHaveLength(1);
  });

  it('getMaisVendidos deve mapear a lista de produtos', async () => {
    mockFetchOnce({ content: [produtoApi] });
    const { getMaisVendidos } = await import('../api/productApi');
    const lista = await getMaisVendidos();
    expect(lista).toHaveLength(1);
  });
});
