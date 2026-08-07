-- Adiciona coluna para armazenar o motivo de cancelamento do pedido
ALTER TABLE pedido ADD COLUMN IF NOT EXISTS motivo_cancelamento VARCHAR(500);
