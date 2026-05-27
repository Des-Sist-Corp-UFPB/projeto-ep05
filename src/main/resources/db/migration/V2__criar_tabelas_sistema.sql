-- Migração V2: Criação das tabelas do Marketplace

-- 1. Tabela de Categorias
CREATE TABLE categoria (
    id          BIGSERIAL PRIMARY KEY,
    nome        VARCHAR(100) NOT NULL UNIQUE,
    descricao   VARCHAR(255),
    criado_em   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- 2. Tabela de Usuários (SYSADMIN, ADMIN, CLIENTE)
CREATE TABLE usuario (
    id            BIGSERIAL PRIMARY KEY,
    nome          VARCHAR(150) NOT NULL,
    email         VARCHAR(150) NOT NULL UNIQUE,
    senha         VARCHAR(255) NOT NULL,
    papel         VARCHAR(20) NOT NULL CHECK (papel IN ('SYSADMIN', 'ADMIN', 'CLIENTE')),
    status        VARCHAR(20) NOT NULL CHECK (status IN ('ATIVO', 'BLOQUEADO')) DEFAULT 'ATIVO',
    criado_em     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- 3. Alteração da tabela de Produto para adicionar categoria, estoque e status ativo/inativo
ALTER TABLE produto ADD COLUMN categoria_id BIGINT REFERENCES categoria(id) ON DELETE SET NULL;
ALTER TABLE produto ADD COLUMN estoque INT NOT NULL DEFAULT 0 CHECK (estoque >= 0);
ALTER TABLE produto ADD COLUMN ativo BOOLEAN NOT NULL DEFAULT TRUE;

-- 4. Tabela de Imagens de Produtos (um produto pode ter várias imagens)
CREATE TABLE produto_imagem (
    id          BIGSERIAL PRIMARY KEY,
    produto_id  BIGINT NOT NULL REFERENCES produto(id) ON DELETE CASCADE,
    url         VARCHAR(500) NOT NULL,
    criado_em   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- 5. Tabela de Endereços dos Clientes
CREATE TABLE endereco (
    id            BIGSERIAL PRIMARY KEY,
    cliente_id    BIGINT NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    logradouro    VARCHAR(150) NOT NULL,
    numero        VARCHAR(20) NOT NULL,
    complemento   VARCHAR(100),
    bairro        VARCHAR(100) NOT NULL,
    cidade        VARCHAR(100) NOT NULL,
    estado        VARCHAR(2) NOT NULL,
    cep           VARCHAR(10) NOT NULL,
    principal     BOOLEAN NOT NULL DEFAULT FALSE,
    criado_em     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- 6. Tabela de Cartões (Tokenizados/Mockados para segurança)
CREATE TABLE cartao (
    id                     BIGSERIAL PRIMARY KEY,
    cliente_id             BIGINT NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    nome_titular           VARCHAR(150) NOT NULL,
    bandeira               VARCHAR(50) NOT NULL,
    quatro_ultimos_digitos VARCHAR(4) NOT NULL,
    token_pagamento        VARCHAR(255) NOT NULL,
    data_expiracao         VARCHAR(7) NOT NULL, -- formato MM/AAAA
    criado_em              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- 7. Tabela de Cupons de Desconto
CREATE TABLE cupom (
    id             BIGSERIAL PRIMARY KEY,
    codigo         VARCHAR(50) NOT NULL UNIQUE,
    desconto       NUMERIC(10, 2) NOT NULL CHECK (desconto > 0),
    tipo           VARCHAR(20) NOT NULL CHECK (tipo IN ('PORCENTAGEM', 'VALOR_FIXO')),
    data_expiracao TIMESTAMP WITH TIME ZONE NOT NULL,
    ativo          BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- 8. Tabela de Avaliações de Produtos
CREATE TABLE avaliacao (
    id          BIGSERIAL PRIMARY KEY,
    produto_id  BIGINT NOT NULL REFERENCES produto(id) ON DELETE CASCADE,
    cliente_id  BIGINT NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    nota        INT NOT NULL CHECK (nota >= 1 AND nota <= 5),
    comentario  TEXT,
    criado_em   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE(produto_id, cliente_id) -- Impede que um cliente avalie o mesmo produto mais de uma vez
);

-- 9. Tabela de Pedidos
CREATE TABLE pedido (
    id                   BIGSERIAL PRIMARY KEY,
    cliente_id           BIGINT NOT NULL REFERENCES usuario(id) ON DELETE RESTRICT,
    endereco_entrega_id  BIGINT NOT NULL REFERENCES endereco(id) ON DELETE RESTRICT,
    cartao_id            BIGINT REFERENCES cartao(id) ON DELETE SET NULL,
    cupom_id             BIGINT REFERENCES cupom(id) ON DELETE SET NULL,
    status               VARCHAR(30) NOT NULL CHECK (status IN ('AGUARDANDO_PAGAMENTO', 'PAGO', 'ENVIADO', 'ENTREGUE', 'CANCELADO')) DEFAULT 'AGUARDANDO_PAGAMENTO',
    total_produtos       NUMERIC(10, 2) NOT NULL CHECK (total_produtos >= 0),
    total_desconto       NUMERIC(10, 2) NOT NULL DEFAULT 0.00 CHECK (total_desconto >= 0),
    total_geral          NUMERIC(10, 2) NOT NULL CHECK (total_geral >= 0),
    codigo_rastreamento  VARCHAR(100),
    criado_em            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    atualizado_em        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- 10. Tabela de Itens de Pedido
CREATE TABLE pedido_item (
    id             BIGSERIAL PRIMARY KEY,
    pedido_id      BIGINT NOT NULL REFERENCES pedido(id) ON DELETE CASCADE,
    produto_id     BIGINT NOT NULL REFERENCES produto(id) ON DELETE RESTRICT,
    quantidade     INT NOT NULL CHECK (quantidade > 0),
    preco_unitario NUMERIC(10, 2) NOT NULL CHECK (preco_unitario >= 0)
);

-- Índices adicionais para performance
CREATE INDEX idx_usuario_email ON usuario(email);
CREATE INDEX idx_produto_categoria ON produto(categoria_id);
CREATE INDEX idx_pedido_cliente ON pedido(cliente_id);
CREATE INDEX idx_pedido_item_pedido ON pedido_item(pedido_id);
CREATE INDEX idx_cupom_codigo ON cupom(codigo);
