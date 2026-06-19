import { apiFetch } from "./api";

// Mapeia o produto da API (campos em português) para o padrão usado no frontend
export function mapProduto(p) {
  return {
    id: p.id,
    name: p.nome,
    description: p.descricao,
    price: parseFloat(p.preco),
    category: p.categoriaNome || "",
    categoriaId: p.categoriaId,
    stock: p.estoque,
    ativo: p.ativo,
    image: p.imagensUrls && p.imagensUrls.length > 0 ? p.imagensUrls[0] : null,
    images: p.imagensUrls || [],
    rating: p.notaMedia || 0,
  };
}

// NOTA: apiFetch agora retorna o JSON diretamente (ou lança em caso de erro).
// Não use mais res.ok / res.json() — o dado já vem parseado.

export const getProdutos = async (busca = null, categoriaId = null, page = 0, size = 20) => {
  const params = new URLSearchParams({ page, size });
  if (busca) params.append("busca", busca);
  if (categoriaId) params.append("categoriaId", categoriaId);

  const data = await apiFetch(`/produtos?${params}`);
  // A API retorna um Page<ProdutoDTO> com campo "content"
  const lista = data?.content ?? data;
  return Array.isArray(lista) ? lista.map(mapProduto) : [];
};

export const getCategorias = async () => {
  return apiFetch("/produtos/categorias"); // retorna [{id, nome, descricao}]
};

export const getProdutoById = async (id) => {
  const data = await apiFetch(`/produtos/${id}`);
  return mapProduto(data);
};

// A API não tem endpoint /promocoes — retorna todos os produtos (filtragem visual)
export const getPromocoes = async () => {
  const data = await apiFetch("/produtos?size=20&sort=preco,asc");
  const lista = data?.content ?? data;
  return Array.isArray(lista) ? lista.map(mapProduto) : [];
};

// A API não tem endpoint separado para mais vendidos — retorna os primeiros 8 produtos
export const getMaisVendidos = async () => {
  const data = await apiFetch("/produtos?size=8");
  const lista = data?.content ?? data;
  return Array.isArray(lista) ? lista.map(mapProduto) : [];
};
