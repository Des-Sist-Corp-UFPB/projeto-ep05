import React, { useState, useEffect, useRef, useCallback } from "react";
import { useAuth } from "../../context/AuthContext";
import "./Profile.css";
import { useNavigate } from "react-router-dom";
import Input from "../../components/Input/Input";
import Button from "../../components/Button/Button";
import MainScrollContainer from "../../components/MainScrollContainer/MainScrollContainer";
import BackButton from "../../components/BackButton/BackButton";
import { apiFetch } from "../../api/api";
import { buscarCEP } from "../../api/cep";
import { formatCep } from "../../utils/validarEndereco/masksAddress";
import { validarCampoAddress, validarFormularioAddress } from "../../utils/validarEndereco/addressValidators";

// ── Aba: Dados Pessoais ───────────────────────────────────────────────────────

const TabDados = ({ user, updateUser }) => {
  const [form, setForm] = useState({
    nome: user?.nome ?? "",
    email: user?.email ?? "",
    senhaAtual: "",
    novaSenha: "",
    confirmarNovaSenha: "",
  });
  const [isEditing, setIsEditing] = useState(false);
  const [erros, setErros] = useState({});
  const [status, setStatus] = useState({ type: "", message: "" });
  const [loading, setLoading] = useState(false);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
    if (erros[name]) setErros((prev) => ({ ...prev, [name]: "" }));
  };

  const validar = () => {
    const novosErros = {};
    if (!form.nome.trim()) novosErros.nome = "Nome é obrigatório";
    if (!form.email.trim()) novosErros.email = "E-mail é obrigatório";
    if (form.novaSenha && form.novaSenha !== form.confirmarNovaSenha)
      novosErros.confirmarNovaSenha = "As senhas não coincidem";
    if (form.novaSenha && !form.senhaAtual)
      novosErros.senhaAtual = "Informe sua senha atual para alterar a senha";
    return novosErros;
  };

  const handleSave = async (e) => {
    e.preventDefault();
    const novosErros = validar();
    if (Object.keys(novosErros).length > 0) { setErros(novosErros); return; }

    setLoading(true);
    const result = await updateUser({
      nome: form.nome,
      email: form.email,
      senhaAtual: form.senhaAtual || undefined,
      novaSenha: form.novaSenha || undefined,
    });
    setLoading(false);
    setStatus({ type: result.success ? "success" : "error", message: result.message });
    if (result.success) {
      setIsEditing(false);
      setForm((prev) => ({ ...prev, senhaAtual: "", novaSenha: "", confirmarNovaSenha: "" }));
    }
    setTimeout(() => setStatus({ type: "", message: "" }), 3500);
  };

  const handleCancel = () => {
    setIsEditing(false);
    setErros({});
    setForm({ nome: user?.nome ?? "", email: user?.email ?? "", senhaAtual: "", novaSenha: "", confirmarNovaSenha: "" });
  };

  return (
    <div className="tab-content">
      {status.message && <div className={`alert ${status.type}`}>{status.message}</div>}

      <form onSubmit={handleSave}>
        <div className="input-group">
          <Input label="Nome Completo" name="nome" value={form.nome}
            onChange={handleChange} disabled={!isEditing} erro={erros.nome} />
        </div>
        <div className="input-group">
          <Input label="E-mail" name="email" type="email" value={form.email}
            onChange={handleChange} disabled={!isEditing} erro={erros.email} />
        </div>

        {isEditing && (
          <>
            <div className="input-group">
              <Input label="Senha Atual" name="senhaAtual" type="password"
                value={form.senhaAtual} onChange={handleChange}
                placeholder="Obrigatório para alterar a senha" erro={erros.senhaAtual} />
            </div>
            <div className="input-group">
              <Input label="Nova Senha (opcional)" name="novaSenha" type="password"
                value={form.novaSenha} onChange={handleChange}
                placeholder="Deixe em branco para não alterar" erro={erros.novaSenha} />
            </div>
            <div className="input-group">
              <Input label="Confirmar Nova Senha" name="confirmarNovaSenha" type="password"
                value={form.confirmarNovaSenha} onChange={handleChange}
                placeholder="Repita a nova senha" erro={erros.confirmarNovaSenha} />
            </div>
          </>
        )}

        <div className="profile-actions">
          {!isEditing ? (
            <Button type="button" className="btn-edit" onClick={() => setIsEditing(true)}>
              Editar Dados
            </Button>
          ) : (
            <div className="profile-actions-edit">
              <Button type="submit" className="btn-save" disabled={loading}>
                {loading ? "Salvando..." : "Salvar"}
              </Button>
              <Button type="button" className="btn-cancel" onClick={handleCancel}>
                Cancelar
              </Button>
            </div>
          )}
        </div>
      </form>
    </div>
  );
};

// ── Aba: Endereços ────────────────────────────────────────────────────────────

const FormEndereco = ({ onSalvo, onCancelar }) => {
  const [form, setForm] = useState({
    cep: "", rua: "", numero: "", complemento: "", bairro: "", cidade: "", estado: "",
  });
  const [erros, setErros] = useState({});
  const [loadingCep, setLoadingCep] = useState(false);
  const [saving, setSaving] = useState(false);
  const [apiError, setApiError] = useState("");
  const debounceRef = useRef(null);
  const cepValido = form.cep.replace(/\D/g, "").length === 8;

  const handleChange = (e) => {
    const { name, value } = e.target;
    const formatted = name === "cep" ? formatCep(value) : value;
    const novosDados = { ...form, [name]: formatted };
    setForm(novosDados);
    setErros((prev) => ({ ...prev, [name]: validarCampoAddress(name, novosDados) }));

    if (name === "cep") {
      clearTimeout(debounceRef.current);
      debounceRef.current = setTimeout(async () => {
        const clean = formatted.replace(/\D/g, "");
        if (clean.length !== 8) return;
        setLoadingCep(true);
        setErros((prev) => ({ ...prev, cep: "" }));
        const data = await buscarCEP(clean);
        if (data && !data.erro) {
          const atualizado = { ...novosDados, rua: data.logradouro || "", bairro: data.bairro || "", cidade: data.localidade || "", estado: data.uf || "" };
          setForm(atualizado);
          setErros(validarFormularioAddress(atualizado));
        } else {
          setErros((prev) => ({ ...prev, cep: "CEP não encontrado" }));
        }
        setLoadingCep(false);
      }, 500);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const errosValidados = validarFormularioAddress(form);
    if (Object.keys(errosValidados).length > 0) { setErros(errosValidados); return; }

    setSaving(true);
    setApiError("");
    try {
      const salvo = await apiFetch("/clientes/enderecos", {
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
      onSalvo(salvo);
    } catch (err) {
      setApiError(err.mensagem || "Erro ao salvar endereço");
      setSaving(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="address-form-inline">
      {apiError && <p className="form-api-error">{apiError}</p>}
      <Input name="cep" label="CEP" placeholder="00000-000" value={form.cep} onChange={handleChange} erro={erros.cep} />
      {loadingCep && <p className="loading-cep-text">Buscando CEP...</p>}
      <Input name="rua" label="Rua / Logradouro" value={form.rua} onChange={handleChange} erro={erros.rua} />
      <div className="address-row">
        <Input name="numero" label="Número" value={form.numero} onChange={handleChange} erro={erros.numero} />
        <Input name="bairro" label="Bairro" value={form.bairro} onChange={handleChange} erro={erros.bairro} />
      </div>
      <Input name="complemento" label="Complemento (opcional)" value={form.complemento} onChange={handleChange} />
      <div className="address-row">
        <Input name="cidade" label="Cidade" value={form.cidade} onChange={handleChange} erro={erros.cidade} readOnly={cepValido} />
        <Input name="estado" label="Estado" value={form.estado} onChange={handleChange} erro={erros.estado} readOnly={cepValido} />
      </div>
      <div className="form-actions">
        <Button type="submit" disabled={saving}>{saving ? "Salvando..." : "Salvar endereço"}</Button>
        {onCancelar && (
          <Button type="button" className="btn-cancel" onClick={onCancelar}>Cancelar</Button>
        )}
      </div>
    </form>
  );
};

const TabEnderecos = () => {
  const [enderecos, setEnderecos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [mostrando, setMostrando] = useState(false); // form de novo endereço
  const [erro, setErro] = useState("");

  const carregar = useCallback(async () => {
    setLoading(true);
    try {
      const data = await apiFetch("/clientes/enderecos");
      setEnderecos(data ?? []);
    } catch (err) {
      setErro(err.mensagem || "Erro ao carregar endereços");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { carregar(); }, [carregar]);

  const handleSalvo = (novo) => {
    setMostrando(false);
    carregar();
  };

  const handleRemover = async (id) => {
    try {
      await apiFetch(`/clientes/enderecos/${id}`, { method: "DELETE" });
      carregar();
    } catch (err) {
      setErro(err.mensagem || "Erro ao remover endereço");
    }
  };

  if (loading) return <p className="tab-loading">Carregando endereços...</p>;

  return (
    <div className="tab-content">
      {erro && <div className="alert error">{erro}</div>}

      {enderecos.length === 0 && !mostrando && (
        <p className="empty-state">Nenhum endereço cadastrado ainda.</p>
      )}

      {enderecos.map((end) => (
        <div key={end.id} className={`address-card-item ${end.principal ? "principal" : ""}`}>
          {end.principal && <span className="badge-principal">Principal</span>}
          <p className="address-line">{end.logradouro}, {end.numero}{end.complemento ? `, ${end.complemento}` : ""}</p>
          <p className="address-line">{end.bairro} — {end.cidade}/{end.estado}</p>
          <p className="address-line">CEP: {end.cep}</p>
          <button className="btn-remover" onClick={() => handleRemover(end.id)}>Remover</button>
        </div>
      ))}

      {mostrando ? (
        <div className="form-novo-endereco">
          <h3>Novo Endereço</h3>
          <FormEndereco onSalvo={handleSalvo} onCancelar={() => setMostrando(false)} />
        </div>
      ) : (
        <Button type="button" className="btn-novo-endereco" onClick={() => setMostrando(true)}>
          + Adicionar novo endereço
        </Button>
      )}
    </div>
  );
};

// ── Aba: Excluir Conta ────────────────────────────────────────────────────────

const TabPerigo = ({ deleteUser }) => {
  const navigate = useNavigate();
  const [confirmando, setConfirmando] = useState(false);

  const handleDelete = async () => {
    await deleteUser();
    navigate("/");
  };

  return (
    <div className="tab-content danger-zone">
      <h3>Zona de Perigo</h3>
      <p>Ao excluir sua conta, você perderá todos os seus pedidos e dados permanentemente.</p>
      {!confirmando ? (
        <button className="btn-delete" onClick={() => setConfirmando(true)}>Excluir minha conta</button>
      ) : (
        <div className="confirm-delete">
          <p className="confirm-delete-texto">Tem certeza? Esta ação não pode ser desfeita.</p>
          <div className="confirm-delete-acoes">
            <button className="btn-delete" onClick={handleDelete}>Sim, excluir minha conta</button>
            <button className="btn-cancel-delete" onClick={() => setConfirmando(false)}>Cancelar</button>
          </div>
        </div>
      )}
    </div>
  );
};

// ── Profile principal ─────────────────────────────────────────────────────────

const ABAS = ["Dados Pessoais", "Endereços", "Excluir Conta"];

const Profile = () => {
  const { user, updateUser, deleteUser } = useAuth();
  const [abaAtiva, setAbaAtiva] = useState(0);

  return (
    <div className="profile-page">
      <div className="profile-card">
        <MainScrollContainer height="calc(90vh - 40px)">
          <div style={{ width: "100%", maxWidth: "1200px" }}>
            <BackButton variant="2" />
          </div>

          <div className="profile-header">
            <div className="avatar-circle">{user?.nome?.charAt(0).toUpperCase()}</div>
            <h1>Olá, {user?.nome}!</h1>
            <p>{user?.email}</p>
          </div>

          {/* Abas */}
          <div className="profile-tabs">
            {ABAS.map((aba, i) => (
              <button
                key={aba}
                className={`profile-tab ${abaAtiva === i ? "ativa" : ""} ${i === 2 ? "tab-perigo" : ""}`}
                onClick={() => setAbaAtiva(i)}
              >
                {aba}
              </button>
            ))}
          </div>

          {abaAtiva === 0 && <TabDados user={user} updateUser={updateUser} />}
          {abaAtiva === 1 && <TabEnderecos />}
          {abaAtiva === 2 && <TabPerigo deleteUser={deleteUser} />}
        </MainScrollContainer>
      </div>
    </div>
  );
};

export default Profile;
