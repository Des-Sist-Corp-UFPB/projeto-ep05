import React from "react";
import { BrowserRouter as Router, Routes, Route } from "react-router-dom";

import Login from "../pages/Login/Login";
import Register from "../pages/Register/Register";
import ForgotPassword from "../pages/ForgotPassword/ForgotPassword";
import Home from "../pages/Home/Home";
import Details from "../pages/DetailsProduct/Details";
import CartProduct from "../pages/CartProduct/CartProduct";
import Checkout from "../pages/Checkout/Checkout";
import Address from "../pages/Addresses/Address";
import Promotion from "../pages/Promotion/Promotion";
import Profile from "../pages/Profile/Profile";
import Categories from "../pages/Categories/Categories";
import Loja from "../pages/Loja/Loja";
import MeusPedidos from "../pages/MeusPedidos/MeusPedidos";

import LayoutPage from "../components/LayoutPage/LayoutPage";
import PrivateRoute from "../components/PrivateRoute/PrivateRoute";
import { CartProvider } from "../context/CartContext";
import { AuthProvider } from "../context/AuthContext";

/**
 * Rotas da aplicação.
 *
 * MUDANÇAS:
 * ─────────
 * 1. Adicionada rota /recuperar-senha → <ForgotPassword />
 * 2. Imports organizados em grupos (auth, público, privado, layout).
 * 3. Removidas linhas em branco e espaçamentos excessivos.
 */
const AppRoutes = () => (
  <Router>
    <AuthProvider>
      <CartProvider>
        <Routes>
          {/* ── Autenticação (sem layout) ─────────────────────────── */}
          <Route path="/login" element={<Login />} />
          <Route path="/cadastro" element={<Register />} />
          <Route path="/recuperar-senha" element={<ForgotPassword />} />

          {/* ── Páginas com layout ────────────────────────────────── */}
          <Route element={<LayoutPage />}>
            {/* Públicas */}
            <Route path="/" element={<Home />} />
            <Route path="/home" element={<Home />} />
            <Route path="/promocao" element={<Promotion />} />
            <Route path="/details/:id" element={<Details />} />
            <Route path="/carrinho" element={<CartProduct />} />
            <Route path="/categorias" element={<Categories />} />
            <Route path="/loja/:categoria" element={<Loja />} />

            {/* Privadas (requerem login) */}
            <Route path="/perfil" element={
              <PrivateRoute><Profile /></PrivateRoute>
            } />
            <Route path="/meus-pedidos" element={
              <PrivateRoute><MeusPedidos /></PrivateRoute>
            } />
            <Route path="/checkout" element={
              <PrivateRoute><Checkout /></PrivateRoute>
            } />
            <Route path="/address" element={
              <PrivateRoute><Address /></PrivateRoute>
            } />
          </Route>
        </Routes>
      </CartProvider>
    </AuthProvider>
  </Router>
);

export default AppRoutes;
