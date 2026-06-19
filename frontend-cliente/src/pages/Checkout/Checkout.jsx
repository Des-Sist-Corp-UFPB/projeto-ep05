import { useState, useEffect } from "react";
import { useCart } from "../../context/CartContext";
import "./Checkout.css";
import { useNavigate } from "react-router-dom";

import Input from "../../components/Input/Input";
import Button from "../../components/Button/Button";
import BackButton from "../../components/BackButton/BackButton";
import { apiFetch } from "../../api/api";

import { formatCVV, formatCardNumber, formatExpiry, formatName } from "../../utils/validarCartao/mascara";
import { validarCampoCard, validarFormularioCard } from "../../utils/validarCartao/validarCartao";

const Checkout = () => {
  const { cart, total, clearCart } = useCart();
  const navigate = useNavigate();

  const [dados, setDados] = useState({ cardNumber: "", expiry: "", cvv: "", name: "" });
  const [cupomCodigo, setCupomCodigo] = useState("");
  const [cupomInfo, setCupomInfo] = useState(null);
  const [cupomErro, setCupomErro] = useState("");
  const [loadingCupom, setLoadingCupom] = useState(false);

  const [erros, setErros] = useState({});
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(false);
  const [apiError, setApiError] = useState("");

  const calcDesconto = () => {
    if (!cupomInfo) return 0;
    if (cupomInfo.tipo === "PORCENTAGEM") return total * (parseFloat(cupomInfo.desconto) / 100);
    if (cupomInfo.tipo === "VALOR_FIXO") return parseFloat(cupomInfo.desconto);
    return 0;
  };

  const totalComDesconto = Math.max(0, total - calcDesconto());

  useEffect(() => {
    if (success) {
      const timer = setTimeout(() => navigate("/"), 3000);
      return () => clearTimeout(timer);
    }
  }, [success]);

  function handleChange(e) {
    const { name, value } = e.target;
    let formattedValue = value;
    if (name === "cardNumber") formattedValue = formatCardNumber(value);
    if (name === "expiry") formattedValue = formatExpiry(value);
    if (name === "cvv") formattedValue = formatCVV(value);
    if (name === "name") formattedValue = formatName(value);

    const novosDados = { ...dados, [name]: formattedValue };
    setDados(novosDados);
    setErros((prev) => ({ ...prev, [name]: validarCampoCard(name, novosDados) }));
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
      // Busca o endereço principal salvo
      let enderecoId = null;
      try {
        const enderecos = await apiFetch("/clientes/enderecos");
        const principal = enderecos.find((e) => e.principal) || enderecos[0];
        if (principal) enderecoId = principal.id;
      } catch {
        // prossegue sem endereço
      }

      await apiFetch("/pedidos", {
        method: "POST",
        body: JSON.stringify({
          itens: cart.map((item) => ({ produtoId: item.id, quantidade: item.quantity })),
          enderecoId,
          cupomCodigo: cupomInfo ? cupomCodigo.trim() : null,
          formaPagamento: "CARTAO",
        }),
      });

      clearCart();
      setSuccess(true);
    } catch (err) {
      setApiError(err.mensagem || "Erro ao finalizar pedido");
      setLoading(false);
    }
  }

  if (success) {
    return (
      <div className="checkout-page">
        <div className="checkout-card payment-success">
          <h1>✅ Pedido realizado!</h1>
          <p>Seu pedido foi confirmado com sucesso 🎉</p>
        </div>
      </div>
    );
  }

  return (
    <div className="checkout-page">
      <div className="checkout-card">
        <BackButton variant="2" />
        <h1>Checkout</h1>

        <div className="checkout-total">
          {cupomInfo ? (
            <>
              <p style={{ textDecoration: "line-through", color: "#999" }}>
                Subtotal: R$ {total.toFixed(2)}
              </p>
              <p style={{ color: "green" }}>
                Desconto ({cupomInfo.tipo === "PORCENTAGEM" ? `${cupomInfo.desconto}%` : `R$ ${cupomInfo.desconto}`}): -R$ {calcDesconto().toFixed(2)}
              </p>
              <strong>Total: R$ {totalComDesconto.toFixed(2)}</strong>
            </>
          ) : (
            <strong>Total: R$ {total.toFixed(2)}</strong>
          )}
        </div>

        <div className="cupom-section" style={{ marginBottom: "16px" }}>
          <div style={{ display: "flex", gap: "8px", alignItems: "flex-end" }}>
            <Input
              label="Cupom de desconto" name="cupom" placeholder="Código do cupom"
              value={cupomCodigo}
              onChange={(e) => { setCupomCodigo(e.target.value.toUpperCase()); setCupomErro(""); setCupomInfo(null); }}
            />
            <Button type="button" variant="tertiary" onClick={handleValidarCupom} disabled={loadingCupom}>
              {loadingCupom ? "..." : "Aplicar"}
            </Button>
          </div>
          {cupomErro && <p style={{ color: "red", fontSize: "12px" }}>{cupomErro}</p>}
          {cupomInfo && <p style={{ color: "green", fontSize: "12px" }}>✅ Cupom aplicado!</p>}
        </div>

        {apiError && <p style={{ color: "red", marginBottom: "12px" }}>{apiError}</p>}

        <form onSubmit={handleSubmit} className="checkout-form">
          <Input label="Nome no cartão" name="name" placeholder="Como está no cartão"
            value={dados.name} onChange={handleChange} erro={erros.name} />
          <Input label="Número do cartão" name="cardNumber" placeholder="0000 0000 0000 0000"
            value={dados.cardNumber} onChange={handleChange} erro={erros.cardNumber} />
          <div className="input-row">
            <Input style={{ width: "135px" }} label="Validade" name="expiry" placeholder="MM/AA"
              value={dados.expiry} onChange={handleChange} erro={erros.expiry} />
            <Input style={{ width: "135px" }} label="CVV" name="cvv" placeholder="123"
              value={dados.cvv} onChange={handleChange} erro={erros.cvv} />
          </div>
          <Button variant="secondary" type="submit" disabled={loading}>
            {loading ? "Processando..." : "Confirmar Pedido"}
          </Button>
        </form>
      </div>
    </div>
  );
};

export default Checkout;
