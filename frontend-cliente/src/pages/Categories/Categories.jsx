import { useNavigate, } from "react-router-dom";
import "./Categories.css";

import { useState, useEffect } from "react";
import {  getCategorias } from "../../api/productApi";
import BackButton from "../../components/BackButton/BackButton";


import MainScrollContainer from "../../components/MainScrollContainer/MainScrollContainer";
import Loading from "../../components/Loading/Loading";
import ErrorState from "../../components/ErrorState/ErrorState";

const Categories = () => {

    const [categorias, setCategorias]= useState([])

    const navigate = useNavigate();

    const [ loading,setLoading]= useState(true);
     const [error, setError] = useState(false)


    useEffect(()=>{

        const loadAllData = async()=>{
            try {
                setError(false)
                const[resCategorias]= await Promise.all([
                    getCategorias()
                ]);
                setCategorias(resCategorias)
                
            } catch (error) {
                console.error("Erro ao buscar dados:", error);
                setError(true)
                
            } finally{
                setLoading(false)
            }

        }
        loadAllData();

       
    },[])

    if(error){
        return(
            <ErrorState
             mensagem="Erro ao carregar os doces" 
            onRetry={() => window.location.reload()} />
        )
    }

    if(loading){
        return <Loading/>
    } 

   

return(
    <div className="categorias-page">
        <MainScrollContainer height="calc(80vh - 40px)" >
        <div style={{ width: '100%', maxWidth: '1000px' }}>
            <BackButton variant="2"/>
        </div>
        
        <h1>Categorias</h1>

        <div className="categorias-grid">

            {categorias.map((cat)=>(
                <div

                key= {cat.id}
                className="categoria-card"
                onClick={() => navigate(`/loja/${cat.nome}`)}
                >
                    <img src={cat.imagem} alt={cat.nome}/>
                    <p>{cat.nome}</p>

                </div>

            ))}

        </div>
        </MainScrollContainer>


    </div>
)

}

export default Categories