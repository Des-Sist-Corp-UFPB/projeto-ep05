import React from "react";
import "./MainScrollContainer.css";

const MainScrollContainer = ({ children, height = "100vh" }) => {
  return (
    <div className="main-scroll-viewport" style={{ height }}>
      <div className="main-scroll-content">
        {children}
      </div>
    </div>
  );
};

export default MainScrollContainer;