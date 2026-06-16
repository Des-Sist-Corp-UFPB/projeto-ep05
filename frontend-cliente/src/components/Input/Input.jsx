import React from "react";
import "./Input.css";

const Input = ({ label, erro, ...props }) => {
  return (
    <div className="input-wrapper">

      {label && (
        <label className="input-label" htmlFor={props.id}>
          {label}
        </label>
      )}

      <input
        {...props} 
        className={` input-field ${erro ? "input-error" : ""} ${props.readOnly ? "input-readonly" : ""}
  `}
      />

      {erro && (
        <p className="error-message">
          {erro}
        </p>
      )}

    </div>
  );
};

export default Input;