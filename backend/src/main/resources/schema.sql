-- Esquema do banco embarcado SQLite.
--
-- Os PRAGMAs de journal_mode, synchronous, busy_timeout e transaction_mode (FDD-001, D-A7) são
-- configurados programaticamente em infra.persistencia.PersistenciaConfig, não aqui.
--
-- Representação exata (ADR-001 e D-A2): dinheiro em centavos, preço em unidades de 10^-8 e
-- quantidade em unidades de 10^-2, todos INTEGER. Nenhuma coluna monetária usa NUMERIC ou REAL.
-- Datas são texto ISO YYYY-MM-DD, cuja ordem lexicográfica coincide com a cronológica.

CREATE TABLE IF NOT EXISTS usuario (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    login TEXT NOT NULL UNIQUE,
    senha_hash TEXT,
    administrador INTEGER NOT NULL DEFAULT 0 CHECK (administrador IN (0, 1))
);

-- O ativo não tem coluna de preço (D-A1): o preço vive em valor_mercado, sempre datado.
CREATE TABLE IF NOT EXISTS ativo (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    codigo TEXT NOT NULL UNIQUE,
    nome TEXT NOT NULL,
    tipo TEXT NOT NULL CHECK (tipo IN ('RV', 'RF', 'FUNDO')),
    data_emissao TEXT NOT NULL,
    data_vencimento TEXT NOT NULL,
    CHECK (data_emissao GLOB '[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]'),
    CHECK (data_vencimento GLOB '[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]'),
    CHECK (data_emissao < data_vencimento)
);

CREATE TABLE IF NOT EXISTS valor_mercado (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    ativo_id INTEGER NOT NULL REFERENCES ativo(id) ON DELETE CASCADE,
    data TEXT NOT NULL,
    preco_mercado_e8 INTEGER NOT NULL CHECK (preco_mercado_e8 >= 0),
    CHECK (data GLOB '[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]')
);

CREATE TABLE IF NOT EXISTS lancamento (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    usuario_id INTEGER NOT NULL REFERENCES usuario(id),
    data TEXT NOT NULL,
    valor_centavos INTEGER NOT NULL,
    descricao TEXT NOT NULL,
    CHECK (data GLOB '[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]')
);

CREATE TABLE IF NOT EXISTS movimentacao (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    usuario_id INTEGER NOT NULL REFERENCES usuario(id),
    ativo_id INTEGER NOT NULL REFERENCES ativo(id),
    data TEXT NOT NULL,
    tipo TEXT NOT NULL CHECK (tipo IN ('COMPRA', 'VENDA')),
    quantidade_e2 INTEGER NOT NULL CHECK (quantidade_e2 > 0),
    valor_centavos INTEGER NOT NULL CHECK (valor_centavos >= 0),
    CHECK (data GLOB '[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]')
);

CREATE INDEX IF NOT EXISTS idx_lancamento_usuario_data ON lancamento (usuario_id, data);
CREATE INDEX IF NOT EXISTS idx_movimentacao_usuario_data ON movimentacao (usuario_id, data);
CREATE INDEX IF NOT EXISTS idx_movimentacao_ativo_data ON movimentacao (ativo_id, data);
CREATE INDEX IF NOT EXISTS idx_valor_mercado_ativo_data ON valor_mercado (ativo_id, data);
CREATE UNIQUE INDEX IF NOT EXISTS uq_valor_mercado_ativo_data ON valor_mercado (ativo_id, data);

-- Migration do nível 2 (D-B2): remove as âncoras de preço que o nível 1 gravava em 0001-01-01.
-- Uma âncora sobrevivente tornaria "sempre existe preço elegível" e mascararia silenciosamente a
-- regra de ausência de preço (D-B3). É idempotente e pode rodar a cada inicialização.
DELETE FROM valor_mercado WHERE data = '0001-01-01';

-- Proprietário fixo dos níveis 1 e 2 (D-A9). O nível 3 acrescenta usuario0..usuario9 e root.
INSERT OR IGNORE INTO usuario (login, administrador) VALUES ('usuario0', 0);
