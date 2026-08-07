-- Migração V8: cria a tabela configuracao_sistema, usada para armazenar
-- parâmetros globais da plataforma (chave/valor) editáveis pelo SysAdmin
-- na tela de Configurações do Sistema (ex.: nome da loja, e-mail de contato).

CREATE TABLE IF NOT EXISTS configuracao_sistema (
    chave         VARCHAR(100) PRIMARY KEY,
    valor         VARCHAR(500),
    atualizado_em TIMESTAMP NOT NULL DEFAULT now()
);

INSERT INTO configuracao_sistema (chave, valor)
SELECT 'nome_plataforma', 'Sweet Delights Manager'
WHERE NOT EXISTS (SELECT 1 FROM configuracao_sistema WHERE chave = 'nome_plataforma');

INSERT INTO configuracao_sistema (chave, valor)
SELECT 'email_contato', 'contato@sweetdelights.com'
WHERE NOT EXISTS (SELECT 1 FROM configuracao_sistema WHERE chave = 'email_contato');
