-- Migração V5: Tabela de Log de Auditoria

CREATE TABLE log_auditoria (
    id          BIGSERIAL PRIMARY KEY,
    papel_ator  VARCHAR(20)  NOT NULL,               -- SYSADMIN | ADMIN | CLIENTE | SYSTEM
    ator        VARCHAR(150) NOT NULL,               -- email ou "SYSTEM"
    categoria   VARCHAR(50)  NOT NULL,               -- USER_MGMT | PRODUTO | PEDIDO | AUTH | SYSTEM
    descricao   VARCHAR(500) NOT NULL,
    recurso_id  BIGINT,                              -- id do recurso afetado (opcional)
    resultado   VARCHAR(20)  NOT NULL DEFAULT 'SUCCESS', -- SUCCESS | FAILURE
    criado_em   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_log_papel  ON log_auditoria (papel_ator);
CREATE INDEX idx_log_criado ON log_auditoria (criado_em DESC);
CREATE INDEX idx_log_ator   ON log_auditoria (ator);
