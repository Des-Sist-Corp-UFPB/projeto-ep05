import { useState, useRef, useEffect } from "react";
import { useNavigate } from "react-router-dom";

import Input from "../../components/Input/Input";
import Button from "../../components/Button/Button";

import { validarCampoAddress, validarFormularioAddress } from "../../utils/validarEndereco/addressValidators";
import { formatCep } from "../../utils/validarEndereco/masksAddress";
import { buscarCEP } from "../../api/cep";
import { apiFetch } from "../../api/api";
import BackButton from "../../components/BackButton/BackButton";

import "./Address.css";
import MainScrollContainer from "../../components/MainScrollContainer/MainScrollContainer";

const Address = () => {
  const navigate = useNavigate();

  // ── Endereços existentes ──────────────────────────────────────
  const [enderecos, setEnderecos] = useState([]);
  const [loadingEnderecos, setLoadingEnderecos] = useState(true);
  const [enderecoSelecionado, setEnderecoSelecionado] = useState(null);
  const [mostrarFormulario, setMostrarFormulario] = useState(false);

  // ── Formulário novo endereço ──────────────────────────────────
  const [form, setForm] = useState({
    cep: "", rua: "", numero: "", complemento: "", bairro: "", cidade: "", estado: "",
  });
  const [erros, setErros] = useState({});
  const [loadingCep, setLoadingCep] = useState(false);
  const [saving, setSaving] = useState(false);
  const [success, setSuccess] = useState(false);
  const [apiError, setApiError] = useState("");

  const debounceRef = useRef(null);
  const cepValido = form.cep.replace(/\D/g, "").length === 8;

  // ── Busca endereços ao montar ─────────────────────────────────
  useEffect(() => {
    apiFetch("/clientes/enderecos")
      .then((data) => {
        setEnderecos(data || []);
        if (data && data.length > 0) {
          setEnderecoSelecionado(data[0].id);
        } else {
          setMostrarFormulario(true);
        }
      })
      .catch(() => setMostrarFormulario(true))
      .finally(() => setLoadingEnderecos(false));
  }, []);

  // ── Handlers formulário ───────────────────────────────────────
  const handleChange = (e) => {
    const { name, value } = e.target;
    let formattedValue = name === "cep" ? formatCep(value) : value;

    const novosDados = { ...form, [name]: formattedValue };
    setForm(novosDados);
    setErros((prev) => ({ ...prev, [name]: validarCampoAddress(name, novosDados) }));

    if (name === "cep") {
      clearTimeout(debounceRef.current);
      debounceRef.current = setTimeout(async () => {
        const cleanCep = formattedValue.replace(/\D/g, "");
        if (cleanCep.length !== 8) return;

        setLoadingCep(true);
        setErros((prev) => ({ ...prev, cep: "" }));

        const data = await buscarCEP(cleanCep);

        if (data && !data.erro) {
          const dadosAtualizados = {
            ...novosDados,
            rua: data.logradouro || "",
            bairro: data.bairro || "",
            cidade: data.localidade || "",
            estado: data.uf || "",
          };
          setForm(dadosAtualizados);
          setErros(validarFormularioAddress(dadosAtualizados));
        } else {
          setErros((prev) => ({ ...prev, cep: "CEP não encontrado" }));
        }

        setLoadingCep(false);
      }, 500);
    }
  };

  const handleSubmitNovo = async (e) => {
    e.preventDefault();

    const errosValidados = validarFormularioAddress(form);
    if (Object.keys(errosValidados).length > 0) {
      setErros(errosValidados);
      return;
    }

    setSaving(true);
    setApiError("");

    try {
      await apiFetch("/clientes/enderecos", {
        method: "POST",
        body: JSON.stringify({
          logradouro: form.rua,
          numero: form.numero,
          complemento: form.complemento || "",
          bairro: form.bairro,
          cidade: form.cidade,
          estado: form.estado,
          cep: form.cep.replace(/\D/g, ""),
          principal: true,
        }),
      });
      setSuccess(true);
    } catch (err) {
      setApiError(err.mensagem || "Erro ao salvar endereço");
      setSaving(false);
    }
  };

  const handleUsarSelecionado = () => {
    navigate("/checkout");
  };

  useEffect(() => {
    if (success) {
      const timer = setTimeout(() => navigate("/checkout"), 2000);
      return () => clearTimeout(timer);
    }
  }, [success, navigate]);

  // ── Tela de sucesso ───────────────────────────────────────────
  if (success) {
    return (
      <div className="success-container">
        <div className="success-card">
          <h1>✅ Endereço salvo!</h1>
          <p>Redirecionando para o pagamento...</p>
          <div className="loader-dots">
            <div></div><div></div><div></div>
          </div>
        </div>
      </div>
    );
  }

  // ── Loading inicial ───────────────────────────────────────────
  if (loadingEnderecos) {
    return (
      <div className="success-container">
        <div className="success-card">
          <p>Carregando endereços...</p>
          <div className="loader-dots">
            <div></div><div></div><div></div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <MainScrollContainer height="calc(100vh - 40px)">
      <div className="address-container">
        <div className="address-card">
          <BackButton variant="2" />
          <h1>Endereço de Entrega</h1>

          {/* ── Lista de endereços existentes ── */}
          {enderecos.length > 0 && (
            <div className="address-lista">
              <p className="address-lista-titulo">Escolha um endereço salvo:</p>

              {enderecos.map((end) => (
                <div
                  key={end.id}
                  className={`address-item ${enderecoSelecionado === end.id ? "address-item--selecionado" : ""}`}
                  onClick={() => {
                    setEnderecoSelecionado(end.id);
                    setMostrarFormulario(false);
                  }}
                >
                  <div className="address-item-radio">
                    <div className={`radio-circulo ${enderecoSelecionado === end.id ? "radio-circulo--ativo" : ""}`} />
                  </div>
                  <div className="address-item-info">
                    <p className="address-item-rua">{end.logradouro}, {end.numero}</p>
                    {end.complemento && <p className="address-item-detalhe">{end.complemento}</p>}
                    <p className="address-item-detalhe">{end.bairro} — {end.cidade}/{end.estado}</p>
                    <p className="address-item-detalhe">CEP: {end.cep}</p>
                  </div>
                </div>
              ))}

              {!mostrarFormulario && (
                <div className="address-acoes">
                  <Button onClick={handleUsarSelecionado} disabled={!enderecoSelecionado}>
                    Usar este endereço
                  </Button>
                  <button
                    className="address-link-novo"
                    onClick={() => { setMostrarFormulario(true); setEnderecoSelecionado(null); }}
                  >
                    + Adicionar novo endereço
                  </button>
                </div>
              )}
            </div>
          )}

          {/* ── Formulário novo endereço ── */}
          {mostrarFormulario && (
            <div className="address-form-wrapper">
              {enderecos.length > 0 && (
                <button
                  className="address-link-novo"
                  onClick={() => { setMostrarFormulario(false); setEnderecoSelecionado(enderecos[0].id); }}
                >
                  ← Voltar para endereços salvos
                </button>
              )}

              <p className="address-lista-titulo" style={{ marginTop: "var(--esp-md)" }}>
                {enderecos.length === 0 ? "Cadastre seu endereço de entrega:" : "Novo endereço:"}
              </p>

              {apiError && <p style={{ color: "red" }}>{apiError}</p>}

              <form onSubmit={handleSubmitNovo} className="address-form">
                <Input name="cep" placeholder="CEP" value={form.cep} onChange={handleChange} erro={erros.cep} />
                {loadingCep && <p>Buscando CEP...</p>}
                <Input name="rua" placeholder="Rua / Logradouro" value={form.rua} onChange={handleChange} erro={erros.rua} />
                <div style={{ display: "grid", gridTemplateColumns: "1fr 2fr", gap: "1px" }}>
                  <Input style={{ width: "91px" }} name="numero" placeholder="Número" value={form.numero} onChange={handleChange} erro={erros.numero} />
                  <Input style={{ width: "200px" }} name="bairro" placeholder="Bairro" value={form.bairro} onChange={handleChange} erro={erros.bairro} />
                </div>
                <Input name="complemento" placeholder="Complemento (opcional)" value={form.complemento} onChange={handleChange} />
                <Input name="cidade" placeholder="Cidade" value={form.cidade} onChange={handleChange} erro={erros.cidade} readOnly={cepValido} />
                <Input name="estado" placeholder="Estado" value={form.estado} onChange={handleChange} erro={erros.estado} readOnly={cepValido} />
                <div className="btn-save-address">
                  <Button type="submit" disabled={saving}>
                    {saving ? "Salvando..." : "Salvar e usar este endereço"}
                  </Button>
                </div>
              </form>
            </div>
          )}
        </div>
      </div>
    </MainScrollContainer>
  );
};

export default Address;
