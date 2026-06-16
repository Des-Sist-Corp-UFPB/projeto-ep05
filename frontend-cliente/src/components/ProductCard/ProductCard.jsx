import "./ProductCard.css";
import { FaPlus } from "react-icons/fa";

import { useNavigate } from "react-router-dom";

const ProductCard = ({ image, name, price, onAdd, oldPrice, id }) => {

  const navigate = useNavigate();

  return (
    <div className="product-card" onClick={() => navigate(`/details/${id}`)}>

      <div className="image-container">
        <img src={image} alt={name} />
      </div>

      <div className="product-bottom">

        <div className="card-footer">
          <span className="product-name">{name}</span>

          <button className="add-button" onClick={(e) => {
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
            <span className="old-price">
              R$ {oldPrice.toFixed(2).replace(".", ",")}
            </span>
          )}
          <span className="product-price">
            {price.toFixed(2).replace(".", ",")}
          </span>

        </div>



      </div>

    </div>
  );
}
export default ProductCard