-- Migration repetível: garante que as tabelas do V4 e V5 existam
-- independente do histórico do Flyway no banco.
-- Este arquivo roda sempre que seu checksum mudar (ou na primeira vez).

-- V4: Tabela de recuperação de senha
CREATE TABLE IF NOT EXISTS recuperacao_senha (
    id          BIGSERIAL PRIMARY KEY,
    usuario_id  BIGINT NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    token       VARCHAR(64) NOT NULL UNIQUE,
    expira_em   TIMESTAMP WITH TIME ZONE NOT NULL,
    usado       BOOLEAN NOT NULL DEFAULT FALSE,
    criado_em   TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_recuperacao_token ON recuperacao_senha (token);

-- V5: Tabela de log de auditoria
CREATE TABLE IF NOT EXISTS log_auditoria (
    id          BIGSERIAL PRIMARY KEY,
    papel_ator  VARCHAR(20)  NOT NULL,
    ator        VARCHAR(150) NOT NULL,
    categoria   VARCHAR(50)  NOT NULL,
    descricao   VARCHAR(500) NOT NULL,
    recurso_id  BIGINT,
    resultado   VARCHAR(20)  NOT NULL DEFAULT 'SUCCESS',
    criado_em   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_log_papel  ON log_auditoria (papel_ator);
CREATE INDEX IF NOT EXISTS idx_log_criado ON log_auditoria (criado_em DESC);
CREATE INDEX IF NOT EXISTS idx_log_ator   ON log_auditoria (ator);
