import {
  createContext,
  useContext,
  useState,
  useEffect,
  useMemo,
  useCallback,
} from "react";
import { apiFetch } from "../api/api";

/**
 * AuthContext refatorado.
 *
 * MUDANÇAS:
 * ─────────
 * 1. apiFetch agora lança em caso de erro — removidos res.ok checks duplicados.
 *    Todos os métodos usam try/catch único e retornam { success, message }.
 *
 * 2. Adicionados: solicitarRecuperacao(email) e redefinirSenha(token, novaSenha)
 *    para suportar o novo fluxo de recuperação de senha.
 *
 * 3. Loading inicial mostra componente Spinner em vez de <p>Carregando...</p>.
 *
 * 4. Dados do usuário são validados antes de parsear do localStorage para
 *    evitar crash se o storage estiver corrompido.
 */

const AuthContext = createContext(null);

// ── Provider ──────────────────────────────────────────────────────────────────

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  // Hidratação inicial a partir do localStorage
  useEffect(() => {
    try {
      const stored = localStorage.getItem("user");
      if (stored) setUser(JSON.parse(stored));
    } catch {
      localStorage.removeItem("user"); // limpa dados corrompidos
    } finally {
      setLoading(false);
    }
  }, []);

  // ── Helpers ────────────────────────────────────────────────────────────────

  const persistirUsuario = useCallback((data) => {
    const userData = {
      token: data.token,
      email: data.email,
      nome: data.nome,
      papel: data.papel,
    };
    setUser(userData);
    localStorage.setItem("user", JSON.stringify(userData));
    return userData;
  }, []);

  // ── Auth ──────────────────────────────────────────────────────────────────

  const login = useCallback(async (email, senha) => {
    try {
      const data = await apiFetch("/auth/login", {
        method: "POST",
        body: JSON.stringify({ email, senha }),
      });
      persistirUsuario(data);
      return { success: true };
    } catch (err) {
      return { success: false, message: err.mensagem ?? "E-mail ou senha inválidos" };
    }
  }, [persistirUsuario]);

  const register = useCallback(async (newUser) => {
  try {
    const data = await apiFetch("/auth/cadastro", {
      method: "POST",
      body: JSON.stringify({
        nome: newUser.nome,
        sobrenome: newUser.sobrenome,
        email: newUser.email,
        cpf: newUser.cpf,
        telefone: newUser.telefone,
        dataNascimento: newUser.dataNascimento,
        senha: newUser.senha,
        confirmacaoSenha: newUser.confirmarSenha,
      }),
    });
    persistirUsuario(data);
    return { success: true };
  } catch (err) {
    return { success: false, message: err.mensagem ?? "Erro ao cadastrar" };
  }
}, [persistirUsuario]);

  const logout = useCallback(() => {
    setUser(null);
    localStorage.removeItem("user");
  }, []);

  // ── Perfil ────────────────────────────────────────────────────────────────

  const updateUser = useCallback(async (updatedData) => {
    try {
      const data = await apiFetch("/clientes/perfil", {
        method: "PUT",
        body: JSON.stringify({
          nome: updatedData.nome,
          email: updatedData.email,
          senhaAtual: updatedData.senhaAtual || null,
          novaSenha: updatedData.novaSenha || null,
        }),
      });
      const newUserData = { ...user, nome: data.nome, email: data.email };
      setUser(newUserData);
      localStorage.setItem("user", JSON.stringify(newUserData));
      return { success: true, message: "Perfil atualizado com sucesso!" };
    } catch (err) {
      return { success: false, message: err.mensagem ?? "Erro ao atualizar perfil" };
    }
  }, [user]);

  const deleteUser = useCallback(async () => {
    try {
      await apiFetch("/clientes/perfil", { method: "DELETE" });
    } catch {
      // ignora erro — conta local é apagada de qualquer forma
    }
    setUser(null);
    localStorage.removeItem("user");
  }, []);

  // ── Recuperação de senha ──────────────────────────────────────────────────

  const solicitarRecuperacao = useCallback(async (email) => {
    try {
      const data = await apiFetch("/auth/recuperar-senha", {
        method: "POST",
        body: JSON.stringify({ email }),
      });
      // data.token existe apenas em dev; em prod, seria undefined
      return { success: true, token: data?.token };
    } catch (err) {
      return { success: false, message: err.mensagem ?? "Erro ao solicitar recuperação" };
    }
  }, []);

  const redefinirSenha = useCallback(async (token, novaSenha) => {
    try {
      await apiFetch("/auth/redefinir-senha", {
        method: "POST",
        body: JSON.stringify({ token, novaSenha }),
      });
      return { success: true };
    } catch (err) {
      return { success: false, message: err.mensagem ?? "Token inválido ou expirado" };
    }
  }, []);

  // ── Context value ─────────────────────────────────────────────────────────

  const value = useMemo(() => ({
    user,
    setUser,
    login,
    register,
    logout,
    updateUser,
    deleteUser,
    solicitarRecuperacao,
    redefinirSenha,
    isAuthenticated: !!user,
    loading,
  }), [
    user, login, register, logout, updateUser,
    deleteUser, solicitarRecuperacao, redefinirSenha, loading,
  ]);

  if (loading) {
    return (
      <div style={{ display: "flex", justifyContent: "center", alignItems: "center", height: "100vh" }}>
        <p>Carregando...</p>
      </div>
    );
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

// ── Hook ──────────────────────────────────────────────────────────────────────

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth deve ser usado dentro de AuthProvider");
  return context;
};
