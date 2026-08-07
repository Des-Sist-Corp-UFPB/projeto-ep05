const validadores = {
  nome: validarNome,
  sobrenome: validarSobrenome,
  email: validarEmail,
  cpf: validarCPF,
  telefone: validarTelefone,
  dataNascimento: validarDataNascimento,
  senha: validarSenha,
  confirmarSenha: validarConfirmacaoSenha,
};

export function validarCampo(name, dados) {
  const validador = validadores[name];
  if (!validador) return undefined;
  const erros = {};
  validador(dados, erros);
  return erros[name];
}

export function validarFormulario(dados) {
  const erros = {};
  Object.keys(validadores).forEach((campo) => {
    validadores[campo](dados, erros);
  });
  return erros;
}

// ── Validadores individuais ───────────────────────────────────────────────────

function validarNome(dados, erros) {
  if (!dados.nome) {
    erros.nome = "Nome é obrigatório";
  } else if (dados.nome.length < 2) {
    erros.nome = "Nome precisa ter pelo menos 2 letras";
  }
}

function validarSobrenome(dados, erros) {
  if (!dados.sobrenome) {
    erros.sobrenome = "Sobrenome é obrigatório";
  } else if (dados.sobrenome.length < 2) {
    erros.sobrenome = "Sobrenome precisa ter pelo menos 2 letras";
  }
}

function validarEmail(dados, erros) {
  if (!dados.email) {
    erros.email = "E-mail é obrigatório";
    return;
  }
  const regexEmail = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!regexEmail.test(dados.email)) {
    erros.email = "Digite um e-mail válido";
  }
}

function validarCPF(dados, erros) {
  if (!dados.cpf) {
    erros.cpf = "CPF é obrigatório";
    return;
  }
  const regexCPF = /^\d{3}\.\d{3}\.\d{3}-\d{2}$/;
  if (!regexCPF.test(dados.cpf)) {
    erros.cpf = "CPF inválido. Use o formato 000.000.000-00";
  }
}

function validarTelefone(dados, erros) {
  if (!dados.telefone) {
    erros.telefone = "Telefone é obrigatório";
    return;
  }
  const regexTelefone = /^\(\d{2}\)\s?\d{4,5}-\d{4}$/;
  if (!regexTelefone.test(dados.telefone)) {
    erros.telefone = "Telefone inválido. Use (00) 00000-0000";
  }
}

function validarDataNascimento(dados, erros) {
  if (!dados.dataNascimento) {
    erros.dataNascimento = "Data de nascimento é obrigatória";
    return;
  }
  const nascimento = new Date(dados.dataNascimento);
  const hoje = new Date();
  if (nascimento >= hoje) {
    erros.dataNascimento = "A data de nascimento deve ser no passado";
  }
}

function validarSenha(dados, erros) {
  if (!dados.senha) {
    erros.senha = "Senha é obrigatória";
    return;
  }
  if (dados.senha.length < 8) {
    erros.senha = "A senha precisa ter pelo menos 8 caracteres";
    return;
  }
  if (!/[A-Z]/.test(dados.senha)) {
    erros.senha = "A senha precisa ter uma letra maiúscula";
    return;
  }
  if (!/[a-z]/.test(dados.senha)) {
    erros.senha = "A senha precisa ter uma letra minúscula";
    return;
  }
  if (!/[0-9]/.test(dados.senha)) {
    erros.senha = "A senha precisa ter um número";
  }
}

function validarConfirmacaoSenha(dados, erros) {
  if (!dados.confirmarSenha) {
    erros.confirmarSenha = "Confirme sua senha";
    return;
  }
  if (dados.senha !== dados.confirmarSenha) {
    erros.confirmarSenha = "As senhas não coincidem";
  }
}
