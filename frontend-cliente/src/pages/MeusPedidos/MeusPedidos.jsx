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

function calcProgresso(status) {
  const idx = ETAPAS.indexOf(status);
  if (status === "CANCELADO") return -1;
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
  const progresso = calcProgresso(pedido.status);
  const cancelado = pedido.status === "CANCELADO";
  const info = STATUS_INFO[pedido.status] || {};

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

      <div className={`pedido-status-badge ${info.cor || ""}`}>
        {info.emoji} {info.label}
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

function CardPedido({ pedido }) {
  const [aberto, setAberto] = useState(false);

  return (
    <div className="pedido-card">
      <div className="pedido-card-header" onClick={() => setAberto(!aberto)}>
        <div className="pedido-card-info">
          <span className="pedido-numero">Pedido #{pedido.id}</span>
          <span className="pedido-data">{formatarData(pedido.criadoEm)}</span>
        </div>
        <div className="pedido-card-direita">
          <span className={`pedido-status-mini ${STATUS_INFO[pedido.status]?.cor || ""}`}>
            {STATUS_INFO[pedido.status]?.emoji} {STATUS_INFO[pedido.status]?.label}
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
        </div>
      )}
    </div>
  );
}

// ── Página principal ──────────────────────────────────────────────────────────

const MeusPedidos = () => {
  const navigate = useNavigate();
  const [pedidos, setPedidos] = useState([]);
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
          <CardPedido key={pedido.id} pedido={pedido} />
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
