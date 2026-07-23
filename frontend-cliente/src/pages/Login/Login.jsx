import React, { useState } from "react";
import "./Login.css";
import Input from "../../components/Input/Input";
import Button from "../../components/Button/Button";
import LayoutStripes from "../../components/LayoutStripes/LayoutStripes";
import Logo from "../../assets/logo.png";
import { useAuth } from "../../context/AuthContext";
import { Link, useNavigate, useLocation } from "react-router-dom";
import BackButton from "../../components/BackButton/BackButton";

/**
 * Página de Login refatorada.
 *
 * MUDANÇAS:
 * ─────────
 * 1. Adicionado link "Esqueceu sua senha?" que redireciona para /recuperar-senha.
 * 2. Erro exibido via componente <AlertErro> em vez de <p> com style inline.
 * 3. Nome do handler corrigido: handleChange (era inconsistente em Register).
 */
const Login = () => {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const from = location.state?.from?.pathname || "/";

  const [form, setForm] = useState({ email: "", senha: "" });
  const [erro, setErro] = useState("");
  const [loading, setLoading] = useState(false);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
    if (erro) setErro("");
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    const result = await login(form.email, form.senha);
    setLoading(false);

    if (result.success) {
      navigate(from, { replace: true });
    } else {
      setErro(result.message || "E-mail ou senha inválidos");
    }
  };

  return (
    <div>
      <BackButton />
      <LayoutStripes image={Logo} title="Login">
        <form className="login-form" onSubmit={handleSubmit}>
          <Input
            name="email"
            label="E-mail"
            type="email"
            placeholder="seu@email.com"
            value={form.email}
            onChange={handleChange}
            autoComplete="email"
          />
          <Input
            name="senha"
            label="Senha"
            type="password"
            placeholder="••••••••"
            value={form.senha}
            onChange={handleChange}
            autoComplete="current-password"
          />

          {erro && <p className="login-erro">{erro}</p>}

          <Button
            className="login-button"
            variant="primary"
            type="submit"
            disabled={loading}
          >
            {loading ? "Entrando..." : "Entrar"}
          </Button>
        </form>

        {/* Link de recuperação de senha — NOVO */}
        <p className="login-text">
          <Link to="/recuperar-senha" className="login-link">
            Esqueceu sua senha?
          </Link>
        </p>

        <p className="login-text">
          Não tem uma conta?{" "}
          <Link to="/cadastro" className="login-link">
            Cadastre-se
          </Link>
        </p>
      </LayoutStripes>
    </div>
  );
};

export default Login;
