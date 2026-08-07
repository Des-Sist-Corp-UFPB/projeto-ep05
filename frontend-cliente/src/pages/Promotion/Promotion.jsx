import ProductCard from "../../components/ProductCard/ProductCard";
import "./Promotion.css";
import { useCart } from "../../context/CartContext";
import { useEffect, useState } from "react";
import { getPromocoes } from "../../api/productApi";
import BackButton from "../../components/BackButton/BackButton";
import ScrollContainer from "../../components/ScrollContainer/ScrollContainer";
import MainScrollContainer from "../../components/MainScrollContainer/MainScrollContainer";
import Loading from "../../components/Loading/Loading";
import ErrorState from "../../components/ErrorState/ErrorState";

const Promotion = () => {
  const { addToCart } = useCart();
  const [produtos, setProdutos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);

  useEffect(() => {
    const loadData = async () => {
      try {
        setError(false);
        const data = await getPromocoes();
        setProdutos(data);
      } catch (err) {
        console.error("Erro ao buscar promoções:", err);
        setError(true);
      } finally {
        setLoading(false);
      }
    };
    loadData();
  }, []);

  if (error) return <ErrorState mensagem="Erro ao carregar promoções" onRetry={() => window.location.reload()} />;
  if (loading) return <Loading />;

  return (
    <MainScrollContainer height="calc(100vh - 40px)">
      <div className="promocoes-page">
        <div style={{ width: "100%", maxWidth: "1200px" }}>
          <BackButton variant="2" />
        </div>
        <h1>🔥 Promoções</h1>

        {produtos.length > 0 ? (
          <ScrollContainer>
            {produtos.map((produto) => (
              <ProductCard
                key={produto.id}
                id={produto.id}
                image={produto.image}
                name={produto.name}
                price={produto.price}
                onAdd={() => addToCart(produto)}
              />
            ))}
          </ScrollContainer>
        ) : (
          <p className="no-promotions">Fique atento! Novas ofertas em breve... 🍰</p>
        )}
      </div>
    </MainScrollContainer>
  );
};

export default Promotion;
