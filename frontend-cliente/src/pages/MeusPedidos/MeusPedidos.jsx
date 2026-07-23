import { useState, useEffect, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { apiFetch } from "../../api/api";
import MainScrollContainer from "../../components/MainScrollContainer/MainScrollContainer";
import BackButton from "../../components/BackButton/BackButton";
import Button from "../../components/Button/Button";
import "./MeusPedidos.css";

// ── Labels e cores por status ─────────────────────────────────────────────────

const STATUS_INFO = {
  AGUARDANDO_PAGAMENTO: { label: "Aguardando Pagamento", emoji: "⏳", cor: "status--aguardando" },
  PAGO:                 { label: "Pago",                  emoji: "💳", cor: "status--pago"      },
  ENVIADO:              { label: "Enviado",               emoji: "🚚", cor: "status--enviado"   },
  ENTREGUE:             { label: "Entregue",              emoji: "✅", cor: "status--entregue"  },
  CANCELADO:            { label: "Cancelado",             emoji: "❌", cor: "status--cancelado" },
};

const ETAPAS = ["AGUARDANDO_PAGAMENTO", "PAGO", "ENVIADO", "ENTREGUE"];

// Statuses que ainda permitem cancelamento (antes de ENVIADO)
const STATUS_CANCELAVEIS = new Set(["AGUARDANDO_PAGAMENTO", "PAGO"]);

function calcProgresso(status) {
  const s = status?.toUpperCase() ?? "";
  const idx = ETAPAS.indexOf(s);
  if (s === "CANCELADO") return -1;
  return idx >= 0 ? idx : 0;
}

function formatarData(instant) {
  if (!instant) return "—";
  return new Date(instant).toLocaleString("pt-BR", {
    day: "2-digit", month: "2-digit", year: "numeric",
    hour: "2-digit", minute: "2-digit",
  });
}

// ── Componente de timeline de um pedido ──────────────────────────────────────

function TimelinePedido({ pedido }) {
  const statusNorm = pedido.status?.toUpperCase() ?? "";
  const progresso = calcProgresso(statusNorm);
  const cancelado = statusNorm === "CANCELADO";

  return (
    <div className="pedido-timeline">
      {cancelado ? (
        <div className="timeline-cancelado">❌ Pedido cancelado</div>
      ) : (
        <div className="timeline-steps">
          {ETAPAS.map((etapa, idx) => {
            const etapaInfo = STATUS_INFO[etapa];
            const ativo = idx === progresso;
            const concluido = idx < progresso;
            return (
              <div key={etapa} className={`timeline-step ${concluido ? "concluido" : ""} ${ativo ? "ativo" : ""}`}>
                <div className="timeline-step-circle">
                  {concluido ? "✓" : etapaInfo.emoji}
                </div>
                <span className="timeline-step-label">{etapaInfo.label}</span>
                {idx < ETAPAS.length - 1 && (
                  <div className={`timeline-connector ${concluido ? "concluido" : ""}`} />
                )}
              </div>
            );
          })}
        </div>
      )}

      <div className={`pedido-status-badge ${STATUS_INFO[statusNorm]?.cor || ""}`}>
        {STATUS_INFO[statusNorm]?.emoji} {STATUS_INFO[statusNorm]?.label}
      </div>

      {pedido.codigoRastreamento && (
        <p className="pedido-rastreamento">
          📦 Rastreamento: <strong>{pedido.codigoRastreamento}</strong>
        </p>
      )}
    </div>
  );
}

// ── Card de pedido ────────────────────────────────────────────────────────────

function CardPedido({ pedido, onCancelar }) {
  const [aberto, setAberto] = useState(false);
  const [cancelando, setCancelando] = useState(false);
  const [confirmando, setConfirmando] = useState(false);
  const [erroCancelamento, setErroCancelamento] = useState("");
  const [motivo, setMotivo] = useState("");

  // Normaliza o status para maiúsculo para garantir compatibilidade com qualquer formato da API
  const statusNorm = pedido.status?.toUpperCase() ?? "";
  const podeCancelar = STATUS_CANCELAVEIS.has(statusNorm);

  async function handleCancelar() {
    if (!confirmando) {
      setMotivo("");
      setConfirmando(true);
      return;
    }
    if (!motivo.trim()) {
      setErroCancelamento("Informe o motivo do cancelamento.");
      return;
    }
    setCancelando(true);
    setErroCancelamento("");
    try {
      await apiFetch(`/pedidos/${pedido.id}/cancelar`, {
        method: "POST",
        body: JSON.stringify({ motivo }),
      });
      setConfirmando(false);
      onCancelar(pedido.id);
    } catch (err) {
      setErroCancelamento(err.mensagem || "Não foi possível cancelar o pedido.");
      setConfirmando(false);
    } finally {
      setCancelando(false);
    }
  }

  return (
    <div className="pedido-card">
      <div className="pedido-card-header" onClick={() => setAberto(!aberto)}>
        <div className="pedido-card-info">
          <span className="pedido-numero">Pedido #{pedido.id}</span>
          <span className="pedido-data">{formatarData(pedido.criadoEm)}</span>
        </div>
        <div className="pedido-card-direita">
          <span className={`pedido-status-mini ${STATUS_INFO[pedido.status?.toUpperCase()]?.cor || ""}`}>
            {STATUS_INFO[pedido.status?.toUpperCase()]?.emoji} {STATUS_INFO[pedido.status?.toUpperCase()]?.label}
          </span>
          <span className="pedido-total">R$ {Number(pedido.totalGeral).toFixed(2)}</span>
          <span className="pedido-chevron">{aberto ? "▲" : "▼"}</span>
        </div>
      </div>

      {aberto && (
        <div className="pedido-card-body">
          <TimelinePedido pedido={pedido} />

          <div className="pedido-itens">
            <h4>Itens</h4>
            {pedido.itens?.map((item, i) => (
              <div key={i} className="pedido-item-linha">
                <span>{item.nomeProduto || item.produto?.nome || "Produto"} x{item.quantidade}</span>
                <span>R$ {Number(item.subtotal || (item.precoUnitario * item.quantidade)).toFixed(2)}</span>
              </div>
            ))}
          </div>

          {pedido.enderecoEntrega && (
            <div className="pedido-endereco">
              <h4>Endereço de entrega</h4>
              <p>
                {pedido.enderecoEntrega.logradouro}, {pedido.enderecoEntrega.numero}
                {pedido.enderecoEntrega.complemento ? ` — ${pedido.enderecoEntrega.complemento}` : ""}
              </p>
              <p>{pedido.enderecoEntrega.bairro} — {pedido.enderecoEntrega.cidade}/{pedido.enderecoEntrega.estado}</p>
              <p>CEP: {pedido.enderecoEntrega.cep}</p>
            </div>
          )}

          {/* ── Motivo de cancelamento (se cancelado) ── */}
          {statusNorm === "CANCELADO" && pedido.motivoCancelamento && (
            <div style={{background:"#fff0f0",borderRadius:8,padding:"0.75rem 1rem",marginTop:"0.75rem",border:"1px solid #fcd4d4"}}>
              <strong style={{color:"#c0392b",fontSize:"0.85rem"}}>Motivo do cancelamento:</strong>
              <p style={{margin:"0.25rem 0 0",fontSize:"0.9rem",color:"#555"}}>{pedido.motivoCancelamento}</p>
            </div>
          )}

          {/* ── Botão de cancelamento ── */}
          {podeCancelar && (
            <div className="pedido-cancelar-area">
              {erroCancelamento && (
                <p className="pedido-cancelar-erro">{erroCancelamento}</p>
              )}

              {confirmando ? (
                <div className="pedido-cancelar-confirm">
                  <p className="pedido-cancelar-aviso">
                    ⚠️ Informe o motivo do cancelamento:
                  </p>
                  <textarea
                    rows={3}
                    placeholder="Ex: Comprei por engano, quero trocar o produto..."
                    value={motivo}
                    onChange={e => setMotivo(e.target.value)}
                    disabled={cancelando}
                    style={{width:"100%",borderRadius:8,border:"1.5px solid #e0c4d4",padding:"0.6rem",fontSize:"0.9rem",resize:"vertical",boxSizing:"border-box",marginBottom:"0.5rem"}}
                  />
                  <div className="pedido-cancelar-botoes">
                    <Button
                      variant="tertiary"
                      onClick={() => { setConfirmando(false); setMotivo(""); setErroCancelamento(""); }}
                      disabled={cancelando}
                    >
                      Não, manter
                    </Button>
                    <Button
                      variant="danger"
                      onClick={handleCancelar}
                      disabled={cancelando || !motivo.trim()}
                    >
                      {cancelando ? "Cancelando..." : "Sim, cancelar"}
                    </Button>
                  </div>
                </div>
              ) : (
                <Button
                  variant="danger"
                  onClick={handleCancelar}
                  disabled={cancelando}
                >
                  Cancelar pedido
                </Button>
              )}
            </div>
          )}
        </div>
      )}
    </div>
  );
}

// ── Página principal ──────────────────────────────────────────────────────────

const MeusPedidos = () => {
  const navigate = useNavigate();
  const [pedidos, setPedidos] = useState([]);
  const [filtroStatus, setFiltroStatus] = useState("");

  const [loading, setLoading] = useState(true);
  const [erro, setErro] = useState("");
  const [pagina, setPagina] = useState(0);
  const [totalPaginas, setTotalPaginas] = useState(1);

  const carregarPedidos = useCallback(async (p = 0) => {
    setLoading(true);
    setErro("");
    try {
      const data = await apiFetch(`/pedidos?page=${p}&size=5&sort=criadoEm,desc`);
      setPedidos(data.content || []);
      setTotalPaginas(data.totalPages || 1);
      setPagina(p);
    } catch {
      setErro("Não foi possível carregar seus pedidos.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { carregarPedidos(0); }, [carregarPedidos]);

  // Atualiza o status do pedido cancelado localmente sem recarregar tudo
  function handlePedidoCancelado(pedidoId) {
    setPedidos((prev) =>
      prev.map((p) =>
        p.id === pedidoId ? { ...p, status: "CANCELADO" } : p
      )
    );
  }

  return (
    <MainScrollContainer height="calc(100vh - 40px)">
      <div className="meus-pedidos-container">
        <div className="meus-pedidos-header">
          <BackButton variant="2" />
          <h1>Meus Pedidos</h1>
        </div>

        {loading && (
          <div className="meus-pedidos-loading">
            <div className="loader-dots"><div /><div /><div /></div>
            <p>Carregando pedidos...</p>
          </div>
        )}

        {erro && <p className="meus-pedidos-erro">{erro}</p>}

        {!loading && !erro && pedidos.length === 0 && (
          <div className="meus-pedidos-vazio">
            <p>🧁 Você ainda não fez nenhum pedido.</p>
            <Button onClick={() => navigate("/")}>Explorar produtos</Button>
          </div>
        )}

        {!loading && pedidos.map((pedido) => (
          <CardPedido
            key={pedido.id}
            pedido={pedido}
            onCancelar={handlePedidoCancelado}
          />
        ))}

        {totalPaginas > 1 && (
          <div className="meus-pedidos-paginacao">
            <Button variant="tertiary" onClick={() => carregarPedidos(pagina - 1)} disabled={pagina === 0}>
              ← Anterior
            </Button>
            <span>{pagina + 1} / {totalPaginas}</span>
            <Button variant="tertiary" onClick={() => carregarPedidos(pagina + 1)} disabled={pagina >= totalPaginas - 1}>
              Próxima →
            </Button>
          </div>
        )}
      </div>
    </MainScrollContainer>
  );
};

export default MeusPedidos;
