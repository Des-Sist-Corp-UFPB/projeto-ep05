import React from "react";
import "./Loading.css";

const Loading = ({ mensagem = "Preparando suas delícias..." }) => {
    return (
        <div className="loading-container">
            <div className="spinner"></div>
            <h2>{mensagem}</h2>
            <p>O servidor está acordando, isso leva uns instantes.</p>
        </div>
    );
};

export default Loading;