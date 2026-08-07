import Button from "../Button/Button";
import { useNavigate, useLocation } from "react-router-dom";
import "./BackButton.css"
import { FaArrowLeft } from "react-icons/fa";
 const BackButton = ({variant, className}) => {
    
    const navigate = useNavigate();
    const location = useLocation();

    const handleBack = () => {
        if (location.pathname === "/login" || location.pathname === "/cadastro") {
            if (location.state?.from) {
                navigate("/home");
                return;
            }
        }

        navigate(-1);
    };

    return (
        <Button onClick={handleBack} className={`btn-voltar btn-voltar--${variant} ${className}`} icon={<FaArrowLeft/>}>
        Voltar
        </Button>
    );
}

export default BackButton



