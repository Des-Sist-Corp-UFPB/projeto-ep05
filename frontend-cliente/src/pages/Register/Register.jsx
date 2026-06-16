import React, { useState } from "react";
import "./Register.css";
import Input from "../../components/Input/Input";
import Button from "../../components/Button/Button";
import LayoutStripes from "../../components/LayoutStripes/LayoutStripes";
import Logo from "../../assets/logo.png";
import BackButton from "../../components/BackButton/BackButton";
import { Link, useNavigate } from "react-router-dom";
import { validarCampo, validarFormulario } from "../../utils/validarFormulario";
import { useAuth } from "../../context/AuthContext";
import MainScrollContainer from "../../components/MainScrollContainer/MainScrollContainer";

/**
 * Página de Cadastro refatorada.
 *
 * MUDANÇAS:
 * ─────────
 * 1. handlechange → handleChange (capitalização consistente).
 * 2. Erro geral de API exibido em elemento próprio .register-erro
 *    em vez de ser injetado em erros.email (que confunde o usuário).
 * 3. autoComplete attributes adicionados para melhor UX.
 */
const Register = () => {
  const navigate = useNavigate();
  const { register } = useAuth();

  const [form, setForm] = useState({ 
    nome: "",
    sobrenome: "",
    email: "",
    cpf: "",
    telefone: "", 
    senha: "",
    confirmarSenha: ""
  });
  const [erros, setErros] = useState({});
  const [erroGeral, setErroGeral] = useState("");
  const [loading, setLoading] = useState(false);

    const handleChange = (e) => {
      const { name } = e.target;
      let { value } = e.target;

      if (name === "cpf") {
        value = value.replace(/\D/g, "").substring(0, 11);
        value = value.replace(/(\d{3})(\d)/, "$1.$2");
        value = value.replace(/(\d{3})(\d)/, "$1.$2");
        value = value.replace(/(\d{3})(\d{1,2})$/, "$1-$2");
      }

      if (name === "telefone") {
        value = value.replace(/\D/g, "").substring(0, 11);
        if (value.length <= 10) {
          value = value.replace(/(\d{2})(\d)/, "($1) $2");
          value = value.replace(/(\d{4})(\d)/, "$1-$2");
        } else {
          value = value.replace(/(\d{2})(\d)/, "($1) $2");
          value = value.replace(/(\d{5})(\d)/, "$1-$2");
        }
      }

      const novosDados = { ...form, [name]: value };
      setForm(novosDados);
      const errosCampo = validarCampo(name, novosDados);
      setErros((prev) => ({ ...prev, [name]: errosCampo }));
      if (erroGeral) setErroGeral("");
    };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const errosValidados = validarFormulario(form);

    if (Object.keys(errosValidados).length > 0) {
      setErros(errosValidados);
      return;
    }

    setLoading(true);
    const result = await register(form);
    setLoading(false);

    if (!result.success) {
      setErroGeral(result.message || "Erro ao cadastrar. Tente novamente.");
      return;
    }

    navigate("/");
  };

  return (
    <div>
      <MainScrollContainer>
        <BackButton />
        <LayoutStripes image={Logo} title="Cadastro">
          <form className="register-form-grid" onSubmit={handleSubmit}>
            <Input
              id="nome" label="Nome" name="nome" type="text"
              value={form.nome} onChange={handleChange}
              placeholder="Seu primeiro nome" erro={erros.nome}
              autoComplete="given-name"
            />
            <Input
              id="sobrenome" label="Sobrenome" name="sobrenome" type="text"
              value={form.sobrenome} onChange={handleChange}
              placeholder="Seu sobrenome" erro={erros.sobrenome}
              autoComplete="family-name"
            />
            <Input
              id="email" name="email" label="E-mail" type="email"
              value={form.email} onChange={handleChange}
              placeholder="seu@email.com" erro={erros.email}
              autoComplete="email"
            />
            <Input
              id="cpf" name="cpf" label="CPF" type="text"
              value={form.cpf} onChange={handleChange}
              placeholder="000.000.000-00" erro={erros.cpf}
              autoComplete="off"
            />
            <Input
              id="telefone" name="telefone" label="Telefone" type="tel"
              value={form.telefone} onChange={handleChange}
              placeholder="(00) 00000-0000" erro={erros.telefone}
              autoComplete="tel"
            />
            <Input
              id="dataNascimento" name="dataNascimento" label="Data de nascimento" type="date"
              value={form.dataNascimento} onChange={handleChange}
              erro={erros.dataNascimento}
            />
            <Input
              id="senha" name="senha" label="Senha" type="password"
              value={form.senha} onChange={handleChange}
              placeholder="Mínimo 8 caracteres" erro={erros.senha}
              autoComplete="new-password"
            />
            <Input
              id="confirmarSenha" name="confirmarSenha" label="Confirmar senha" type="password"
              value={form.confirmarSenha} onChange={handleChange}
              placeholder="Repita a senha" erro={erros.confirmarSenha}
              autoComplete="new-password"
            />

            {/* Erro geral de API (ex: e-mail já cadastrado) */}
            {erroGeral && <p className="register-erro">{erroGeral}</p>}

            <Button className="register-button" variant="primary" type="submit" disabled={loading}>
              {loading ? "Cadastrando..." : "Cadastrar"}
            </Button>
          </form>

          <p className="register-text">
            Já tem uma conta?{" "}
            <Link to="/login" className="register-link">
              Entrar
            </Link>
          </p>
        </LayoutStripes>
      </MainScrollContainer>
    </div>
  );
};

export default Register;
