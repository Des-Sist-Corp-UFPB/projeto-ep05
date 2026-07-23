import React, { useState } from "react";
import "./ForgotPassword.css";
import Input from "../../components/Input/Input";
import Button from "../../components/Button/Button";
import LayoutStripes from "../../components/LayoutStripes/LayoutStripes";
import Logo from "../../assets/logo.png";
import BackButton from "../../components/BackButton/BackButton";
import { Link } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";

/**
 * Fluxo de recuperação de senha em 2 passos:
 *
 *  Passo 1 — Solicitar (email):
 *    POST /api/auth/recuperar-senha { email }
 *    → exibe mensagem genérica de sucesso
 *    → token de dev REMOVIDO da UI por segurança (FIX #5)
 *
 *  Passo 2 — Redefinir (token + nova senha):
 *    POST /api/auth/redefinir-senha { token, novaSenha }
 *    → redireciona para login com mensagem de sucesso
 */
const ForgotPassword = () => {
  const { solicitarRecuperacao, redefinirSenha } = useAuth();

  const [passo, setPasso] = useState(1);

  // Passo 1
  const [email, setEmail] = useState("");

  // Passo 2
  const [token, setToken] = useState("");
  const [novaSenha, setNovaSenha] = useState("");
  const [confirmarSenha, setConfirmarSenha] = useState("");

  const [loading, setLoading] = useState(false);
  const [erro, setErro] = useState("");
  const [sucesso, setSucesso] = useState("");

  // ── Passo 1: solicitar token ─────────────────────────────────────────────

  const handleSolicitar = async (e) => {
    e.preventDefault();
    if (!email.trim()) {
      setErro("Informe seu e-mail");
      return;
    }
    setLoading(true);
    setErro("");
    const result = await solicitarRecuperacao(email);
    setLoading(false);

    if (result.success) {
      // FIX #5: token de dev não é exibido na UI em nenhum ambiente
      setPasso(2);
    } else {
      setErro(result.message);
    }
  };

  // ── Passo 2: redefinir senha ─────────────────────────────────────────────

  const handleRedefinir = async (e) => {
    e.preventDefault();
    if (!token.trim()) {
      setErro("Cole o token recebido por e-mail");
      return;
    }
    if (novaSenha.length < 6) {
      setErro("A senha deve ter pelo menos 6 caracteres");
      return;
    }
    if (novaSenha !== confirmarSenha) {
      setErro("As senhas não coincidem");
      return;
    }

    setLoading(true);
    setErro("");
    const result = await redefinirSenha(token, novaSenha);
    setLoading(false);

    if (result.success) {
      setSucesso("Senha redefinida com sucesso! Você já pode fazer login.");
      setPasso(3);
    } else {
      setErro(result.message);
    }
  };

  // ── Render ────────────────────────────────────────────────────────────────

  return (
    <div>
      <BackButton />
      <LayoutStripes image={Logo} title="Recuperar Senha">

        {/* ── Passo 1: e-mail ── */}
        {passo === 1 && (
          <form className="forgot-form" onSubmit={handleSolicitar}>
            <p className="forgot-descricao">
              Informe seu e-mail e enviaremos um link para redefinir sua senha.
            </p>

            <Input
              name="email"
              label="E-mail"
              type="email"
              placeholder="seu@email.com"
              value={email}
              onChange={(e) => { setEmail(e.target.value); setErro(""); }}
              autoComplete="email"
            />

            {erro && <p className="forgot-erro">{erro}</p>}

            <Button variant="primary" type="submit" disabled={loading} className="forgot-button">
              {loading ? "Enviando..." : "Enviar link de recuperação"}
            </Button>
          </form>
        )}

        {/* ── Passo 2: token + nova senha ── */}
        {passo === 2 && (
          <form className="forgot-form" onSubmit={handleRedefinir}>
            <p className="forgot-descricao">
              Verifique seu e-mail e cole o token abaixo junto com sua nova senha.
            </p>

            <Input
              name="token"
              label="Token de recuperação"
              type="text"
              placeholder="Cole o token aqui"
              value={token}
              onChange={(e) => { setToken(e.target.value); setErro(""); }}
            />
            <Input
              name="novaSenha"
              label="Nova senha"
              type="password"
              placeholder="Mínimo 6 caracteres"
              value={novaSenha}
              onChange={(e) => { setNovaSenha(e.target.value); setErro(""); }}
              autoComplete="new-password"
            />
            <Input
              name="confirmarSenha"
              label="Confirmar nova senha"
              type="password"
              placeholder="Repita a nova senha"
              value={confirmarSenha}
              onChange={(e) => { setConfirmarSenha(e.target.value); setErro(""); }}
              autoComplete="new-password"
            />

            {erro && <p className="forgot-erro">{erro}</p>}

            <Button variant="primary" type="submit" disabled={loading} className="forgot-button">
              {loading ? "Salvando..." : "Redefinir senha"}
            </Button>
          </form>
        )}

        {/* ── Passo 3: sucesso ── */}
        {passo === 3 && (
          <div className="forgot-sucesso">
            <span className="forgot-sucesso-icone">✓</span>
            <p>{sucesso}</p>
            <Link to="/login" className="forgot-link">
              Ir para o login
            </Link>
          </div>
        )}

        {passo < 3 && (
          <p className="forgot-text">
            Lembrou a senha?{" "}
            <Link to="/login" className="forgot-link">
              Entrar
            </Link>
          </p>
        )}
      </LayoutStripes>
    </div>
  );
};

export default ForgotPassword;
