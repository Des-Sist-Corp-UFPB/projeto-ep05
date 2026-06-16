const validadores = {
    cardNumber : validarCardNumber,
    expiry: validarExpiry,
    cvv: validarCVV,
    name: validarName
}


export function validarCampoCard(name, dados) {

    const erros = {}; // por que erros é um objeto?

    const validador = validadores[name]; // como funciona essa parte?

    if(validador){
        validador(dados,erros); // oque esse validar faz?
    }

    return erros[name];


}

export function validarFormularioCard(dados) {
    const erros = {};

    Object.keys(validadores).forEach((campo) => {
        validadores[campo](dados,erros); // preciso entender essa logica e como funciona.v principalmente o que seria esse dados?
    })
    return erros;



}





function validarCardNumber(dados, erros) {

    const numbers = dados.cardNumber?.replace(/\s/g, "") 

    if (!numbers) {
        erros.cardNumber = "Número do cartão é obrigatório";
        return;
    }
    else if (numbers.length != 16) {
        erros.cardNumber = "Cartão deve ter 16 dígitos";
        return;
    }

}

function validarExpiry(dados, erros) {
    const value = dados.expiry

    if (!value) {
        erros.expiry = "Validade é obrigatória";
    }
    else if (!/^\d{2}\/\d{2}$/.test(value)) {
       
        erros.expiry = "Formato inválido (MM/AA)";
        return;

    }

    const [month] = value.split("/") 


    if (month < 1 || month > 12) { 
        erros.expiry = "Mês inválido";
    }

}

function validarCVV(dados, erros) {
    if (!dados.cvv) {
        erros.cvv = "CVV obrigatório";
        return;
    }

    if (dados.cvv.length !== 3) {
        erros.cvv = "CVV inválido";
    }
}

function validarName(dados, erros) {
    if (!dados.name) {
        erros.name = "Nome é obrigatório";
        return;
    }

    if (dados.name.length < 3) {
        erros.name = "Nome muito curto";
    }
}