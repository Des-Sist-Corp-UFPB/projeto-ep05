import { useParams } from "react-router-dom";
import ProductCard from "../../components/ProductCard/ProductCard";
import { useCart } from "../../context/CartContext";
import BackButton from "../../components/BackButton/BackButton";
import { getProdutos, getCategorias } from "../../api/productApi";
import { useState, useEffect } from "react";
import "./Loja.css";
import ScrollContainer from "../../components/ScrollContainer/ScrollContainer";
import Loading from "../../components/Loading/Loading";
import ErrorState from "../../components/ErrorState/ErrorState";

const Loja = () => {
  const { categoria } = useParams();
  const [produtos, setProdutos] = useState([]);
  const { addToCart } = useCart();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);

  useEffect(() => {
    const loadData = async () => {
      try {
        setError(false);

        // Busca o id da categoria pelo nome para filtrar direto na API
        const categorias = await getCategorias();
        const cat = categorias.find(
          (c) => c.nome.toLowerCase() === categoria.toLowerCase()
        );

        let data;
        if (cat) {
          data = await getProdutos(null, cat.id, 0, 50);
        } else {
          // Fallback: busca todos e filtra localmente
          data = await getProdutos(null, null, 0, 100);
          data = data.filter(
            (p) => p.category.toLowerCase() === categoria.toLowerCase()
          );
        }

        setProdutos(data);
      } catch (err) {
        console.error("Erro ao buscar dados:", err);
        setError(true);
      } finally {
        setLoading(false);
      }
    };

    loadData();
  }, [categoria]);

  if (error) return <ErrorState mensagem="Erro ao carregar os produtos" onRetry={() => window.location.reload()} />;
  if (loading) return <Loading />;

  return (
    <div className="loja-page">
      <div style={{ width: "100%", maxWidth: "1200px" }}>
        <BackButton variant="2" />
      </div>

      <h1>{categoria}</h1>

      {produtos.length > 0 ? (
        <div className="products-grid">
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
        </div>
      ) : (
        <p className="no-products">Nenhum produto encontrado nesta categoria ainda... 🧁</p>
      )}
    </div>
  );
};

export default Loja;
