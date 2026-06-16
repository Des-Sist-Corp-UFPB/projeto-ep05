import React, { useState } from "react";
import { useAuth } from "../../context/AuthContext";
import "./Profile.css";
import { useNavigate } from "react-router-dom";
import Input from "../../components/Input/Input";
import Button from "../../components/Button/Button";
import MainScrollContainer from "../../components/MainScrollContainer/MainScrollContainer";
import BackButton from "../../components/BackButton/BackButton";

/**
 * Página de Perfil refatorada.
 *
 * MUDANÇAS:
 * ─────────
 * 1. Campos de senha renomeados para bater exatamente com o PerfilUpdateRequest
 *    do backend: { nome, email, senhaAtual, novaSenha }.
 *    Antes havia campo "confirmarNovaSenha" que era enviado como novaSenha
 *    no AuthContext — confuso e propenso a bug.
 *
 * 2. Validação de confirmar nova senha feita localmente antes de chamar a API.
 *
 * 3. window.confirm substituído por estado de confirmação inline (sem popup nativo)
 *    para UX mais consistente e testável.
 *
 * 4. Mensagens de status exibidas via classe CSS (.alert.success / .alert.error).
 */
const Profile = () => {
  const { user, updateUser, deleteUser } = useAuth();
  const navigate = useNavigate();

  const [form, setForm] = useState({
    nome: user?.nome ?? "",
    email: user?.email ?? "",
    senhaAtual: "",
    novaSenha: "",
    confirmarNovaSenha: "",
  });

  const [isEditing, setIsEditing] = useState(false);
  const [confirmandoExclusao, setConfirmandoExclusao] = useState(false);
  const [status, setStatus] = useState({ type: "", message: "" });
  const [erros, setErros] = useState({});
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
    if (form.novaSenha && form.novaSenha !== form.confirmarNovaSenha) {
      novosErros.confirmarNovaSenha = "As senhas não coincidem";
    }
    if (form.novaSenha && !form.senhaAtual) {
      novosErros.senhaAtual = "Informe sua senha atual para alterar a senha";
    }
    return novosErros;
  };

  const handleSave = async (e) => {
    e.preventDefault();
    const novosErros = validar();
    if (Object.keys(novosErros).length > 0) {
      setErros(novosErros);
      return;
    }

    setLoading(true);
    // Envia apenas os campos que o backend espera
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
    setForm({
      nome: user?.nome ?? "",
      email: user?.email ?? "",
      senhaAtual: "",
      novaSenha: "",
      confirmarNovaSenha: "",
    });
  };

  const handleDelete = async () => {
    await deleteUser();
    navigate("/");
  };

  return (
    <div className="profile-page">
      <div className="profile-card">
        <MainScrollContainer height="calc(80vh - 40px)">
          <div style={{ width: "100%", maxWidth: "1200px" }}>
            <BackButton variant="2" />
          </div>

          <div className="profile-header">
            <div className="avatar-circle">
              {user?.nome?.charAt(0).toUpperCase()}
            </div>
            <h1>{isEditing ? "Editando Perfil" : `Olá, ${user?.nome}!`}</h1>
            <p>{user?.email}</p>
          </div>

          {status.message && (
            <div className={`alert ${status.type}`}>{status.message}</div>
          )}

          <form onSubmit={handleSave}>
            <div className="input-group">
              <Input
                label="Nome Completo" name="nome"
                value={form.nome} onChange={handleChange}
                disabled={!isEditing} erro={erros.nome}
              />
            </div>
            <div className="input-group">
              <Input
                label="E-mail" name="email" type="email"
                value={form.email} onChange={handleChange}
                disabled={!isEditing} erro={erros.email}
              />
            </div>

            {isEditing && (
              <>
                <div className="input-group">
                  <Input
                    label="Senha Atual" name="senhaAtual" type="password"
                    value={form.senhaAtual} onChange={handleChange}
                    placeholder="Obrigatório para alterar a senha"
                    erro={erros.senhaAtual}
                  />
                </div>
                <div className="input-group">
                  <Input
                    label="Nova Senha (opcional)" name="novaSenha" type="password"
                    value={form.novaSenha} onChange={handleChange}
                    placeholder="Deixe em branco para não alterar"
                    erro={erros.novaSenha}
                  />
                </div>
                <div className="input-group">
                  <Input
                    label="Confirmar Nova Senha" name="confirmarNovaSenha" type="password"
                    value={form.confirmarNovaSenha} onChange={handleChange}
                    placeholder="Repita a nova senha"
                    erro={erros.confirmarNovaSenha}
                  />
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

          <hr />

          {/* Zona de perigo com confirmação inline */}
          <div className="danger-zone">
            <h3>Zona de Perigo</h3>
            <p>Ao excluir sua conta, você perderá todos os seus pedidos e dados.</p>

            {!confirmandoExclusao ? (
              <button
                className="btn-delete"
                onClick={() => setConfirmandoExclusao(true)}
              >
                Excluir minha conta
              </button>
            ) : (
              <div className="confirm-delete">
                <p className="confirm-delete-texto">
                  Tem certeza? Esta ação é permanente e não pode ser desfeita.
                </p>
                <div className="confirm-delete-acoes">
                  <button className="btn-delete" onClick={handleDelete}>
                    Sim, excluir minha conta
                  </button>
                  <button
                    className="btn-cancel-delete"
                    onClick={() => setConfirmandoExclusao(false)}
                  >
                    Cancelar
                  </button>
                </div>
              </div>
            )}
          </div>
        </MainScrollContainer>
      </div>
    </div>
  );
};

export default Profile;
