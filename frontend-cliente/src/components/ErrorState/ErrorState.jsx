import React from "react";
import "./ErrorState.css";

const ErrorState = ({ mensagem = "Ops! Algo deu errado." }) => {
    return (
        <div className="error-container">
            <span style={{ fontSize: "50px" }}>⚠️</span>
            <h2>{mensagem}</h2>
            <p>Não conseguimos conectar ao servidor. Verifique sua internet.</p>
            <button onClick={() => window.location.reload()} className="btn-retry">
                Tentar Novamente
            </button>
        </div>
    );
};

export default ErrorState;