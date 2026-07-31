import { FaUserCircle } from "react-icons/fa";
import { FaBasketShopping } from "react-icons/fa6";
import { Link, Outlet, useNavigate } from "react-router-dom";
import Background from "../../assets/img/Background.png";
import Logo from "../../assets/logo.png";
import AssistenteChat from "../AssistenteChat/AssistenteChat";
import Button from "../Button/Button";
import './LayoutPage.css';
import { GiCupcake, GiPartyPopper } from "react-icons/gi";
import { FaStar } from "react-icons/fa";


import { useAuth } from "../../context/AuthContext";
import { useCart } from "../../context/CartContext";

const LayoutPage = () => {
    const { totalItems } = useCart()
    const navigate = useNavigate();

    const { user, logout, isAuthenticated } = useAuth();


    const handleUserClick = () => {
        if (isAuthenticated) {
            logout();
            navigate("/")
        } else {
            navigate('/login')
        }

    }

    return (
        <div className="layout-page-container">

           
            <div className="layout-page-header">

                
                <div className="layout-page-logo" onClick={() => navigate("/home")}>
                    <img src={Logo} alt="Logo" />
                    <p>Sweet Delights</p>
                </div>

                
                <div className="layout-page-menu">
                    <Link to="/Categorias"><GiCupcake /> Categorias</Link>
                    <Link to="/home#destaques"><FaStar /> Destaques</Link>
                    <Link to="/promocao"><GiPartyPopper /> Promoções</Link>
                </div>

                
                <div className="layout-page-user-buttons">
                    <Button

                        onClick={handleUserClick}
                        variant="primary"
                    >
                        {isAuthenticated && user
                            ? `Sair (${user.nome})`
                            : "Login"}
                    </Button>

                    <Button icon={<FaBasketShopping />} onClick={() => navigate('/carrinho')} variant="secondary">
                        | {totalItems} Itens
                    </Button>
                    {isAuthenticated ? (
                        <Link to="/meus-pedidos" className="profile-icon-link" title="Meus Pedidos">
                            <div className="profile-icon-container">
                                📦
                            </div>
                        </Link>
                    ) : null}
                    {isAuthenticated ? (
                        <Link to="/perfil" className="profile-icon-link">
                            <div className="profile-icon-container">
                                <FaUserCircle />
                            </div>
                        </Link>
                    ) : null}

                </div>
            </div>

            
            <div className="layout-page-content">
                <Outlet />
            </div>

            
            <div className="wave-container">
                
                <img src={Background} alt="" />
            </div>

            <div className="wave-container">
                <img src={Background} alt="" />
            </div>
            {isAuthenticated && <AssistenteChat />}
        </div>
    );
}

export default LayoutPage;
