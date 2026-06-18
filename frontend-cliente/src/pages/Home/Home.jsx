import { useEffect, useState } from "react";
import "./Home.css";
import logo from "../../assets/logo.png";
import ProductCard from "../../components/ProductCard/ProductCard";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { useCart } from "../../context/CartContext";
import { getProdutos, getMaisVendidos, getCategorias } from "../../api/productApi";
import ScrollContainer from "../../components/ScrollContainer/ScrollContainer";
import MainScrollContainer from "../../components/MainScrollContainer/MainScrollContainer";
import Loading from "../../components/Loading/Loading";
import ErrorState from "../../components/ErrorState/ErrorState";

const Home = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const { addToCart } = useCart();

  const [produtos, setProdutos] = useState([]);
  const [maisVendidos, setMaisVendidos] = useState([]);
  const [categorias, setCategorias] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);

  useEffect(() => {
    const loadAllData = async () => {
      try {
        setError(false);
        const [resProdutos, resMaisVendidos, resCategorias] = await Promise.all([
          getProdutos(),
          getMaisVendidos(),
          getCategorias()
        ]);
        setProdutos(resProdutos);
        setMaisVendidos(resMaisVendidos);
        setCategorias(resCategorias);
      } catch (err) {
        console.error("Erro ao buscar dados:", err);
        setError(true);
      } finally {
        setLoading(false);
      }
    };
    loadAllData();
  }, []);

  useEffect(() => {
    if (!loading && location.hash === "#destaques") {
      const viewport = document.querySelector(".main-scroll-viewport");
      const section = document.getElementById("destaques");
      if (viewport && section) {
        viewport.scrollTo({ top: section.offsetTop - 20, behavior: "smooth" });
        navigate(location.pathname, { replace: true });
      }
    }
  }, [location, navigate, loading]);

  if (error) return <ErrorState mensagem="Erro ao carregar os produtos" onRetry={() => window.location.reload()} />;
  if (loading) return <Loading />;

  return (
    <MainScrollContainer height="calc(100vh - 40px)">
      <div className="container-home">
        <header className="header-home">
          <img src={logo} alt="Logo" />
          <div className="header-home-texto">
            <h1>Sweet Delights</h1>
            <p>Brownies, Cookie, Trufas e muito mais</p>
          </div>
        </header>

        <div className="home-content">
          <div className="home-inner-container">
            <h2 style={{ color: "white" }}>Popular</h2>
            <ScrollContainer>
              {produtos.map((product) => (
                <ProductCard
                  key={product.id}
                  id={product.id}
                  name={product.name}
                  price={product.price}
                  image={product.image}
                  onAdd={() => addToCart(product)}
                />
              ))}
            </ScrollContainer>
          </div>
        </div>

        <h2 className="home-texto-h2">Categorias</h2>
        <div className="home-categories">
          {categorias.slice(0, 3).map((cat, index) => (
            <React.Fragment key={cat.id}>
              <p
                onClick={() => navigate(`/loja/${cat.nome}`)}
                style={{ cursor: "pointer" }}
              >
                {cat.nome}
              </p>
              {index < 2 && <span className="divider"></span>}
            </React.Fragment>
          ))}
          <span className="divider"></span>
          <Link to="/categorias" className="link-mais">Mais...</Link>
        </div>

        <div className="home-section">
          <h2 className="home-texto-h2" id="destaques">Mais Vendidos da Semana</h2>
          <ScrollContainer>
            {maisVendidos.map((product) => (
              <ProductCard
                key={product.id}
                id={product.id}
                name={product.name}
                price={product.price}
                image={product.image}
                onAdd={() => addToCart(product)}
              />
            ))}
          </ScrollContainer>
        </div>

        <div className="home-search">
          <h1>
            Um doce é pequeno, mas a
            <br /> felicidade que traz é gigante.
          </h1>
          <p>Pesquise seu doce favorito</p>
        </div>

        <footer className="footer">
          <div className="footer-brand">
            <h2>Cantinho Doce</h2>
            <p>Doces feitos com carinho 🍰</p>
          </div>
          <div className="footer-columns">
            <div>
              <h4>Company</h4>
              <Link to="/categorias">Categorias</Link>
              <Link to="/promocao">Promoções</Link>
            </div>
            <div>
              <h4>Minha Conta</h4>
              <Link to="/perfil">Perfil</Link>
              <Link to="/carrinho">Carrinho</Link>
            </div>
            <div>
              <h4>Legal</h4>
              <Link to="/">Início</Link>
            </div>
          </div>
        </footer>
      </div>
    </MainScrollContainer>
  );
};

export default Home;
