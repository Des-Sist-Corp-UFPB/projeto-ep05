
export async function buscarCEP(cep) {
    const cleanCep = cep.replace(/\D/g, ""); 

    if(cleanCep.length !== 8) return null;

    try {
        const reponse= await fetch(`https://viacep.com.br/ws/${cleanCep}/json/`);
        const data = await reponse.json();

        if(data.erro)return null;

        return data
        
    } catch (error) {
        console.error("Erro ao buscar CEP:", error)
        return null
        
    }


}