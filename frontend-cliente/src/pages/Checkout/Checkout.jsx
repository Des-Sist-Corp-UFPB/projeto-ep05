import { useState, useEffect, useRef } from "react";
import { useCart } from "../../context/CartContext";
import "./Checkout.css";
import { useNavigate } from "react-router-dom";

import Input from "../../components/Input/Input";
import Button from "../../components/Button/Button";
import BackButton from "../../components/BackButton/BackButton";
import { apiFetch } from "../../api/api";

import {
  formatCVV,
  formatCardNumber,
  formatExpiry,
  formatName,
} from "../../utils/validarCartao/mascara";
import {
  validarCampoCard,
  validarFormularioCard,
} from "../../utils/validarCartao/validarCartao";

// ── Utilitários de bandeira ───────────────────────────────────────────────────

function detectarBandeira(numero) {
  const n = numero.replace(/\s/g, "");
  if (/^4/.test(n)) return "VISA";
  if (/^5[1-5]/.test(n) || /^2(2[2-9][1-9]|[3-6]\d{2}|7[01]\d|720)/.test(n))
    return "MASTERCARD";
  if (/^3[47]/.test(n)) return "AMEX";
  if (/^6(?:011|5)/.test(n)) return "ELO";
  return null;
}

const LOGOS_BANDEIRA = {
  VISA: "https://upload.wikimedia.org/wikipedia/commons/4/41/Visa_Logo.png",
  MASTERCARD:
    "https://upload.wikimedia.org/wikipedia/commons/2/2a/Mastercard-logo.svg",
  AMEX: "https://upload.wikimedia.org/wikipedia/commons/3/30/American_Express_logo.svg",
  ELO: "https://upload.wikimedia.org/wikipedia/commons/thumb/9/9f/Elo_card_association_logo.svg/1200px-Elo_card_association_logo.svg.png",
};

// ── Passos do checkout ────────────────────────────────────────────────────────
const PASSOS = ["Resumo", "Pagamento", "Confirmação"];

// ── Componente principal ──────────────────────────────────────────────────────

const Checkout = () => {
  const { cart, total, clearCart } = useCart();
  const navigate = useNavigate();

  const [passo, setPasso] = useState(0); // 0=Resumo, 1=Pagamento, 2=Confirmação
  const [pedidoId, setPedidoId] = useState(null);
  const [dados, setDados] = useState({
    cardNumber: "",
    expiry: "",
    cvv: "",
    name: "",
  });
  const [cvvFocus, setCvvFocus] = useState(false);
  const [cupomCodigo, setCupomCodigo] = useState("");
  const [cupomInfo, setCupomInfo] = useState(null);
  const [cupomErro, setCupomErro] = useState("");
  const [loadingCupom, setLoadingCupom] = useState(false);

  const [erros, setErros] = useState({});
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(false);
  const [apiError, setApiError] = useState("");

  const bandeira = detectarBandeira(dados.cardNumber);

  // ── Cálculo de desconto ─────────────────────────────────────────────────────

  const calcDesconto = () => {
    if (!cupomInfo) return 0;
    if (cupomInfo.tipo === "PORCENTAGEM")
      return total * (parseFloat(cupomInfo.desconto) / 100);
    if (cupomInfo.tipo === "VALOR_FIXO") return parseFloat(cupomInfo.desconto);
    return 0;
  };

  const totalComDesconto = Math.max(0, total - calcDesconto());

  // ── Redirect após sucesso ───────────────────────────────────────────────────

  useEffect(() => {
    if (success) {
      const timer = setTimeout(() => navigate("/meus-pedidos"), 3500);
      return () => clearTimeout(timer);
    }
  }, [success]);

  // ── Handlers ────────────────────────────────────────────────────────────────

  function handleChange(e) {
    const { name, value } = e.target;
    let formattedValue = value;
    if (name === "cardNumber") formattedValue = formatCardNumber(value);
    if (name === "expiry") formattedValue = formatExpiry(value);
    if (name === "cvv") formattedValue = formatCVV(value);
    if (name === "name") formattedValue = formatName(value);

    const novosDados = { ...dados, [name]: formattedValue };
    setDados(novosDados);
    setErros((prev) => ({
      ...prev,
      [name]: validarCampoCard(name, novosDados),
    }));
  }

  async function handleValidarCupom() {
    if (!cupomCodigo.trim()) return;
    setLoadingCupom(true);
    setCupomErro("");
    setCupomInfo(null);
    try {
      const data = await apiFetch(`/pedidos/cupom/${cupomCodigo.trim()}`);
      setCupomInfo(data);
    } catch (err) {
      setCupomErro(err.mensagem || "Cupom inválido");
    } finally {
      setLoadingCupom(false);
    }
  }

  async function handleSubmit(e) {
    e.preventDefault();

    const errosValidados = validarFormularioCard(dados);
    if (Object.keys(errosValidados).length > 0) {
      setErros(errosValidados);
      return;
    }

    if (cart.length === 0) {
      setApiError("Seu carrinho está vazio.");
      return;
    }

    setLoading(true);
    setApiError("");

    try {
      // Passo 1: buscar endereço principal
      const enderecos = await apiFetch("/clientes/enderecos");
      const principal = enderecos?.find((e) => e.principal) || enderecos?.[0];
      if (!principal) {
        setApiError("Cadastre um endereço de entrega antes de finalizar.");
        setLoading(false);
        return;
      }

      // Passo 2: cadastrar cartão no backend (backend tokeniza via Mercado Pago)
      const numeroLimpo = dados.cardNumber.replace(/\s/g, "");
      const bandeiraFinal = bandeira || "OUTRO";

      // Converter validade de MM/AA para MM/AAAA
      const [mes, anoAbrev] = dados.expiry.split("/");
      const anoCompleto =
        anoAbrev && anoAbrev.length === 2 ? `20${anoAbrev}` : anoAbrev;
      const dataExpiracao = `${mes}/${anoCompleto}`;

      const cartaoSalvo = await apiFetch("/clientes/cartoes", {
        method: "POST",
        body: JSON.stringify({
          nomeTitular: dados.name,
          numeroCartao: numeroLimpo,
          bandeira: bandeiraFinal,
          dataExpiracao,
          cvv: dados.cvv,
        }),
      });

      // Passo 3: criar pedido com o cartaoId retornado pelo backend
      const pedidoCriado = await apiFetch("/pedidos", {
        method: "POST",
        body: JSON.stringify({
          itens: cart.map((item) => ({
            produtoId: item.id,
            quantidade: item.quantity,
          })),
          enderecoId: principal.id,
          cartaoId: cartaoSalvo.id,
          codigoCupom: cupomInfo ? cupomCodigo.trim() : null,
        }),
      });

      clearCart();
      setPedidoId(pedidoCriado?.id || null);
      setSuccess(true);
    } catch (err) {
      setApiError(err.mensagem || "Erro ao finalizar pedido. Tente novamente.");
      setLoading(false);
    }
  }

  // ── Tela de sucesso ──────────────────────────────────────────────────────────

  if (success) {
    return (
      <div className="checkout-page">
        <div className="checkout-card payment-success">
          <div className="success-icon">✅</div>
          <h1>Pedido confirmado!</h1>
          <p>Pagamento processado com sucesso pelo Mercado Pago 🎉</p>
          {pedidoId && <p className="success-pedido-id">Pedido #{pedidoId}</p>}
          <p className="success-redirect">Redirecionando para seus pedidos...</p>
        </div>
      </div>
    );
  }

  // ── Layout principal ─────────────────────────────────────────────────────────

  return (
    <div className="checkout-page">
      <div className="checkout-card">
        <BackButton variant="2" />
        <h1>Checkout</h1>

        {/* Stepper */}
        <div className="checkout-stepper">
          {PASSOS.map((label, i) => (
            <div key={label} className="stepper-item">
              <div className={`stepper-dot ${i <= passo ? "ativo" : ""}`}>
                {i < passo ? "✓" : i + 1}
              </div>
              <span className={`stepper-label ${i === passo ? "ativo" : ""}`}>
                {label}
              </span>
            </div>
          ))}
        </div>

        {/* ── PASSO 0: Resumo do pedido ── */}
        {passo === 0 && (
          <div className="checkout-resumo">
            <div className="resumo-itens">
              {cart.map((item) => (
                <div key={item.id} className="resumo-item">
                  <span className="resumo-nome">{item.nome || item.name}</span>
                  <span className="resumo-qty">x{item.quantity}</span>
                  <span className="resumo-preco">
                    R${" "}
                    {(
                      (item.preco || item.price || 0) * item.quantity
                    ).toFixed(2)}
                  </span>
                </div>
              ))}
            </div>

            {/* Cupom */}
            <div className="cupom-section">
              <div className="cupom-row">
                <Input
                  label="Cupom de desconto"
                  name="cupom"
                  placeholder="Código do cupom"
                  value={cupomCodigo}
                  onChange={(e) => {
                    setCupomCodigo(e.target.value.toUpperCase());
                    setCupomErro("");
                    setCupomInfo(null);
                  }}
                />
                <Button
                  type="button"
                  variant="tertiary"
                  onClick={handleValidarCupom}
                  disabled={loadingCupom}
                >
                  {loadingCupom ? "..." : "Aplicar"}
                </Button>
              </div>
              {cupomErro && <p className="cupom-erro">{cupomErro}</p>}
              {cupomInfo && (
                <p className="cupom-ok">
                  ✅ Cupom aplicado! Desconto de{" "}
                  {cupomInfo.tipo === "PORCENTAGEM"
                    ? `${cupomInfo.desconto}%`
                    : `R$ ${cupomInfo.desconto}`}
                </p>
              )}
            </div>

            {/* Total */}
            <div className="checkout-total">
              {cupomInfo ? (
                <>
                  <p className="total-riscado">
                    Subtotal: R$ {total.toFixed(2)}
                  </p>
                  <p className="total-desconto">
                    Desconto: - R$ {calcDesconto().toFixed(2)}
                  </p>
                  <strong>Total: R$ {totalComDesconto.toFixed(2)}</strong>
                </>
              ) : (
                <strong>Total: R$ {total.toFixed(2)}</strong>
              )}
            </div>

            <Button
              variant="secondary"
              type="button"
              onClick={() => setPasso(1)}
              disabled={cart.length === 0}
            >
              Ir para Pagamento →
            </Button>
          </div>
        )}

        {/* ── PASSO 1: Dados do Cartão ── */}
        {passo === 1 && (
          <div>
            {/* Card visual */}
            <div className={`card-visual ${cvvFocus ? "virado" : ""}`}>
              <div className="card-front">
                <div className="card-chip" />
                <div className="card-number-display">
                  {dados.cardNumber || "•••• •••• •••• ••••"}
                </div>
                <div className="card-bottom">
                  <div>
                    <span className="card-label">Titular</span>
                    <span className="card-value">
                      {dados.name.toUpperCase() || "SEU NOME"}
                    </span>
                  </div>
                  <div>
                    <span className="card-label">Validade</span>
                    <span className="card-value">{dados.expiry || "MM/AA"}</span>
                  </div>
                  {bandeira && LOGOS_BANDEIRA[bandeira] && (
                    <img
                      src={LOGOS_BANDEIRA[bandeira]}
                      alt={bandeira}
                      className="card-bandeira-logo"
                    />
                  )}
                </div>
              </div>
              <div className="card-back">
                <div className="card-tarja" />
                <div className="card-cvv-strip">
                  <span className="card-label">CVV</span>
                  <span className="card-cvv-value">
                    {dados.cvv || "•••"}
                  </span>
                </div>
              </div>
            </div>

            {/* Badge Mercado Pago */}
            <div className="mp-badge">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5" stroke="#009EE3" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
              </svg>
              Pagamento seguro via <strong>Mercado Pago</strong>
            </div>

            {apiError && <p className="api-error">{apiError}</p>}

            <form onSubmit={handleSubmit} className="checkout-form">
              <Input
                label="Nome no cartão"
                name="name"
                placeholder="Como está impresso no cartão"
                value={dados.name}
                onChange={handleChange}
                erro={erros.name}
              />
              <div className="card-number-wrapper">
                <Input
                  label="Número do cartão"
                  name="cardNumber"
                  placeholder="0000 0000 0000 0000"
                  value={dados.cardNumber}
                  onChange={handleChange}
                  erro={erros.cardNumber}
                  maxLength={19}
                />
                {bandeira && LOGOS_BANDEIRA[bandeira] && (
                  <img
                    src={LOGOS_BANDEIRA[bandeira]}
                    alt={bandeira}
                    className="bandeira-inline"
                  />
                )}
              </div>

              <div className="input-row">
                <Input
                  label="Validade"
                  name="expiry"
                  placeholder="MM/AA"
                  value={dados.expiry}
                  onChange={handleChange}
                  erro={erros.expiry}
                  maxLength={5}
                />
                <Input
                  label="CVV"
                  name="cvv"
                  placeholder="123"
                  value={dados.cvv}
                  onChange={handleChange}
                  erro={erros.cvv}
                  maxLength={4}
                  onFocus={() => setCvvFocus(true)}
                  onBlur={() => setCvvFocus(false)}
                />
              </div>

              <div className="checkout-actions">
                <Button
                  type="button"
                  variant="tertiary"
                  onClick={() => { setPasso(0); setApiError(""); }}
                >
                  ← Voltar
                </Button>
                <Button variant="secondary" type="submit" disabled={loading}>
                  {loading ? (
                    <span className="loading-spinner">
                      <span className="spinner" /> Processando...
                    </span>
                  ) : (
                    "Confirmar Pedido"
                  )}
                </Button>
              </div>
            </form>
          </div>
        )}
      </div>
    </div>
  );
};

export default Checkout;
