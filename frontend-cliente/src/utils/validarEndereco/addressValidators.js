const validadores ={
    cep: validarCEP,
    rua: validarRua,
    numero: validarNumero,
    bairro: validarBairro,
    cidade:validarCidade,
    estado: validarEstado
}


export function validarCampoAddress(name, dados) {
    const erros = {}

    const validador = validadores[name] 
    
    if(validador){
        validador(dados,erros);
    }    
    return erros[name]

}


export function validarFormularioAddress(dados) {
     const erros = {}

     Object.keys(validadores).forEach((campo) => { 
        validadores[campo](dados,erros)
     })

     return erros;

}


function validarCEP(dados, erros) {
  if (!dados.cep) {
    erros.cep = "CEP obrigatório";
    return;
  }

  if (dados.cep.replace(/\D/g, "").length !== 8) {
    erros.cep = "CEP inválido";
  }
}


function validarRua(dados, erros) {
  if (!dados.rua) erros.rua = "Rua obrigatória";
}

function validarNumero(dados, erros) {
  if (!dados.numero) erros.numero = "Número obrigatório";
}

function validarBairro(dados, erros) {
  if (!dados.bairro) erros.bairro = "Bairro obrigatório";
}

function validarCidade(dados, erros) {
  if (!dados.cidade) erros.cidade = "Cidade obrigatória";
}

function validarEstado(dados, erros) {
  if (!dados.estado) erros.estado = "Estado obrigatório";
}