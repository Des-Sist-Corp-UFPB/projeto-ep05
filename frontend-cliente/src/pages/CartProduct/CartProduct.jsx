import { useState } from "react";

import "./CartProduct.css"
import QuantityControl from "../../components/QuantityControl/QuantityControl"
import { useCart } from "../../context/CartContext";
import { useNavigate } from "react-router-dom";

import Button from "../../components/Button/Button";

import MainScrollContainer from "../../components/MainScrollContainer/MainScrollContainer";
import BackButton from "../../components/BackButton/BackButton";

function CartProduct() {
    const navigate = useNavigate()

    const { cart, removeFromCart, updateQuantity, total, clearCart } = useCart()
    const stock = 10;

    return (

        <div className="cart-background">


            <div className="cart-container">

                <MainScrollContainer height="calc(80vh - 40px)" >
                    <div style={{ width: '100%', maxWidth: '1200px' }}>
                        <BackButton variant="2" />
                    </div>

                    <h1>Carrinho</h1>
                    {cart.length === 0 ? (
                        <p className="empty-msg">Carrinho está vazio 🧁</p>
                    ) : (
                        <div>
                            {cart.map(item => (
                                <div key={item.id} className="cart-item">
                                    <img src={item.image} alt={item.image} width={80} />
                                    <h3>{item.name}</h3>


                                    <div className="cart-item-info">
                                        <p>Quantidade:</p>

                                        <QuantityControl
                                            value={item.quantity}
                                            min={0}
                                            max={stock}
                                            onChange={(newValue) =>
                                                updateQuantity(item.id, newValue)
                                            }
                                        />
                                    </div>
                                    <p className="price-tag">Preço: R$ {item.price.toFixed(2)}</p>

                                    <Button variant="secondary" onClick={() => removeFromCart(item.id)}>
                                        Remover

                                    </Button>
                                    <hr />
                                </div>
                            ))}

                            <div className="cart-summary">
                                <h2>Total: R$ {total.toFixed(2)}</h2>
                                <div className="cart-buttons">
                                    <Button onClick={clearCart} variant="tertiary">
                                        Limpar Carrinho
                                    </Button>
                                    <Button onClick={() => navigate("/address")}>
                                        Finalizar Pedido
                                    </Button>
                                </div>
                            </div>
                        </div>

                    )}

                </MainScrollContainer>


            </div>
        </div>


    );
};
export default CartProduct;
