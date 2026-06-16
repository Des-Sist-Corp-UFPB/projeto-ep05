import { createContext, useContext, useEffect, useMemo, useState, useCallback } from "react";

const CartContext = createContext();

export  function CartProvider({ children }) {

    const [cart, setCart] = useState(() => {
        const stored = localStorage.getItem("cart");
        return stored ? JSON.parse(stored) : []; 
    });

    useEffect(() => {
        localStorage.setItem("cart", JSON.stringify(cart));
    }, [cart]) 



  
    const addToCart = useCallback((product) => {
        setCart((prevCart) => {
            const productExists = prevCart.find(item => item.id === product.id);

            if (productExists) {
                return prevCart.map(item =>
                    item.id === product.id
                        ? { ...item, quantity: item.quantity + 1 }
                        : item
                );
            }
            return [...prevCart, { ...product, quantity: 1 }];
        });
    }, []); 

    const removeFromCart = useCallback((id) => {
        setCart((prevCart) => prevCart.filter(item => item.id !== id));
    }, []); 


    const updateQuantity = useCallback((id, newQuantity) => {
        if (newQuantity <= 0) {
            removeFromCart(id); 
            return;
        }
        setCart((prevCart) =>
            prevCart.map(item =>
                item.id === id ? { ...item, quantity: newQuantity } : item
            )
        );
    }, [removeFromCart]); 
    const clearCart = useCallback(() => {
        setCart([]);
    }, []); 


    const total = cart.reduce((acc, item) => {
        return acc + item.price * item.quantity
    }, 0);

    const totalItems = cart.reduce((acc, item) => { 
        return acc + item.quantity;
    }, 0);


    const value = useMemo(() => ({
        cart,
        addToCart,
        removeFromCart,
        updateQuantity,
        total,
        clearCart,
        totalItems
    }), [cart, addToCart, removeFromCart, updateQuantity, total, clearCart, totalItems]);

    return (
        <CartContext.Provider value={value} >{children} </CartContext.Provider>
    )

}

export function useCart() {
    const context = useContext(CartContext)
    if (!context) {
        throw new Error('useCart deve ser usado dentro de CartProvider')
    }

    return context


}



