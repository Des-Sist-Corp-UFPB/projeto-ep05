/**
 * Camada de acesso à API REST do backend.
 *
 * MUDANÇAS:
 * ─────────
 * 1. apiFetch agora lança um objeto de erro estruturado { status, mensagem }
 *    em vez de retornar a Response crua. Isso elimina a necessidade de
 *    res.ok checks espalhados em todo o código.
 *
 * 2. Adicionado parseErro() que lê o JSON de erro padronizado do backend
 *    (ErroResponse) e extrai a mensagem de forma segura.
 *
 * 3. BASE_URL exportada por nome para facilitar mocks em testes.
 */

export const BASE_URL = import.meta.env.VITE_API_URL ?? "http://localhost:8080/api";

// ── Token ─────────────────────────────────────────────────────────────────────

function getToken() {
  try {
    const user = localStorage.getItem("user");
    return user ? JSON.parse(user).token : null;
  } catch {
    return null;
  }
}

// ── Erro estruturado ──────────────────────────────────────────────────────────

/**
 * Lê a resposta de erro do backend (ErroResponse JSON ou texto puro)
 * e retorna uma mensagem legível.
 */
async function parseErro(res) {
  try {
    const data = await res.json();
    // ErroResponse: { status, mensagem, caminho, timestamp }
    return data.mensagem ?? "Erro desconhecido";
  } catch {
    // Fallback para respostas de texto puro (ex: Spring Security padrão)
    return res.statusText || "Erro na requisição";
  }
}

// ── apiFetch ─────────────────────────────────────────────────────────────────

/**
 * Wrapper central para todas as chamadas à API.
 *
 * @returns {Promise<any>} dados parseados do JSON de sucesso
 * @throws  {{ status: number, mensagem: string }} em caso de erro HTTP
 */
export async function apiFetch(path, options = {}) {
  const token = getToken();

  const headers = {
    "Content-Type": "application/json",
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...options.headers,
  };

  let res;
  try {
    res = await fetch(`${BASE_URL}${path}`, { ...options, headers });
  } catch {
    throw { status: 0, mensagem: "Sem conexão com o servidor" };
  }

  if (!res.ok) {
    const mensagem = await parseErro(res);
    throw { status: res.status, mensagem };
  }

  // 204 No Content ou DELETE sem corpo
  if (res.status === 204 || res.headers.get("content-length") === "0") {
    return null;
  }

  return res.json();
}
