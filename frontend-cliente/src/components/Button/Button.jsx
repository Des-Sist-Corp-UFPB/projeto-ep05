// Button.jsx
import React from "react";
import "./Button.css";

const Button = ({
  children,
  variant = "primary",
  icon,
  type = "button",
  onClick,
  disabled = false,
  className = "",
}) => {
  return (
    <button
      className={`btn btn--${variant} ${className}`}
      type={type}
      onClick={onClick}
      disabled={disabled}
    >
      {icon && <span className="btn__icon">{icon}</span>}
      <span className="btn__text">{children}</span>
    </button>
  );
};

export default Button;


