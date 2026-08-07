ALTER TABLE usuario
    ADD COLUMN IF NOT EXISTS sobrenome        VARCHAR(100),
    ADD COLUMN IF NOT EXISTS cpf              VARCHAR(14) UNIQUE,
    ADD COLUMN IF NOT EXISTS telefone         VARCHAR(15),
    ADD COLUMN IF NOT EXISTS data_nascimento  DATE;