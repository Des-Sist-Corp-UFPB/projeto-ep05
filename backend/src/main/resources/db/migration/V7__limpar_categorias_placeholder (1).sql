-- Migração V7: corrige categorias/produtos "placeholder" que ficaram no banco
-- de uma versão anterior do projeto (antes do domínio mudar para uma
-- confeitaria — "Sweet Delights"). O DatabaseSeeder só semeia categorias/
-- produtos quando as tabelas estão vazias, então bancos já semeados antes
-- da mudança de domínio ficaram com "Eletrônicos", "Livros" e "Vestuário"
-- em vez das categorias corretas de confeitaria.
--
-- Esta migration:
--   1. Remove produtos das categorias placeholder, MAS SÓ os que nunca
--      foram comprados (sem nenhum pedido_item associado) — nunca apaga
--      histórico de pedido real.
--   2. Remove as categorias placeholder que ficaram sem nenhum produto.
--   3. Garante (de forma idempotente) as categorias corretas do domínio:
--      Doces, Bolos, Tortas, Salgados, Sobremesas — e mais algumas
--      derivadas comuns em confeitaria: Cupcakes, Trufas e Bombons, Cookies.
--   4. Semeia alguns produtos de exemplo em cada categoria nova, só se
--      ainda não existirem (idempotente por nome).

DO $$
DECLARE
    produtos_sem_pedido BIGINT[];
BEGIN
    -- 1. Produtos das categorias placeholder que nunca foram pedidos
    SELECT array_agg(p.id) INTO produtos_sem_pedido
    FROM produto p
    JOIN categoria c ON c.id = p.categoria_id
    WHERE c.nome IN ('Eletrônicos', 'Livros', 'Vestuário')
      AND NOT EXISTS (SELECT 1 FROM pedido_item pi WHERE pi.produto_id = p.id);

    IF produtos_sem_pedido IS NOT NULL THEN
        DELETE FROM avaliacao WHERE produto_id = ANY(produtos_sem_pedido);
        DELETE FROM produto_imagem WHERE produto_id = ANY(produtos_sem_pedido);
        DELETE FROM produto WHERE id = ANY(produtos_sem_pedido);
    END IF;

    -- 2. Categorias placeholder que ficaram sem nenhum produto (nem os com pedido real)
    DELETE FROM categoria c
    WHERE c.nome IN ('Eletrônicos', 'Livros', 'Vestuário')
      AND NOT EXISTS (SELECT 1 FROM produto p WHERE p.categoria_id = c.id);
END $$;

-- 3. Garante que as categorias corretas do domínio existam
INSERT INTO categoria (nome, descricao)
SELECT 'Doces', 'Docinhos variados para o dia a dia e para festas'
WHERE NOT EXISTS (SELECT 1 FROM categoria WHERE nome = 'Doces');

INSERT INTO categoria (nome, descricao)
SELECT 'Bolos', 'Bolos artesanais para todas as ocasiões'
WHERE NOT EXISTS (SELECT 1 FROM categoria WHERE nome = 'Bolos');

INSERT INTO categoria (nome, descricao)
SELECT 'Tortas', 'Tortas doces geladas e assadas'
WHERE NOT EXISTS (SELECT 1 FROM categoria WHERE nome = 'Tortas');

INSERT INTO categoria (nome, descricao)
SELECT 'Salgados', 'Salgados para festas e eventos'
WHERE NOT EXISTS (SELECT 1 FROM categoria WHERE nome = 'Salgados');

INSERT INTO categoria (nome, descricao)
SELECT 'Sobremesas', 'Sobremesas individuais e taças'
WHERE NOT EXISTS (SELECT 1 FROM categoria WHERE nome = 'Sobremesas');

-- Derivadas (sugestões adicionais de confeitaria — remova se não quiser)
INSERT INTO categoria (nome, descricao)
SELECT 'Cupcakes', 'Cupcakes decorados de diversos sabores'
WHERE NOT EXISTS (SELECT 1 FROM categoria WHERE nome = 'Cupcakes');

INSERT INTO categoria (nome, descricao)
SELECT 'Trufas e Bombons', 'Trufas, bombons e chocolates artesanais'
WHERE NOT EXISTS (SELECT 1 FROM categoria WHERE nome = 'Trufas e Bombons');

INSERT INTO categoria (nome, descricao)
SELECT 'Cookies', 'Cookies artesanais recheados e tradicionais'
WHERE NOT EXISTS (SELECT 1 FROM categoria WHERE nome = 'Cookies');

-- 4. Produtos de exemplo em cada categoria nova (idempotente por nome)
INSERT INTO produto (nome, descricao, preco, categoria_id, estoque, ativo)
SELECT 'Brigadeiro Gourmet (cx 12un)', 'Caixa com 12 brigadeiros gourmet sortidos', 39.90,
       (SELECT id FROM categoria WHERE nome = 'Doces'), 30, TRUE
WHERE NOT EXISTS (SELECT 1 FROM produto WHERE nome = 'Brigadeiro Gourmet (cx 12un)');

INSERT INTO produto (nome, descricao, preco, categoria_id, estoque, ativo)
SELECT 'Paçoca Gourmet (cx 10un)', 'Caixa com 10 paçocas gourmet artesanais', 29.90,
       (SELECT id FROM categoria WHERE nome = 'Doces'), 25, TRUE
WHERE NOT EXISTS (SELECT 1 FROM produto WHERE nome = 'Paçoca Gourmet (cx 10un)');

INSERT INTO produto (nome, descricao, preco, categoria_id, estoque, ativo)
SELECT 'Bolo de Chocolate com Ninho', 'Bolo de chocolate recheado com creme de leite Ninho', 89.90,
       (SELECT id FROM categoria WHERE nome = 'Bolos'), 15, TRUE
WHERE NOT EXISTS (SELECT 1 FROM produto WHERE nome = 'Bolo de Chocolate com Ninho');

INSERT INTO produto (nome, descricao, preco, categoria_id, estoque, ativo)
SELECT 'Red Velvet', 'Bolo red velvet com cobertura de cream cheese', 99.90,
       (SELECT id FROM categoria WHERE nome = 'Bolos'), 8, TRUE
WHERE NOT EXISTS (SELECT 1 FROM produto WHERE nome = 'Red Velvet');

INSERT INTO produto (nome, descricao, preco, categoria_id, estoque, ativo)
SELECT 'Torta de Limão', 'Torta gelada de limão com merengue', 69.90,
       (SELECT id FROM categoria WHERE nome = 'Tortas'), 10, TRUE
WHERE NOT EXISTS (SELECT 1 FROM produto WHERE nome = 'Torta de Limão');

INSERT INTO produto (nome, descricao, preco, categoria_id, estoque, ativo)
SELECT 'Torta Holandesa', 'Torta gelada de chocolate com biscoito', 74.90,
       (SELECT id FROM categoria WHERE nome = 'Tortas'), 10, TRUE
WHERE NOT EXISTS (SELECT 1 FROM produto WHERE nome = 'Torta Holandesa');

INSERT INTO produto (nome, descricao, preco, categoria_id, estoque, ativo)
SELECT 'Coxinha de Frango (cx 20un)', 'Caixa com 20 coxinhas de frango congeladas', 59.90,
       (SELECT id FROM categoria WHERE nome = 'Salgados'), 20, TRUE
WHERE NOT EXISTS (SELECT 1 FROM produto WHERE nome = 'Coxinha de Frango (cx 20un)');

INSERT INTO produto (nome, descricao, preco, categoria_id, estoque, ativo)
SELECT 'Risole de Camarão (cx 15un)', 'Caixa com 15 risoles de camarão congelados', 64.90,
       (SELECT id FROM categoria WHERE nome = 'Salgados'), 20, TRUE
WHERE NOT EXISTS (SELECT 1 FROM produto WHERE nome = 'Risole de Camarão (cx 15un)');

INSERT INTO produto (nome, descricao, preco, categoria_id, estoque, ativo)
SELECT 'Mousse de Maracujá', 'Mousse individual de maracujá', 14.90,
       (SELECT id FROM categoria WHERE nome = 'Sobremesas'), 25, TRUE
WHERE NOT EXISTS (SELECT 1 FROM produto WHERE nome = 'Mousse de Maracujá');

INSERT INTO produto (nome, descricao, preco, categoria_id, estoque, ativo)
SELECT 'Pudim de Leite Condensado', 'Pudim tradicional de leite condensado', 12.90,
       (SELECT id FROM categoria WHERE nome = 'Sobremesas'), 25, TRUE
WHERE NOT EXISTS (SELECT 1 FROM produto WHERE nome = 'Pudim de Leite Condensado');

INSERT INTO produto (nome, descricao, preco, categoria_id, estoque, ativo)
SELECT 'Cupcake Red Velvet (cx 6un)', 'Caixa com 6 cupcakes de red velvet', 34.90,
       (SELECT id FROM categoria WHERE nome = 'Cupcakes'), 20, TRUE
WHERE NOT EXISTS (SELECT 1 FROM produto WHERE nome = 'Cupcake Red Velvet (cx 6un)');

INSERT INTO produto (nome, descricao, preco, categoria_id, estoque, ativo)
SELECT 'Trufas de Chocolate Belga', 'Caixa com 6 trufas artesanais de chocolate belga', 45.00,
       (SELECT id FROM categoria WHERE nome = 'Trufas e Bombons'), 40, TRUE
WHERE NOT EXISTS (SELECT 1 FROM produto WHERE nome = 'Trufas de Chocolate Belga');

INSERT INTO produto (nome, descricao, preco, categoria_id, estoque, ativo)
SELECT 'Cookie Recheado de Nutella', 'Cookie artesanal recheado com Nutella', 9.90,
       (SELECT id FROM categoria WHERE nome = 'Cookies'), 30, TRUE
WHERE NOT EXISTS (SELECT 1 FROM produto WHERE nome = 'Cookie Recheado de Nutella');
