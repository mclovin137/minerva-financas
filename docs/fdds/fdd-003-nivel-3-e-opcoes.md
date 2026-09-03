### FDD: Nível 3, contrato comum e opções cumulativas

Versão: 1.2  
Data: 2026-09-02  
Responsável: Severino  
Revisor: Yoda  
**Revisão arquitetural: Yoda — 2026-09-02 — status: aprovado, com um `❓ LACUNA` não bloqueante registrado em 8.**

---

### 1. Escopo e decisão

Este FDD detalha FR-008, FR-009 e FR-010 do [PRD-001](../prds/prd-001-financas-pessoais.md), dentro do [HLD-001](../hlds/hld-001-arquitetura.md). **As opções A e B são escopo cumulativo e obrigatório**: A (Basic, autorização e isolamento) e B (posição assíncrona, escala e paralelismo) devem ser implementadas e testadas. Não existe decisão final entre opções. Deploy cloud está fora; Dockerfile e docker-compose permanecem incluídos. O contrato de erro, as escalas, o modo de transação e as fronteiras de pacote estão fechados no [FDD-001](fdd-001-nivel-1.md), itens 3, 4, 6 e 7. **Este FDD foi revisado e aprovado por Yoda em 2026-09-02; T-003 está liberada para implementação, exceto o valor exato do seed (item 8).**

### 2. Contratos canônicos comuns aos três níveis

| Método e caminho | Payload | Sucesso |
|---|---|---|
| `POST /contacorrente/credito` | `{"valor":12.42,"descricao":"...","data":"2020-02-28"}` | `201`, sem corpo |
| `POST /contacorrente/debito` | idem | `201`, sem corpo |
| `POST /movimentacao/compra` | `{"ativo":"ATIVO1","data":"2020-02-28","quantidade":2.5,"valor":105.53}` | `201`, sem corpo |
| `POST /movimentacao/venda` | idem | `201`, sem corpo |
| `GET /contacorrente/saldo?data=2020-02-28` | nenhum | `200`, `{"saldo":1234.56}` |

O campo `data` é aceito e persistido no N1 e obrigatório a partir do N2 (FDD-001, D-A8); essas cinco rotas e seus payloads **não mudam em nenhum nível**. CRUD de ativos, preço por data, consultas com período e posição síncrona estão fechados no [FDD-001](fdd-001-nivel-1.md) e [FDD-002](fdd-002-nivel-2-datas.md). A posição síncrona é `GET /posicao?data=2020-02-28`.

### 3. Contrato da opção A

HTTP Basic é exigido em toda requisição. Pré-cadastrar `usuario0` a `usuario9`, senhas `senha0` a `senha9`, e `root`/`spiderman`. Ativos e preços são compartilhados; somente root administra-os. Usuários comuns acessam apenas seus próprios lançamentos, movimentos e posições; root não transaciona nem consulta dados confidenciais.

#### 3.1 Matriz de capacidade por rota (decisão D-C1)

| Rota | `usuarioN` | `root` |
|---|---|---|
| `POST /contacorrente/credito`, `/debito` | permitido, sobre os próprios dados | `403 CAPACIDADE_NEGADA` |
| `POST /movimentacao/compra`, `/venda` | permitido, sobre os próprios dados | `403 CAPACIDADE_NEGADA` |
| `GET /contacorrente/saldo`, `/lancamentos` | somente os próprios | `403 CAPACIDADE_NEGADA` |
| `GET /movimentacao`, `GET /posicao`, `GET /posicao/{id}` | somente os próprios | `403 CAPACIDADE_NEGADA` |
| `GET /ativos`, `GET /ativos/{ativo}` | permitido (dado compartilhado) | permitido |
| `POST`, `PUT`, `DELETE /ativos` | `403 CAPACIDADE_NEGADA` | permitido |
| `PUT`, `DELETE /ativos/{ativo}/precos/{data}` | `403 CAPACIDADE_NEGADA` | permitido |

Basic ausente, malformado ou com credencial inválida responde `401 NAO_AUTENTICADO` com `WWW-Authenticate: Basic realm="minerva"`, **antes** de qualquer verificação de existência de recurso — para não transformar o 404 em oráculo de enumeração. Códigos acrescidos ao catálogo do FDD-001: `NAO_AUTENTICADO` (401) e `CAPACIDADE_NEGADA` (403).

#### 3.2 Credenciais e proprietário (decisão D-C2)

As senhas do seed são armazenadas como **hash BCrypt** em `usuario.senha_hash`, geradas na inicialização; senha em claro não é persistida nem registrada em log, e o cabeçalho `Authorization` nunca aparece bruto em log ou métrica. `usuario.administrador = 1` apenas para `root`. A resolução do proprietário troca o usuário fixo `usuario0` do N1/N2 (FDD-001, D-A9) pelo principal autenticado; nenhuma migration é necessária, e os dados criados antes da opção A permanecem sob `usuario0` — custo aceito e registrado.

O filtro de proprietário é aplicado **no caso de uso e no predicado SQL**, nunca só na serialização. Um `usuario_id` ausente no `WHERE` é o modo de falha mais provável desta opção, e é o que TC-070 e TC-072 devem provar.

### 4. Contrato da opção B

`GET /posicao?data=2020-02-28` retorna `202` e `{"id":42}`. `GET /posicao/42` retorna `425` enquanto pendente, `200` com a posição ao concluir e `404` se inexistente, já descartado ou pertencente a outro usuário. O resultado é descartado após a entrega. A execução processa **200.000 movimentações**, mede heap com `Runtime` imediatamente antes/depois (variação abaixo de **64 MB**) e registra paralelismo mínimo de **2 threads observável**. Reproduzir por `./mvnw -pl backend -Dtest=PosicaoDesempenhoIT test` ou `mvn -pl backend -Dtest=PosicaoDesempenhoIT test`.

#### 4.1 Ciclo de vida da execução (decisão D-C3)

- **Isolamento:** a execução pertence ao usuário que a criou. Consulta por outro usuário responde `404 RECURSO_NAO_ENCONTRADO`, não `403` — o 403 confirmaria a existência do id alheio.
- **Descarte:** ocorre na **primeira entrega bem-sucedida**, por remoção atômica do registro em memória (compare-and-remove); consultas seguintes respondem 404. Duas consultas simultâneas do mesmo id: exatamente uma recebe 200, a outra 404.
- **Expiração:** execução criada e nunca consultada expira em **10 minutos**, por varredura periódica. Sem isso, "memória constante" valeria para uma execução e não para o processo.
- **Backpressure:** no máximo **4 execuções pendentes por usuário**; a quinta responde `429 EXCESSO_DE_EXECUCOES`, acrescido ao catálogo do FDD-001. O HLD-001 exige limite de execuções; este é o limite.
- **Falha:** execução que falha registra estado `FALHA` e responde `500 ERRO_INTERNO` na consulta, nunca `425` — mascarar falha como progresso é falso verde observável pelo cliente (TC-077).

#### 4.2 Como a memória constante é obtida (decisão D-C4, normativa)

1. A leitura usa `JdbcTemplate` com `RowCallbackHandler` ou `queryForStream`, com `setFetchSize` definido e `try-with-resources`. É **proibido** materializar `List<Movimentacao>`, `collect(toList())` sobre o stream de movimentações ou qualquer coleção proporcional a 200.000 linhas.
2. O estado acumulado é o agregado **por ativo** — quantidade, soma de valor de compra, soma de quantidade de compra, soma de venda —, todos `long`. Com 128 ativos, o estado é da ordem de kilobytes e não cresce com o número de movimentações.
3. O resultado guardado até a entrega é a lista de posições (≤ número de ativos), não as movimentações.
4. A resposta 200 serializa a lista de posições já agregada; nenhuma etapa reconstrói a base de fatos.

#### 4.3 Paralelismo (decisão D-C5)

O trabalho é particionado **por `ativo_id`** (`ativo_id % N`), porque a agregação é independente por ativo e a combinação final é uma concatenação, sem merge de estado compartilhado. `N` é fixo, mínimo 2, limitado por `min(4, disponíveis)`. O executor é um `ExecutorService` **dedicado**, com threads nomeadas `posicao-worker-<i>`, nunca o pool do container HTTP: a nomeação é o que torna o paralelismo observável para TC-080, e o pool dedicado é o que impede a opção B de consumir os threads que atendem as demais rotas.

Cada partição usa a própria conexão. Isso é seguro porque o WAL permite leitores concorrentes; a limitação de escritor único do SQLite não se aplica a uma consulta.

#### 4.4 Veredito de viabilidade das metas D3

**Viáveis com a stack da ADR-001**, sob as condições acima. Fundamento: o driver `sqlite-jdbc` avança o cursor sob demanda, então 200.000 linhas nunca coexistem em heap; o estado de agregação é O(número de ativos); o objeto de resposta é O(número de ativos). A variação de heap medida por `Runtime` passa a ser dominada por buffers do driver, pelo `ExecutorService` e pelo ruído de GC, muito abaixo de 64 MB. Os dois modos de falha realistas, e o que os previne, são: materializar a lista de movimentações (proibido em D-C4.1) e usar JPA, que a ADR-001 já descartou por este motivo. O risco remanescente é de **medição**, não de arquitetura: `Runtime` mede heap usado, não alocado, e o resultado oscila com o GC — a fixture do TC-079 deve estabilizar o heap antes da primeira leitura e usar a mesma execução para as duas medições.

### 5. Status HTTP comum

`200` leitura/alteração concluída; `201` criação de fato sem corpo; `202` aceitação da execução assíncrona; `204` remoção sem representação; `400` JSON, campo, data, escala ou filtro inválido; `401` Basic ausente/inválido; `403` capacidade incompatível; `404` recurso/execução inexistente ou de outro proprietário; `409` conflito de saldo, quantidade ou estado; `425` execução ainda em processamento; `429` excesso de execuções pendentes; `500` falha inesperada; `503` indisponibilidade transitória do armazenamento. Todo corpo de erro segue o contrato único do FDD-001, item 3.

### 6. Observabilidade, evidência e pronto

Registrar status, latência, correlation id, execution id, heap e threads sem senha, sem `Authorization` bruto e sem payload financeiro completo. TC-057 a TC-080 da [matriz](../tasks/matriz-de-testes.md) cobrem contrato, A, B, concorrência, sucesso, bordas, validação, erros e estados inválidos. A evidência é repetível em qualquer ordem, com fixture/limpeza próprias, request/response e relatório no pipeline `testes-integracao`, em `artifacts/testes/<commit>/<tc-id>/`, referenciado no PR.

### 7. Container e execução (decisão D-C6)

O Dockerfile publica o jar do backend e declara `data/` como volume, porque o SQLite de `application.yml` grava em `data/minerva-financas.db`; sem volume, o arquivo morre com o contêiner e o schema é recriado a cada subida — o que passaria despercebido até um teste de persistência falhar. O `docker-compose` sobe um único serviço, sem banco externo, coerente com a execução standalone da regra de ferro 6.

### 8. Lacuna registrada

- `❓ LACUNA` — **valores do seed `ATIVO0`–`ATIVO127`**: nome, tipo, `dataEmissao`, `dataVencimento` e preço inicial por ativo não constam do PRD-001, do HLD-001 nem desta cadeia documental. Como o N2 remove o preço direto do ativo e exige a janela de negociação, o seed precisa criar também a linha de preço por data, e esses valores são **dados do enunciado, não decisão arquitetural** — inventá-los produziria um seed que a avaliação do desafio pode contradizer. **Dono: usuário.** Bloqueia apenas o TC-064; a fixture determinística de 200.000 movimentações da opção B pode ser gerada pelo próprio teste e não depende desta lacuna.

### Histórico

- 2026-09-02: A e B tornadas cumulativas, contrato assíncrono e metas D3 fixados após auditoria.
- 2026-09-02: revisão arquitetural de Yoda — **aprovado**. Fechadas as decisões D-C1 a D-C6. Achados corrigidos: matriz de capacidade por rota ausente; armazenamento das senhas do seed indefinido; ausência de regra de isolamento da execução assíncrona; descarte, expiração, backpressure e distinção falha/425 indefinidos; ausência de instrução normativa contra materializar as 200.000 movimentações; critério de partição e nomeação de threads ausente para TC-080; `429` fora da tabela de status; volume do SQLite no contêiner. Registrado o `❓ LACUNA` dos valores de seed, que volta ao usuário.
