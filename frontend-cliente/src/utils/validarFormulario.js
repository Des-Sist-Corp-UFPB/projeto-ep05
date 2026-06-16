
const validadores = {
  nome: validarNome,
  email: validarEmail,
  senha: validarSenha,
  confirmarSenha: validarConfirmacaoSenha
}


export function validarCampo(name, dados){

  const erros = {}

  const validador = validadores[name]

  if(validador){
    validador(dados, erros)
  }

  return erros[name]

}


export function validarFormulario(dados){

  const erros = {}

  Object.keys(validadores).forEach((campo)=>{
    validadores[campo](dados, erros)
  })

  return erros
}




function validarNome(dados, erros){

  if(!dados.nome){
    erros.nome = "Nome é obrigatório"
  }
  else if(dados.nome.length < 3){
    erros.nome = "Nome precisa ter pelo menos 3 letras"
  }

}

function validarEmail(dados, erros){

  if(!dados.email){
    erros.email = "Email é obrigatório"
    return
  }

  const regexEmail = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

  if(!regexEmail.test(dados.email)){
    erros.email = "Digite um email válido"
  }

}

function validarSenha(dados, erros){

  if(!dados.senha){
    erros.senha = "Senha é obrigatória"
    return
  }

  if(dados.senha.length < 8){
    erros.senha = "A senha precisa ter pelo menos 8 caracteres"
    return
  }

  if(!/[A-Z]/.test(dados.senha)){
    erros.senha = "A senha precisa ter uma letra maiúscula"
    return
  }

  if(!/[a-z]/.test(dados.senha)){
    erros.senha = "A senha precisa ter uma letra minúscula"
    return
  }

  if(!/[0-9]/.test(dados.senha)){
    erros.senha = "A senha precisa ter um número"
    return
  }

  if(!/[!@#$%^&*(),.?":{}|<>]/.test(dados.senha)){
    erros.senha = "A senha precisa ter um caractere especial"
  }

}

function validarConfirmacaoSenha(dados, erros){

  if(!dados.confirmarSenha){
    erros.confirmarSenha = "Confirme sua senha"
    return
  }

  if(dados.senha !== dados.confirmarSenha){
    erros.confirmarSenha = "As senhas não coincidem"
  }

}