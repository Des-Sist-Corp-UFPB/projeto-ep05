import React from "react";
import "./LayoutStripes.css";
import Background from "../../assets/img/Background.png";

const LayoutStripes = ({ image, title, children }) => {
  return (
    <div className="layout-page-container-stripes">
      
      <div className="layout-page-content-stripes">
        {image && (
          <img src={image} alt="Logo" className="layout-logo-stripes" />
        )}

        {title && <h1 className="layout-title-stripes">{title}</h1>}

        
        {children}
      </div>

      <div className="wave-container-stripes">
        <img src={Background} alt="" />
      </div>
    </div>
  );
};

export default LayoutStripes;
