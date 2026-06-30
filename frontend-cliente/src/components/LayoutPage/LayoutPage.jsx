import React from "react";
import './LayoutPage.css';
import Button from "../Button/Button";
import { FaUserCircle } from "react-icons/fa";
import { Link, Outlet, useNavigate } from "react-router-dom";
import Logo from "../../assets/logo.png";
import Background from "../../assets/img/Background.png"
import { FaBasketShopping } from "react-icons/fa6";



import { useCart } from "../../context/CartContext";
import { useAuth } from "../../context/AuthContext";

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
                    <Link to="/Categorias">Categorias</Link>
                    <Link to="/home#destaques">Destaques</Link>
                    <Link to="/promocao">Promoções</Link>
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
        </div>
    );
}

export default LayoutPage;
