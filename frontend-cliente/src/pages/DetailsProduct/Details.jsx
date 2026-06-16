import { useParams } from "react-router-dom";
import "./Details.css";
import Button from "../../components/Button/Button";
import { useCart } from "../../context/CartContext";
import { getProdutoById } from "../../api/productApi";
import { useState, useEffect } from "react";
import BackButton from "../../components/BackButton/BackButton";
import Loading from "../../components/Loading/Loading";
import ErrorState from "../../components/ErrorState/ErrorState";
import MainScrollContainer from "../../components/MainScrollContainer/MainScrollContainer";

const Details = () => {
  const { id } = useParams();
  const { addToCart } = useCart();
  const [produto, setProduto] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);

  useEffect(() => {
    const loadProduto = async () => {
      try {
        setError(false);
        const data = await getProdutoById(id);
        setProduto(data);
      } catch (err) {
        console.error("Erro ao buscar produto:", err);
        setError(true);
      } finally {
        setLoading(false);
      }
    };
    loadProduto();
  }, [id]);

  if (error) return <ErrorState mensagem="Erro ao carregar o produto" onRetry={() => window.location.reload()} />;
  if (loading) return <Loading />;

  return (
    <MainScrollContainer height="calc(100vh - 40px)">
      <div className="details-page">
        <div style={{ width: "100%", maxWidth: "1000px" }}>
          <BackButton variant="2" />
        </div>

        <div className="details-container">
          <div className="details-image">
            {produto.image ? (
              <img src={produto.image} alt={produto.name} />
            ) : (
              <div style={{ width: "100%", height: "300px", background: "#f0f0f0", display: "flex", alignItems: "center", justifyContent: "center", borderRadius: "12px" }}>
                <span style={{ fontSize: "64px" }}>🧁</span>
              </div>
            )}
          </div>

          <div className="details-info">
            <h1>{produto.name}</h1>

            <div className="details-meta">
              <span>⭐ {produto.rating ? produto.rating.toFixed(1) : "Sem avaliações"}</span>
              <span>📦 Estoque: {produto.stock}</span>
              {produto.category && <span>🏷️ {produto.category}</span>}
            </div>

            <p className="details-description">{produto.description}</p>

            <h2 className="details-price">R$ {produto.price.toFixed(2)}</h2>

            <div style={{ width: "100%", maxWidth: "300px" }}>
              <Button onClick={() => addToCart(produto)} disabled={produto.stock <= 0}>
                {produto.stock > 0 ? "Adicionar ao carrinho" : "Fora de estoque"}
              </Button>
            </div>
          </div>
        </div>
      </div>
    </MainScrollContainer>
  );
};

export default Details;
