PRAGMA journal_mode = WAL;

CREATE TABLE IF NOT EXISTS usuario (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    login TEXT NOT NULL UNIQUE,
    senha_hash TEXT,
    administrador INTEGER NOT NULL DEFAULT 0 CHECK (administrador IN (0, 1))
);

CREATE TABLE IF NOT EXISTS ativo (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    codigo TEXT NOT NULL UNIQUE,
    nome TEXT NOT NULL,
    tipo TEXT NOT NULL CHECK (tipo IN ('RV', 'RF', 'FUNDO')),
    data_emissao TEXT,
    data_vencimento TEXT,
    CHECK (data_emissao IS NULL OR data_vencimento IS NULL OR data_emissao < data_vencimento)
);

CREATE TABLE IF NOT EXISTS valor_mercado (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    ativo_id INTEGER NOT NULL REFERENCES ativo(id) ON DELETE CASCADE,
    data TEXT NOT NULL,
    preco_mercado NUMERIC NOT NULL,
    CHECK (data GLOB '[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]'),
    CHECK (preco_mercado >= 0)
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
    quantidade NUMERIC NOT NULL CHECK (quantidade > 0),
    valor_centavos INTEGER NOT NULL CHECK (valor_centavos >= 0),
    CHECK (data GLOB '[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]')
);

CREATE INDEX IF NOT EXISTS idx_lancamento_usuario_data ON lancamento (usuario_id, data);
CREATE INDEX IF NOT EXISTS idx_movimentacao_usuario_data ON movimentacao (usuario_id, data);
CREATE INDEX IF NOT EXISTS idx_movimentacao_ativo_data ON movimentacao (ativo_id, data);
CREATE INDEX IF NOT EXISTS idx_valor_mercado_ativo_data ON valor_mercado (ativo_id, data);
CREATE UNIQUE INDEX IF NOT EXISTS uq_valor_mercado_ativo_data ON valor_mercado (ativo_id, data);
