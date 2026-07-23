import React from "react";
import "./ErrorState.css";

// FIX #4: componente agora respeita a prop onRetry passada pelo pai
const ErrorState = ({ mensagem = "Ops! Algo deu errado.", onRetry }) => {
    const handleRetry = onRetry ?? (() => window.location.reload());

    return (
        <div className="error-container">
            <span style={{ fontSize: "50px" }}>⚠️</span>
            <h2>{mensagem}</h2>
            <p>Não conseguimos conectar ao servidor. Verifique sua internet.</p>
            <button onClick={handleRetry} className="btn-retry">
                Tentar Novamente
            </button>
        </div>
    );
};

export default ErrorState;
