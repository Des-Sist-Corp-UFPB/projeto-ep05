import { useState } from "react";
import "./ProductCard.css";
import { FaPlus } from "react-icons/fa";
import { useNavigate } from "react-router-dom";

const ProductCard = ({ image, name, price, onAdd, oldPrice, id }) => {
  const navigate = useNavigate();
  const [imgOk, setImgOk] = useState(true);

  return (
    <div className="product-card" onClick={() => navigate(`/details/${id}`)}>
      <div className="image-container">
        {image && imgOk ? (
          <img src={image} alt={name} onError={() => setImgOk(false)} />
        ) : (
          <svg className="image-placeholder" viewBox="0 0 64 64" role="img" aria-label={name}>
            <circle cx="32" cy="40" r="18" fill="#f8a3bb" />
            <path d="M16 30 Q32 8 48 30" fill="none" stroke="#833320" strokeWidth="4" strokeLinecap="round" />
            <circle cx="25" cy="36" r="2.4" fill="#833320" />
            <circle cx="39" cy="36" r="2.4" fill="#833320" />
            <circle cx="32" cy="45" r="2.4" fill="#833320" />
          </svg>
        )}
      </div>

      <div className="product-bottom">
        <div className="card-footer">
          <span className="product-name">{name}</span>
          <button
            className="add-button"
            onClick={(e) => {
              e.preventDefault();
              e.stopPropagation();
              onAdd && onAdd();
            }}
          >
            <FaPlus />
          </button>
        </div>

        <div>
          {oldPrice && (
            <span className="old-price">R$ {oldPrice.toFixed(2).replace(".", ",")}</span>
          )}
          <span className="product-price">{price.toFixed(2).replace(".", ",")}</span>
        </div>
      </div>
    </div>
  );
};
export default ProductCard;