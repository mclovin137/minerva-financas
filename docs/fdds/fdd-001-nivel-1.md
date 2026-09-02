### FDD: APIs financeiras do nível 1

Versão: 1.2  
Data: 2026-09-02  
Responsável: Severino  
Revisor: Yoda  
**Revisão arquitetural: Yoda — 2026-09-02 — status: aprovado.**

---

### 1. Contexto, origem e limites

Este FDD detalha FR-001 a FR-004 do [PRD-001](../prds/prd-001-financas-pessoais.md), dentro do [HLD-001](../hlds/hld-001-arquitetura.md). O contrato abaixo é fechado antes da implementação; `data` é aceita e persistida no nível 1, e sua semântica temporal completa é ampliada pelo FDD-002. A posição síncrona já usa o caminho comum entre os três níveis. **Este FDD foi revisado e aprovado por Yoda em 2026-09-02; T-001 está liberada para implementação.**

Inclui conta, CRUD de ativos, lançamentos, movimentações e posição síncrona. Exclui Basic multiusuário, regras de emissão/vencimento e posição assíncrona, que entram cumulativamente no nível 3.

As decisões arquiteturais fechadas na revisão estão na seção 6 e são **normativas**: a implementação não precisa inventar nada além delas. O que permanece `❓ LACUNA` está explicitamente marcado e não pode ser preenchido por suposição.

### 2. Contrato público N1

Todos os requests e responses têm `Content-Type: application/json`. `data` usa `YYYY-MM-DD`. No N1 `data` é opcional e, quando ausente, assume a data corrente no fuso oficial da aplicação (`America/Sao_Paulo`, decisão D-A8); a partir do N2 ela é obrigatória nos POSTs. Os POSTs canônicos não têm corpo de resposta.

| Operação | Método e caminho | Payload | Resposta de sucesso |
|---|---|---|---|
| Crédito | `POST /contacorrente/credito` | `{"valor":12.42,"descricao":"...","data":"2020-02-28"}` | `201 Created`, sem corpo |
| Débito | `POST /contacorrente/debito` | mesmo formato do crédito | `201 Created`, sem corpo |
| Saldo | `GET /contacorrente/saldo?data=2020-02-28` | nenhum | `200 OK`, `{"saldo":1234.56}` |
| Listar ativos | `GET /ativos` | nenhum | `200 OK`, lista de ativos |
| Criar ativo | `POST /ativos` | `{"ativo":"ATIVO1","nome":"Ação 1","tipo":"RV","precoMercado":105.53}` | `201 Created`, sem corpo |
| Consultar ativo | `GET /ativos/{ativo}` | nenhum | `200 OK`, objeto do ativo |
| Alterar ativo | `PUT /ativos/{ativo}` | mesmo payload da criação | `200 OK`, objeto atualizado |
| Remover ativo | `DELETE /ativos/{ativo}` | nenhum | `204 No Content` |
| Comprar | `POST /movimentacao/compra` | `{"ativo":"ATIVO1","data":"2020-02-28","quantidade":2.5,"valor":105.53}` | `201 Created`, sem corpo |
| Vender | `POST /movimentacao/venda` | mesmo formato da compra | `201 Created`, sem corpo |
| Lançamentos | `GET /contacorrente/lancamentos?dataInicio=2020-02-01&dataFim=2020-02-28` | nenhum; ambos os filtros obrigatórios | `200 OK`, lista |
| Movimentações | `GET /movimentacao?dataInicio=2020-02-01&dataFim=2020-02-28` | nenhum; ambos os filtros obrigatórios | `200 OK`, lista |
| Posição | `GET /posicao?data=2020-02-28` | nenhum | `200 OK`, lista de posições |

Para ativos, `tipo` aceita somente `RV`, `RF` ou `FUNDO`; `precoMercado` aceita até oito casas. Valores monetários aceitam duas casas e quantidades duas casas. O `precoMercado` do N1 é a projeção do preço vigente do ativo, não uma coluna do ativo (decisão D-A1); preço por data entra no N2.

#### 2.1 Representações de resposta (fechadas nesta revisão)

Nenhuma lista tem paginação no N1; o recorte é o filtro obrigatório de data. Campos ausentes não são omitidos: quando indefinidos, vêm como `null`.

**Ativo** (`GET /ativos`, item de `GET /ativos`, `GET /ativos/{ativo}`, corpo de `PUT`)
```json
{"ativo":"ATIVO1","nome":"Ação 1","tipo":"RV","precoMercado":105.53}
```

**Lançamento** (item de `GET /contacorrente/lancamentos`)
```json
{"id":1,"data":"2020-02-28","valor":12.42,"descricao":"Crédito inicial"}
```
`valor` é positivo em crédito e negativo em débito; a lista vem ordenada por `data` e, no empate, por `id`, ambos crescentes.

**Movimentação** (item de `GET /movimentacao`)
```json
{"id":1,"ativo":"ATIVO1","data":"2020-02-28","tipo":"COMPRA","quantidade":2.50,"valor":105.53}
```
`tipo` assume `COMPRA` ou `VENDA`; ordenação idêntica à de lançamentos.

**Posição** (item de `GET /posicao?data=...`)
```json
{"ativo":"ATIVO1","nome":"Ação 1","tipo":"RV","quantidade":2.50,
 "precoMercado":105.53,"valorMercadoTotal":263.82,"precoMedio":100.00000000,
 "rendimento":1.05530000,"lucro":-250.00}
```
A lista contém **somente ativos com ao menos uma movimentação do proprietário até a data consultada**, ordenados por `ativo` crescente (decisão D-A4). `lucro` pode ser negativo; a invariante de não negatividade vale para saldo e quantidade, nunca para lucro.

### 3. Status HTTP, contrato de erro e justificativa

| Status | Quando | Justificativa |
|---|---|---|
| 200 | GET e PUT válidos | A leitura/alteração foi concluída e há representação quando aplicável. |
| 201 | POST que cria lançamento, movimento ou ativo | Um recurso/fato novo foi persistido; o corpo é omitido por contrato. |
| 204 | DELETE válido | A remoção foi concluída sem representação para devolver. |
| 400 | JSON ausente/malformado, campo ausente, tipo, sinal ou escala inválidos; filtro ausente/invertido | A requisição não pode ser processada com segurança pelo cliente. |
| 404 | Ativo inexistente ou recurso consultado inexistente | O caminho é válido, mas o recurso não existe. |
| 409 | Saldo insuficiente, quantidade insuficiente, ativo com movimentações em remoção/alteração incompatível ou duplicidade | A requisição é válida, porém conflita com o estado financeiro persistido. |
| 500 | Falha inesperada de domínio não mapeada ou persistência não recuperável | Erro do servidor sem expor detalhes internos. |
| 503 | Indisponibilidade transitória do armazenamento (`SQLITE_BUSY` após `busy_timeout`) | A falha é do servidor e é retentável pelo cliente. |

**Contrato de erro, único nos três níveis** (decisão D-A6). Todo status de 4xx e 5xx responde `application/json` com:
```json
{"codigo":"SALDO_INSUFICIENTE","mensagem":"Saldo insuficiente na data informada.",
 "campos":[{"campo":"valor","mensagem":"..."}]}
```
`campos` é omitido quando o erro não é de campo. `mensagem` é texto em pt-BR destinado à interface e **nunca** contém SQL, stack trace, caminho de arquivo, credencial ou valor de outro usuário. Catálogo mínimo de `codigo`, extensível pelos FDDs seguintes sem renomear os existentes:

| `codigo` | Status | Significado |
|---|---|---|
| `REQUISICAO_INVALIDA` | 400 | JSON ausente ou malformado |
| `CAMPO_INVALIDO` | 400 | campo ausente, tipo, sinal ou escala inválidos |
| `FILTRO_INVALIDO` | 400 | filtro obrigatório ausente, não parseável ou intervalo invertido |
| `ATIVO_NAO_ENCONTRADO` | 404 | código de ativo inexistente |
| `RECURSO_NAO_ENCONTRADO` | 404 | demais recursos inexistentes |
| `ATIVO_DUPLICADO` | 409 | código de ativo já cadastrado |
| `SALDO_INSUFICIENTE` | 409 | o saldo ficaria negativo em alguma data afetada |
| `QUANTIDADE_INSUFICIENTE` | 409 | a quantidade ficaria negativa em alguma data afetada |
| `ATIVO_EM_USO` | 409 | remoção/alteração incompatível com movimentações existentes |
| `ERRO_INTERNO` | 500 | falha não mapeada |
| `ARMAZENAMENTO_INDISPONIVEL` | 503 | contenção de escrita não resolvida dentro do `busy_timeout` |

Não há autenticação no N1. A tabela de `401`/`403` e Basic do N3 é aditiva e está no [FDD-003](fdd-003-nivel-3-e-opcoes.md).

### 4. Regras de cálculo, escalas e atomicidade

- Compra debita a conta e venda credita a conta. O lançamento espelho é **uma linha real em `lancamento`**, criada na mesma transação do movimento, e aparece em `GET /contacorrente/lancamentos` (decisão D-A5). Descrição canônica: `Compra ATIVO1` e `Venda ATIVO1`, com o código do ativo tal como cadastrado.
- Quantidade total = `Σ compras − Σ vendas`; valor de mercado total = quantidade total × preço de mercado vigente na data consultada.
- **Preço médio das compras = (Σ valor de cada compra) ÷ (Σ quantidade de cada compra)**: média ponderada pela quantidade, nunca média aritmética dos preços. Somam-se valores e quantidades na escala intermediária exata; o **floor é aplicado somente ao quociente da divisão**, ao reduzi-lo à escala do resultado.
- `rendimento = preco_mercado ÷ preco_medio`, calculado sobre o **`precoMedio` já reduzido à sua escala**, para que a razão exibida seja reproduzível a partir dos campos exibidos. `rendimento` é razão, não percentual.
- `lucro = Σ vendas − Σ compras`, em centavos exatos, sem divisão e sem floor.
- **Escalas de resultado** (decisão D-A2): `saldo`, `valor`, `valorMercadoTotal` e `lucro` são monetários, escala 2, floor. `precoMercado` e `precoMedio` são preços unitários, escala 8, floor. `quantidade` tem escala 2, exata. `rendimento` tem escala 8, floor.
- Dinheiro é calculado em centavos inteiros; quantidade e preço preservam suas escalas até o ponto de conversão. Nenhuma operação confirma saldo ou quantidade negativa **em nenhuma data**, inclusive quando os fatos são inseridos fora de ordem (decisão D-A3).

### 5. Casos, evidências e pronto

Os casos TC-001 a TC-032 da [matriz de testes](../tasks/matriz-de-testes.md) cobrem cada operação, sucesso, borda, validação, erro, estado inválido e concorrência aplicável. Cada caso tem fixture exclusiva, limpeza própria, execução repetível em qualquer ordem e request/response publicado pelo pipeline `testes-integracao` em `artifacts/testes/<commit>/<tc-id>/`, com referência no PR. TC-032 é satisfeito pela decisão D-A4.

### 6. Decisões arquiteturais fechadas na revisão (normativas)

#### D-A1 — Evolução do `precoMercado` entre N1 e N2
O ativo **nunca** tem coluna de preço. O preço de mercado vive sempre em `valor_mercado (ativo_id, data, preco)`, como já está em `backend/src/main/resources/schema.sql`. No N1, o `precoMercado` recebido em `POST`/`PUT /ativos` é persistido como o preço do ativo na **data-âncora `0001-01-01`**, por upsert sobre o índice único `uq_valor_mercado_ativo_data`; a leitura do ativo e a posição usam a mesma seleção do N2 — *o preço mais recente com data menor ou igual à data de referência*. No N1 existe exatamente uma linha, a âncora, então a seleção devolve o preço informado.

Consequência: a consulta de posição do N1 é literalmente a do N2 e o T-002 não a reescreve. No T-002 o campo sai do contrato de `/ativos` e a escrita da âncora deixa de existir; as âncoras remanescentes são removidas pela migration, para não mascararem a regra de ausência de preço elegível (ver FDD-002, D-B2). Alternativa descartada: coluna `preco_mercado` no ativo, dropada no T-002 — obrigaria dois caminhos de consulta e uma migration destrutiva de coluna.

#### D-A2 — Representação exata na persistência (conformidade com a ADR-001)
A [ADR-001](../adrs/adr-001-stack-da-aplicacao.md) proíbe `double` para dinheiro, preço e quantidade. A afinidade `NUMERIC` do SQLite armazena `105.53` como `REAL`, o que **viola essa decisão** e acumula erro ao somar 200.000 quantidades. O T-001 corrige o schema, sem ADR nova, porque isto é conformidade com decisão já registrada e não decisão nova:

- `valor_mercado.preco_mercado` → `preco_mercado_e8 INTEGER NOT NULL CHECK (preco_mercado_e8 >= 0)`, em unidades de 10⁻⁸.
- `movimentacao.quantidade` → `quantidade_e2 INTEGER NOT NULL CHECK (quantidade_e2 > 0)`, em unidades de 10⁻².
- `lancamento.valor_centavos` e `movimentacao.valor_centavos` permanecem `INTEGER` de centavos.

Na fronteira JSON, números são desserializados como `BigDecimal` (`DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS` habilitado) e **nunca** como `double` ou `float`; a serialização de saída emite a escala fixa da tabela de escalas do item 4.

#### D-A3 — Não negatividade é temporal desde o N1
A validação usa a mesma janela do N2 (`SUM(...) OVER (ORDER BY data, id)` e mínimo sobre as datas maiores ou iguais à do fato) já no T-001. Um caminho único vale para os três níveis, e o N1 é o caso particular em que todos os fatos compartilham a mesma data. Custo aceito e registrado: uma sequência fora de ordem que um N1 cego a datas aceitaria passa a ser rejeitada com 409 — o que é o comportamento exigido no N2 e no N3, que são a entrega final.

#### D-A4 — Ativo sem compras não existe em histórico válido
Venda exige quantidade disponível, e quantidade disponível só nasce de compra; logo, em qualquer histórico aceito, um ativo com movimentação tem `Σ quantidade de compras > 0` e a divisão do preço médio está sempre definida. A posição lista **apenas ativos movimentados pelo proprietário até a data**, o que fecha o `❓ LACUNA` de exibição do PRD FR-004 sem inventar regra: o caso simplesmente não é alcançável. Defesa em profundidade: se `Σ quantidade de compras` for zero, `precoMedio` e `rendimento` vêm `null` e nenhum erro é lançado.

#### D-A5 — Não existe tabela de conta com saldo materializado
A conta corrente é a **projeção** dos lançamentos do proprietário: `saldo(data) = Σ valor_centavos com data <= data`. Nenhum saldo é armazenado. Saldo materializado seria uma segunda fonte de verdade, divergiria sob concorrência e contradiria o HLD-001 (*fatos persistidos no SQLite são a fonte de verdade*). O `Conta` do modelo do HLD-001 é agregado de domínio, não tabela.

#### D-A6 — Contrato de erro único
Definido no item 3 e válido nos três níveis. Existe porque o PRD exige "erro JSON na API" sem fixar forma; sem esta decisão cada endpoint inventaria a sua.

#### D-A7 — Transação, isolamento e concorrência no SQLite
Toda unidade que lê para validar e depois escreve (débito, compra, venda, alteração de ativo) abre a transação em modo **`BEGIN IMMEDIATE`**, configurado no `DataSource` (`SQLiteConfig.setTransactionMode(IMMEDIATE)`), nunca em modo `DEFERRED`. Em `DEFERRED` a promoção de leitura para escrita no meio da transação produz `SQLITE_BUSY` não retentável e abre janela entre validar e gravar. Complementos obrigatórios: `PRAGMA journal_mode = WAL`, `PRAGMA busy_timeout = 5000` (já em `application.yml`) e `PRAGMA synchronous = NORMAL`, cujo custo aceito é a possibilidade de perder transações do último checkpoint em queda de energia — aceitável para banco embarcado de desafio, e registrado aqui em vez de ficar implícito.

**Escopo cortado:** nenhum lock de aplicação (semáforo ou `ReentrantLock`) para serializar escritores. `BEGIN IMMEDIATE` mais `busy_timeout` já serializam, e um lock em memória não protegeria um segundo processo — seria falsa garantia com custo de manutenção.

#### D-A8 — Fuso, data padrão e obrigatoriedade
Fuso oficial da aplicação: `America/Sao_Paulo`. No N1, `data` ausente assume a data corrente nesse fuso, em POST e em consulta. A partir do N2, `data` é **obrigatória** nos POSTs e nas consultas de saldo e posição; ausência responde 400 `CAMPO_INVALIDO` ou `FILTRO_INVALIDO`. Essa evolução é incompatível por construção e está registrada no FDD-002, item 8.

#### D-A9 — Proprietário dos dados antes da opção A
`lancamento.usuario_id` e `movimentacao.usuario_id` são `NOT NULL`. Enquanto não há Basic, N1 e N2 resolvem o proprietário para o usuário fixo **`usuario0`**, semeado na inicialização, e o N3 apenas troca a resolução do principal por HTTP Basic — sem migration e sem coluna anulável. Custo aceito: dados criados no N1/N2 pertencem a `usuario0` quando a opção A entra.

#### D-A10 — Ambiente dos testes de integração
O perfil `test` de `application.yml` usa `mode=memory&cache=shared`, onde o WAL **não** se aplica; testes de concorrência nesse perfil exercitariam um motor diferente do de produção e produziriam falso verde nos TC-008, TC-016, TC-024, TC-063 e TC-072. O T-001 troca o perfil de teste por **arquivo temporário exclusivo por execução** (`jdbc:sqlite:${java.io.tmpdir}/minerva-test-<uuid>.db`), com os mesmos PRAGMAs de produção, apagado ao final. Isso preserva a fixture isolada e a reexecução em qualquer ordem exigidas pelo PRD.

### 7. Fronteiras de pacote e camada — **substituídas pela ADR-003**

> **Esta seção não é mais normativa.** A organização do backend passou a ser decidida pela
> [ADR-003](../adrs/adr-003-estrutura-do-backend.md): pasta por entidade com subpasta por função
> (`<entidade>/{rest,actor,service,builder,dao,helper,dto,dominio}`), fluxo
> REST → Actor → { Helper, Service, Builder, DAO }, interfaces com prefixo `I` e fábrica para
> escolher o actor de operações irmãs. O detalhamento prescritivo está na seção A do
> [playbook de backend](../playbooks/playbook-backend.md).

O pacote base continua sendo **`br.com.minerva.financas`**. O que esta seção fixava e **permanece
válido**, agora sob a ADR-003:

1. O domínio não importa `org.springframework`, `jakarta`, `java.sql`, `javax.sql` nem serialização.
2. O REST não conhece o DAO; conversa com o caso de uso pelo actor.
3. Nenhum DTO atravessa para o domínio, e nenhuma entidade de domínio é serializada como resposta.

O gate do pipeline `review` foi reescrito para verificar essas três direções na estrutura por
entidade. A tabela de pacotes hexagonais que ocupava esta seção — `dominio.comum`, `aplicacao.porta`,
`infra.persistencia` e afins — **não vale mais**; consultá-la como contexto vigente é erro.

### Histórico

- 2026-09-02: contrato N1 fechado, fórmulas D1 e rastreabilidade TC adicionadas após auditoria.
- 2026-09-02: item 7 substituído pela ADR-003, por decisão do usuário; contrato público e regras de cálculo inalterados.
- 2026-09-02: revisão arquitetural de Yoda — **aprovado**. Fechadas as decisões D-A1 a D-A10 e as fronteiras de pacote; adicionados as representações de resposta, o contrato de erro e as escalas de resultado. Achados corrigidos: contradição do `precoMercado` entre N1 e N2; ausência de schema de resposta em cinco leituras; ausência de contrato de erro; `NUMERIC` de preço e quantidade violando a ADR-001; falta de descrição do lançamento espelho; ambiguidade do proprietário antes da opção A; escala do preço médio e do rendimento; ausência de modo de transação para o par ler-validar-escrever; perfil de teste em memória incapaz de exercitar WAL; ambiguidade entre as árvores de pacote `br.com.maps` e `br.com.minerva`.
