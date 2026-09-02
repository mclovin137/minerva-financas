# Playbook Database — problemas comuns de banco relacional e SQL

**Autor:** Cristóvão Augusto

> **Como usar**: referência de consulta seletiva do `backend-dev`. NÃO ler inteiro — o índice
> mapeia gatilho → seção. Cada seção responde: onde o problema costuma aparecer, **como
> identificar** (sintoma + ferramenta) e **como resolver** (com "qual opção quando").
>
> **Este é o playbook-base do template**, escrito com PostgreSQL como referência (skill de apoio:
> `postgres-patterns`; a maior parte vale para qualquer relacional). Na descoberta, contextualize
> os exemplos ao domínio e à stack de acesso a dados escolhida (ORM/driver/gerador de queries).

| # | Problema | Consultar quando… |
|---|----------|-------------------|
| 1 | [N+1 queries](#1-n1-queries) | loop chamando query; listagem com relacionamento |
| 2 | [Falta de índice](#2-falta-de-índice--seq-scan) | query lenta, filtro/join/order sem índice |
| 3 | [Índice inútil ou em excesso](#3-índice-inútil-ou-em-excesso) | criar índice "por via das dúvidas" |
| 4 | [Deadlock](#4-deadlock) | transações travando entre si |
| 5 | [Contenção de lock / transação longa](#5-contenção-de-lock--transação-longa) | latência sob concorrência |
| 6 | [Pool esgotado / conexão vazada](#6-pool-esgotado--conexão-vazada) | timeout ao obter conexão; `idle in transaction` |
| 7 | [Race check-then-act](#7-race-check-then-act) | "verifica se existe, depois insere/atualiza" |
| 8 | [Isolamento e anomalias](#8-isolamento-e-anomalias) | lost update, leitura inconsistente |
| 9 | [Paginação com OFFSET](#9-paginação-com-offset) | listagens paginadas |
| 10 | [Armadilhas de NULL](#10-armadilhas-de-null) | NOT IN, UNIQUE, agregados, comparações |
| 11 | [Tipos errados](#11-tipos-errados) | dinheiro, datas/horas, IDs, texto livre |
| 12 | [Validação só na aplicação](#12-validação-só-na-aplicação) | invariante de negócio sem constraint |
| 13 | [Bloat e autovacuum](#13-bloat-e-autovacuum) | tabela com muito UPDATE/DELETE crescendo/lenta |
| 14 | [Diagnóstico de query lenta](#14-diagnóstico-de-query-lenta) | qualquer investigação de performance |
| 15 | [Timeouts](#15-timeouts) | query presa segurando recursos; lock infinito |
| 16 | [Busca de texto](#16-busca-de-texto) | busca por nome/título/descrição |
| 17 | [Disciplina da camada de dados](#17-disciplina-da-camada-de-dados) | NULL scan, transações, erros do driver, SQL dinâmico |
| 18 | [Checklist de revisão de query](#18-checklist-de-revisão-de-query) | toda query/migration nova em PR |

---

## 1. N+1 queries

**Como identificar**: query dentro de `for` sobre resultado de outra query (revisão pega de graça); logs mostrando a mesma query repetida com parâmetros diferentes em rajada; latência da rota crescendo linear com o tamanho da lista. ORMs com lazy loading escondem N+1 — ligue o log de SQL em dev.

**Como resolver**
1. **JOIN** trazendo tudo numa query — default para relação 1:1 ou 1:poucos.
2. **Duas queries + junção em memória** — `WHERE fk = ANY($1)` com os IDs coletados (o "eager loading" dos ORMs); melhor quando a relação multiplica linhas (1:N grande).
3. Nunca "resolver" com cache: N+1 cacheado continua N+1 no miss.

## 2. Falta de índice / seq scan

**Como identificar**: `EXPLAIN (ANALYZE, BUFFERS)` mostrando `Seq Scan` em tabela que cresce, ou `Rows Removed by Filter` alto; query lenta só quando a tabela enche (dev com 10 linhas nunca revela). Lembrete: Postgres **não** indexa FK automaticamente.

**Como resolver**
1. Índice b-tree na(s) coluna(s) do filtro/join — em índice composto, coluna de igualdade primeiro, range depois.
2. Índice parcial quando só um subconjunto é consultado: `CREATE INDEX ... WHERE status != 'arquivado'`.
3. Covering index (`INCLUDE`) só se o `EXPLAIN` mostrar Heap Fetches doendo.
4. Toda FK criada em migration ganha índice na mesma migration, salvo justificativa.

## 3. Índice inútil ou em excesso

**Como identificar**: `pg_stat_user_indexes.idx_scan = 0` após uso real; índice solitário em coluna de baixíssima cardinalidade (o planner raramente usa); redundância `(a)` + `(a, b)`.

**Como resolver**: cada índice custa escrita e espaço — remover os não usados (migration de `DROP INDEX`); preferir 1 composto certo a 2 simples; baixa cardinalidade só como parcial ou parte de composto.

## 4. Deadlock

**Como identificar**: erro `40P01 deadlock detected` (o log do servidor mostra o **par exato** de queries — ler, ele entrega); testes de concorrência (o QA deve ter cenário de operações simultâneas sobre os mesmos registros).

**Como resolver**
1. **Ordem determinística de travamento** — travar linhas sempre na mesma ordem (`ORDER BY id FOR UPDATE`). Elimina a classe do problema.
2. Transações curtas (§5) — menos tempo segurando lock, menos janela de ciclo.
3. Retry da transação inteira no 40P01 (seguro: uma delas foi revertida por completo), com limite de tentativas.
4. `NOWAIT`/`SKIP LOCKED` onde esperar não faz sentido ("recurso indisponível" na hora — playbook-backend §10).

## 5. Contenção de lock / transação longa

**Como identificar**: `pg_stat_activity` com `state = 'idle in transaction'` ou `wait_event_type = 'Lock'`; `pg_locks` com `granted = false`; p99 alto sob concorrência com CPU do banco baixa (todos esperando, ninguém trabalhando); migration enfileirada atrás de transação longa.

**Como resolver**
1. **Regra de ouro: nenhuma chamada externa (HTTP, fila, e-mail) dentro de transação aberta.** Padrão: transação 1 grava estado intermediário com expiração; fora de transação, chamada externa; transação 2 confirma. A "trava" que atravessa a chamada é **estado + expiração**, não lock do banco.
2. Transação = menor unidade de escrita consistente; ler antes, escrever rápido, commitar.
3. `lock_timeout` curto em migrations (§15) para não enfileirar produção atrás de um `ALTER`.

## 6. Pool esgotado / conexão vazada

**Como identificar**: erro `too many clients already` (servidor) ou timeout no acquire (app); `idle in transaction` acumulando em `pg_stat_activity` = transação vazada; contagem colada no máximo constante; métricas do pool (tamanho, tempo de acquire) na observabilidade (roles.md §6.8).

**Como resolver**
1. Vazamento: fechar cursors/rows sempre; transação sempre com rollback garantido logo após o begin (`defer`/`finally` — rollback pós-commit é no-op seguro).
2. Dimensionar: máximo do pool = limite do servidor − margem (admin/migrations). O pool é a **válvula de back pressure** (playbook-backend §7): esgotou → espera com timeout e falha rápido, não derruba o banco.
3. Mínimo baixo em ambiente que hiberna (playbook-backend §19).

## 7. Race check-then-act

**Como identificar**: qualquer par SELECT-depois-escreve onde a decisão depende do SELECT — dois requests passam no SELECT juntos e ambos escrevem. Pergunta de revisão: "o que acontece se dois requests executarem isso na mesma milissegundo?"

**Como resolver**
1. **Constraint UNIQUE + `ON CONFLICT`** — o banco é o árbitro: `INSERT ... ON CONFLICT DO NOTHING` e checar linhas afetadas (0 = perdeu a corrida). Default para dedup.
2. **`SELECT ... FOR UPDATE`** antes da decisão — serializa os concorrentes na linha; para fluxos que precisam ler estado + decidir + escrever.
3. **UPDATE condicional atômico** — `UPDATE ... SET status='x' WHERE id=$1 AND status='y'`, checar `RowsAffected`; ótimo quando a transição de estado é simples.
4. Nunca resolver com mutex na aplicação: não sobrevive a 2 instâncias nem a restart.

## 8. Isolamento e anomalias

**Como identificar**: fluxo read-modify-write sem `FOR UPDATE` em `READ COMMITTED` (default) = **lost update** possível; relatórios multi-query com totais que "não batem" (read skew); bugs irreproduzíveis que só aparecem sob carga.

**Como resolver**
1. Manter `READ COMMITTED` + **locking explícito** (§7.2) ou update atômico (§7.3) nas escritas críticas — barato e previsível; estratégia default.
2. **Lock otimista** (coluna `version`, `UPDATE ... WHERE version = $n`) — conflito raro + segurar lock atrapalha (edições de admin).
3. `REPEATABLE READ` para relatórios multi-query que precisam de snapshot consistente.
4. `SERIALIZABLE` — resolve tudo ao custo de retries obrigatórios (erro 40001); só se um fluxo provar que precisa.

## 9. Paginação com OFFSET

**Como identificar**: `LIMIT $1 OFFSET $2` em listagem que cresce. Sintomas: páginas altas cada vez mais lentas (OFFSET lê e descarta tudo antes); itens pulados/duplicados quando inserem linhas entre páginas.

**Como resolver**
1. **Keyset/cursor**: `WHERE (criado_em, id) < ($ts, $id) ORDER BY criado_em DESC, id DESC LIMIT $n` — estável e O(página). Default para tabela que cresce sem limite. Exige índice na chave do cursor e desempate por coluna única.
2. OFFSET é aceitável quando: tabela pequena e limitada, UI precisa de "pular para página N", ou uso interno raro. Não reescrever esses.

## 10. Armadilhas de NULL

**Casos clássicos**
- `WHERE coluna != 'x'` **exclui** linhas NULL silenciosamente.
- `NOT IN (subquery)` retorna **vazio** se a subquery contiver um NULL.
- `UNIQUE` permite múltiplos NULLs.
- `COUNT(coluna)` ignora NULL vs `COUNT(*)` — relatórios divergem.

**Como resolver**: preferir `NOT NULL` + default semântico quando existir; `NOT EXISTS` no lugar de `NOT IN` com subquery (imune a NULL e plano geralmente melhor); `IS DISTINCT FROM` quando NULL deve comparar como valor; Postgres 15+: `UNIQUE NULLS NOT DISTINCT`; na aplicação, coluna nullable → tipo nullable (§17) — nunca mapear NULL para zero-value mudo.

## 11. Tipos errados

| Dado | Errado | Certo |
|------|--------|-------|
| Dinheiro | `float`/`real` (arredondamento acumula), `money` | **menor unidade em `BIGINT`** (centavos) ou `NUMERIC`; moeda explícita se puder variar |
| Data/hora | `timestamp` sem timezone (hora ambígua) | **`timestamptz`**; app em UTC, converte na borda |
| ID público | sequencial exposto na URL (enumerável) | **UUID** para tudo que aparece em URL/link; sequencial pode ser PK interna |
| Texto | `varchar(255)` cargo-cult | `text` + `CHECK (char_length(...) <= n)` quando o limite é regra de negócio |
| Enum de status | string livre | `CHECK (status IN (...))` — mais fácil de evoluir que tipo ENUM |

Errar tipo é caro de consertar (migration + backfill + código) — acertar na primeira migration.

## 12. Validação só na aplicação

**Como identificar**: em revisão de migration, perguntar por cada invariante do domínio: "onde o **banco** garante isso?" — unicidade de recurso disputado, valores não negativos, FKs com `ON DELETE` **explícito e pensado** (`RESTRICT` por default — nunca cascade acidental).

**Como resolver**: constraint no banco é a última linha de defesa e a única à prova de concorrência (§7); validação na app é UX (erro bonito antes), não integridade. As duas coexistem — só a do banco é obrigatória.

## 13. Bloat e autovacuum

**Como identificar**: `pg_stat_user_tables` com `n_dead_tup` alto vs `n_live_tup`, `last_autovacuum` antigo; tabela cujo tamanho físico só cresce com contagem estável. Candidatas típicas: tabelas de alta rotatividade (outbox, filas, reservas com expiração).

**Como resolver**: confiar no autovacuum e **não desligar**; afinar por tabela quente (`autovacuum_vacuum_scale_factor` menor) só com evidência do §14; limpeza de tabelas transientes em lotes pequenos e frequentes (não `DELETE` gigante mensal), retenção curta; `VACUUM FULL` trava a tabela — último recurso.

## 14. Diagnóstico de query lenta

**Ferramentas, na ordem**
1. **`EXPLAIN (ANALYZE, BUFFERS)`** com parâmetros reais — ler de dentro para fora: `Seq Scan` em tabela grande (§2), estimativa vs real discrepante (estatísticas velhas → `ANALYZE`), `Sort`/`Hash` derramando para disco, Nested Loop com inner caro.
2. **`pg_stat_statements`** — habilitar desde o ambiente local; responde "quais queries mais custam no agregado" (média×frequência importa mais que a lenta ocasional).
3. **`log_min_duration_statement`** (ex.: 200ms em dev) — pega as lentas em uso real sem instrumentar nada.
4. Métricas/traces da aplicação por query (roles.md §6.8).

**Regra**: nenhuma otimização (índice, reescrita, cache) sem `EXPLAIN ANALYZE` antes/depois anexado ao PR — "achei que ficaria mais rápido" não é evidência.

## 15. Timeouts

**Camadas (todas)**
1. **Timeout de contexto/statement em toda query** do lado da aplicação — curto em rota síncrona (ex.: 5s), maior em job/consumer.
2. **`statement_timeout`** no nível da conexão/role como backstop do servidor.
3. **`lock_timeout` curto em migrations** (ex.: `5s`) + retry: o `ALTER` falha rápido em vez de enfileirar a produção inteira atrás dele.
4. `idle_in_transaction_session_timeout` — mata transação vazada (§6) antes de virar incidente.

## 16. Busca de texto

**Como identificar o problema**: `ILIKE '%termo%'` — wildcard à esquerda **não usa índice b-tree**; seq scan em toda busca.

**Como resolver**
1. Tabela pequena (dezenas/centenas de linhas): **seq scan está ok** — registrar a decisão e seguir (anti-overengineering).
2. Crescendo: extensão `pg_trgm` + índice GIN (`gin_trgm_ops`) — atende `ILIKE '%x%'` e busca com typo.
3. Full-text search (`tsvector`) — busca linguística real (múltiplas palavras, relevância); só com requisito concreto.
4. Motor de busca dedicado (Elasticsearch etc.) — só em escala/complexidade que o justifique.

## 17. Disciplina da camada de dados

Independente da stack de acesso (ORM, driver, gerador de queries), os erros recorrentes:

1. **Coluna nullable → tipo nullable na aplicação.** Scan de NULL em tipo não-nullable falha em runtime, não em compile — conferir no schema, não descobrir em produção.
2. **Transação com rollback garantido**: begin → rollback registrado imediatamente (`defer`/`finally`/context manager) → trabalho → commit. Esquecer vaza transação (§6). **A transação pertence ao caso de uso, não ao repositório** — repositórios recebem a transação/querier de fora para participar dela.
3. **"No rows" traduzido para erro de domínio** na camada de dados (`ErrNaoEncontrado`), comparado por tipo/sentinela — nunca por string.
4. **Violações de constraint mapeadas por código do erro** (23505 unique, 23503 FK, 40P01 deadlock §4, 40001 serialização §8) para erros de domínio — é assim que "o banco como árbitro" (§7.1) chega limpo ao handler.
5. **SQL dinâmico**: filtros opcionais via `($1 IS NULL OR coluna = $1)` ou queries separadas — **nunca** concatenar SQL com input (injection — playbook-security §1).
6. **Batching** para muitos INSERTs pequenos (1 round-trip), dentro de transação normal.

## 18. Checklist de revisão de query

Para toda query/migration nova no PR:

1. Tem índice para o filtro/join/order? (`EXPLAIN` se houver dúvida — §2, §14)
2. Roda em loop? (N+1 — §1)
3. Par check-then-act sem constraint/lock? (§7)
4. Transação: curta, sem chamada externa dentro, rollback garantido? (§5, §17.2)
5. Locks em ordem determinística? `NOWAIT`/`SKIP LOCKED` onde esperar não vale? (§4)
6. NULL: alguma comparação/`NOT IN`/UNIQUE afetada? (§10)
7. Tipos: dinheiro na menor unidade, `timestamptz`, ID público não-enumerável? (§11)
8. Invariante de negócio tem constraint correspondente no banco? (§12)
9. Paginação: keyset se a tabela cresce sem limite? (§9)
10. Timeout na chamada; `lock_timeout` se for migration com `ALTER`? (§15)
