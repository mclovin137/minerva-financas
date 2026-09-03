# Minerva Finanças

Aplicação de finanças pessoais do processo seletivo da MAPS, implementada até o **nível 3**, com as
opções **A (multiusuário)** e **B (consulta assíncrona de alto volume)** — as duas, não uma.

A aplicação ajuda a gerir ativos financeiros e a acompanhar patrimônio e saldo em conta corrente,
sempre em função de uma **data**: saldo, posição e preço de mercado são conceitos datados, e é isso
que organiza o desenho inteiro.

---

## Sumário

- [Como executar](#como-executar)
- [Como compilar e testar](#como-compilar-e-testar)
- [Stack e por que cada peça](#stack-e-por-que-cada-peça)
- [Arquitetura](#arquitetura)
- [Contrato da API](#contrato-da-api)
- [Códigos de status HTTP](#códigos-de-status-http)
- [Precisão numérica e arredondamento](#precisão-numérica-e-arredondamento)
- [Regras temporais](#regras-temporais)
- [Opção A — multiusuário e segurança](#opção-a--multiusuário-e-segurança)
- [Opção B — consulta assíncrona de alto volume](#opção-b--consulta-assíncrona-de-alto-volume)
- [Thread-safety e concorrência](#thread-safety-e-concorrência)
- [Desempenho: decisões e medições](#desempenho-decisões-e-medições)
- [Interface web](#interface-web)
- [Testes](#testes)
- [Deploy](#deploy)
- [Premissas assumidas](#premissas-assumidas)
- [O que não foi entregue](#o-que-não-foi-entregue)

---

## Como executar

**Standalone**, sem nenhum serviço externo — o banco é embarcado e nasce sozinho na primeira
execução:

```bash
./mvnw -f backend/pom.xml spring-boot:run
```

A aplicação sobe em `http://localhost:8080`, servindo **a API e a interface web no mesmo host e na
mesma porta**. O banco fica em `backend/data/minerva-financas.db`, criado sozinho na primeira
execução; apagar esse arquivo recria o ambiente do zero.

Para incluir a interface no jar, compile-a antes — o Maven a empacota automaticamente quando
`frontend/dist` existe, e ignora o diretório quando não existe:

```bash
npm --prefix frontend ci && npm --prefix frontend run build
./mvnw -f backend/pom.xml spring-boot:run
```

Um jar autocontido também serve:

```bash
./mvnw -f backend/pom.xml -DskipTests package
java -jar backend/target/minerva-financas-backend-0.0.1-SNAPSHOT.jar
```

**Em contêiner**:

```bash
docker compose up --build
```

Toda requisição exige HTTP Basic (opção A). Um exemplo de ponta a ponta:

```bash
curl -u usuario0:senha0 -X POST http://localhost:8080/contacorrente/credito \
  -H 'Content-Type: application/json' \
  -d '{"valor": 1000.00, "descricao": "aporte inicial", "data": "2020-02-28"}'

curl -u usuario0:senha0 -X POST http://localhost:8080/movimentacao/compra \
  -H 'Content-Type: application/json' \
  -d '{"ativo": "ATIVO1", "data": "2020-02-28", "quantidade": 2.5, "valor": 105.53}'

curl -u usuario0:senha0 'http://localhost:8080/contacorrente/saldo?data=2020-02-28'
# {"saldo":894.47}

curl -u usuario0:senha0 'http://localhost:8080/posicao?data=2020-02-28'
```

## Como compilar e testar

O build é **Maven**, com wrapper versionado — não é preciso ter Maven instalado.

| Objetivo | Comando |
|---|---|
| Compilar | `./mvnw -f backend/pom.xml compile` |
| Rodar toda a suíte | `./mvnw -f backend/pom.xml test` |
| Empacotar | `./mvnw -f backend/pom.xml package` |
| Executar | `./mvnw -f backend/pom.xml spring-boot:run` |
| Só o teste de desempenho | `./mvnw -f backend/pom.xml test -Dtest=PosicaoDesempenhoIT` |

Requisito: **JDK 25**. Nenhum banco, servidor de aplicação ou serviço externo precisa estar no ar.

## Stack e por que cada peça

| Peça | Versão | Por quê |
|---|---|---|
| Java | 25 | LTS mais recente; `record`, `sealed` e pattern matching em `switch` deixam o domínio menor e mais explícito |
| Spring Boot | 4.1.1 | Servidor embutido, injeção e mapeamento HTTP sem servidor de aplicação — requisito de execução standalone |
| Spring JDBC | via Boot | SQL explícito e leitura por cursor; ver abaixo por que não JPA |
| SQLite | 3.51 (driver `org.xerial`) | Banco embarcado, arquivo único, zero administração |
| `spring-security-crypto` | via Boot | Só o `BCryptPasswordEncoder`, sem o starter de segurança inteiro |
| React + TypeScript | 19 / 5.9 | Interface web; TypeScript em modo estrito, sem `any` no código da aplicação |
| Vite | 7 | Build e servidor de desenvolvimento com proxy para a API |

Tudo o mais é runtime padrão do Java ou já vinha no Spring Boot.

### Por que SQLite

O enunciado pede banco **embarcado** e execução standalone. SQLite é um arquivo: não há processo
para subir, porta para abrir nem usuário para criar, e o mesmo binário roda igual na máquina do
avaliador e no contêiner. Suporta transações ACID, índices e funções de janela — que é exatamente o
que a validação temporal precisa.

O custo é conhecido e está tratado: **um escritor por vez**. Ver
[thread-safety](#thread-safety-e-concorrência).

### Por que Spring JDBC e não JPA/Hibernate

A opção B exige memória constante ao processar centenas de milhares de movimentações. JPA mantém
cache de primeiro nível e materializa o grafo de entidades da unidade de trabalho: o uso de memória
tende a acompanhar o volume lido, que é justamente o que a opção B proíbe. Com JDBC, o `ResultSet` é
percorrido por cursor, cada linha é agregada e descartada, e nada proporcional ao volume é retido.

Em troca, o SQL é escrito à mão. Aceitável: as consultas aqui são poucas, temporais e específicas —
o tipo de consulta que a gente acabaria escrevendo à mão em JPQL de qualquer forma.

### Frameworks e design patterns usados

O enunciado pede justificativa para o que não é trivial:

- **DDD organizado por entidade (ADR-003).** Cada entidade concentra REST, Actor, Service, Builder,
  DAO, Helper, DTO e domínio conforme a necessidade. O domínio não conhece HTTP nem SQL; a direção
  dessa fronteira é verificada mecanicamente pelo pipeline de review.
- **Objetos de valor** (`Dinheiro`, `Quantidade`, `PrecoUnitario`). Cada um valida a própria escala na
  construção, então é impossível haver um valor monetário com três casas circulando pelo sistema.
- **Acumulador** (`CalculoPosicao.Acumulador`). Estado de agregação O(1) por ativo, que é o mecanismo
  concreto da memória constante da opção B.
- **`sealed interface` para o estado da execução assíncrona.** O `switch` sobre `EmAndamento`,
  `Concluida` e `Falha` é verificado pelo compilador: um estado novo quebra a compilação em vez de
  cair silenciosamente em um `else`.

Não há framework de mapeamento objeto-relacional, biblioteca de validação além do que vem no Boot,
nem abstração especulativa "para o caso de trocar de banco".

## Arquitetura

A ADR-003 define organização por entidade, com dependências seguindo o fluxo:

```
<entidade>/rest ──► actor ──► { helper, service, builder, dao }
                         └──► <entidade>/dominio
```

As entidades são `contacorrente`, `ativo`, `movimentacao`, `posicao` e `usuario`. `comum` contém
somente tipos genuinamente compartilhados. Cada entidade pode ter as subpastas `rest`, `actor`,
`service`, `builder`, `dao`, `helper`, `dto` e `dominio`, conforme a necessidade real.

REST recebe e devolve DTO; Actor orquestra o caso de uso; Service concentra a regra de negócio;
Builder monta domínio ou resposta; e DAO é o único componente que fala SQL. Interfaces têm prefixo
`I` (`IContaCorrenteDAO`/`ContaCorrenteDAO`) e nomes de classes, métodos, campos e parâmetros não
usam abreviações. Operações irmãs compartilham o endpoint e usam fábrica quando há mais de uma
implementação concreta (`LancamentoActorFactory`, `MovimentacaoActorFactory` e
`PosicaoActorFactory`). As rotas públicas permanecem as mesmas.

O job `fronteiras-de-arquitetura` de `.github/workflows/review.yml` verifica, nos pacotes por
entidade, que `*/dominio` não importa Spring, Jakarta, JDBC ou Jackson; que Actor/Service/Helper/Builder não
importam a infraestrutura global antiga ou HTTP; e REST não importa DAO, SQL ou infraestrutura. Ele também verifica o prefixo
`I` apenas em interfaces top-level nos diretórios da estrutura nova. Enquanto os pacotes globais
`api`, `aplicacao`, `dominio`, `infra` e `seguranca` não fazem parte da produção consolidada.

Não há ArchUnit: o gate é determinístico e não adiciona dependência ao backend.

## Contrato da API

Toda requisição e resposta é `application/json`. Datas são `YYYY-MM-DD`.

### Conta corrente

| Operação | Rota | Corpo | Sucesso |
|---|---|---|---|
| Crédito | `POST /contacorrente/credito` | `{"valor":12.42,"descricao":"...","data":"2020-02-28"}` | `201`, sem corpo |
| Débito | `POST /contacorrente/debito` | idem | `201`, sem corpo |
| Saldo | `GET /contacorrente/saldo?data=2020-02-28` | — | `200` `{"saldo":1234.56}` |
| Lançamentos | `GET /contacorrente/lancamentos?dataInicio=&dataFim=` | — | `200`, lista |

### Ativos e mercado

| Operação | Rota | Sucesso |
|---|---|---|
| Listar | `GET /ativos` | `200` |
| Consultar | `GET /ativos/{ativo}` | `200` |
| Criar | `POST /ativos` | `201`, sem corpo |
| Alterar | `PUT /ativos/{ativo}` | `200`, ativo atualizado |
| Remover | `DELETE /ativos/{ativo}` | `204` |
| Definir preço na data | `PUT /ativos/{ativo}/precos/{data}` | `201` ao criar, `200` ao substituir |
| Excluir preço na data | `DELETE /ativos/{ativo}/precos/{data}` | `204` |

Corpo do ativo: `{"ativo":"ATIVO1","nome":"Ação 1","tipo":"RV","dataEmissao":"2020-01-02","dataVencimento":"2021-01-02"}`.
Tipos válidos: `RV`, `RF`, `FUNDO`. Todos os campos são obrigatórios.

O ativo **não tem** campo de preço de mercado: o preço vive datado, em `PUT /ativos/{ativo}/precos/{data}`.
Enviar `precoMercado` no corpo do ativo responde `400` em vez de ser ignorado — tolerar o campo
esconderia um bug do cliente.

As rotas de preço não vêm do enunciado, que não as especifica. Escolhi `PUT` sobre
`{ativo}/precos/{data}` porque a data é a chave natural do preço e casa com o índice único
`(ativo_id, data)`: o `PUT` fica idempotente por definição, sem precisar decidir à parte o que
acontece ao recriar um preço que já existe.

### Movimentação e posição

| Operação | Rota | Corpo | Sucesso |
|---|---|---|---|
| Compra | `POST /movimentacao/compra` | `{"ativo":"ATIVO1","data":"2020-02-28","quantidade":2.5,"valor":105.53}` | `201`, sem corpo |
| Venda | `POST /movimentacao/venda` | idem | `201`, sem corpo |
| Consulta | `GET /movimentacao?dataInicio=&dataFim=` | — | `200`, lista |
| Posição | `GET /posicao?data=2020-02-28` | — | `202` `{"id":42}` |
| Resultado da posição | `GET /posicao/{id}` | — | `425` ou `200` |

Cada linha de posição traz nome, tipo, quantidade total, preço de mercado, valor de mercado total,
preço médio, rendimento e lucro:

```json
{"ativo":"ATIVO1","nome":"Ação 1","tipo":"RV","quantidade":2.50,
 "precoMercado":105.53000000,"valorMercadoTotal":263.82,
 "precoMedio":100.00000000,"rendimento":1.05530000,"lucro":-250.00}
```

### Formato de erro

Todo 4xx e 5xx responde no mesmo formato, com mensagem em pt-BR e **sem** SQL, stack trace, caminho
de arquivo ou credencial:

```json
{"codigo":"SALDO_INSUFICIENTE","mensagem":"O saldo ficaria negativo em alguma data a partir da data informada."}
```

| `codigo` | Status |
|---|---|
| `REQUISICAO_INVALIDA` | 400 |
| `CAMPO_INVALIDO` | 400 |
| `FILTRO_INVALIDO` | 400 |
| `DATA_NAO_UTIL` | 400 |
| `FORA_DA_JANELA` | 400 |
| `NAO_AUTENTICADO` | 401 |
| `CAPACIDADE_NEGADA` | 403 |
| `ATIVO_NAO_ENCONTRADO` / `RECURSO_NAO_ENCONTRADO` | 404 |
| `ATIVO_DUPLICADO` / `SALDO_INSUFICIENTE` / `QUANTIDADE_INSUFICIENTE` / `ATIVO_EM_USO` | 409 |
| `EXCESSO_DE_EXECUCOES` | 429 |
| `ERRO_INTERNO` | 500 |

## Códigos de status HTTP

O enunciado pede as escolhas justificadas:

| Status | Quando | Por quê |
|---|---|---|
| **200** | `GET` e `PUT` bem-sucedidos | leitura ou alteração concluída, com representação a devolver |
| **201** | criação de lançamento, movimentação, ativo ou preço | um fato novo foi persistido; o corpo é omitido nos POSTs canônicos porque o enunciado diz que não precisam responder conteúdo |
| **202** | criação da execução assíncrona | a requisição foi **aceita**, não concluída — que é exatamente a semântica da opção B |
| **204** | `DELETE` bem-sucedido | a remoção terminou e não há representação a devolver |
| **400** | JSON malformado, campo ausente, escala ou sinal inválidos, filtro ausente/invertido, fim de semana, fora da janela | o cliente pode corrigir sozinho, sem consultar o estado do servidor. Fim de semana e janela são propriedades **da data enviada**, avaliadas antes de qualquer leitura financeira — por isso 400 e não 409 |
| **401** | Basic ausente ou inválido | respondido **antes** de qualquer verificação de existência, para que o 404 não vire oráculo de enumeração |
| **403** | capacidade incompatível com o papel | o usuário é conhecido; o que falta é permissão |
| **404** | ativo, recurso ou execução inexistente | inclui execução de **outro** usuário: um 403 ali confirmaria que o id existe |
| **409** | saldo ou quantidade insuficiente, ativo duplicado, ativo em uso | a requisição é válida, mas conflita com o **estado financeiro persistido** — o cliente precisaria mudar o estado, não o payload |
| **425** | execução assíncrona ainda em andamento | "cedo demais"; é o status que o enunciado especifica |
| **429** | execuções pendentes demais para o usuário | backpressure explícito, em vez de deixar a fila crescer sem limite |
| **500** | falha não mapeada | inclui falha da execução assíncrona, que **nunca** é mascarada como 425 |
| **503** | contenção de escrita não resolvida no `busy_timeout` | falha do servidor e retentável pelo cliente |

A fronteira que organiza a tabela: **400 é erro de payload, 409 é conflito com o estado, 500 é culpa
do servidor.**

## Precisão numérica e arredondamento

O enunciado é explícito: valores em reais não têm fração de centavo, preços unitários podem ter mais
casas, e o arredondamento é **sempre para baixo**.

Nenhum `double` ou `float` existe no código de produção — e isso é verificado pelo pipeline de
review. `105.53` não é representável em ponto flutuante binário, e o erro aparece no centavo depois
de algumas somas.

| Grandeza | Escala | Armazenamento |
|---|---|---|
| Dinheiro | 2 casas | `long` de centavos, `INTEGER` no banco |
| Quantidade | 2 casas | `long` em unidades de 10⁻², `INTEGER` |
| Preço unitário | 8 casas | `long` em unidades de 10⁻⁸, `INTEGER` |

Na fronteira JSON os números entram como `BigDecimal` (`USE_BIG_DECIMAL_FOR_FLOATS`), nunca como
`double`. Escala excedente é rejeitada com 400, não truncada em silêncio.

**Onde o floor entra.** As somas intermediárias ficam em inteiros exatos; o truncamento acontece
**apenas no quociente**, já reduzido à escala do resultado:

```
preço médio = (Σ valor das compras, em centavos) ÷ (Σ quantidade das compras, em 10⁻²)
```

Os dois fatores de escala 10⁻² se cancelam na razão, então a divisão é feita uma vez, no fim. Duas
compras — 1 unidade por R$ 10,00 e 3 unidades por R$ 60,00 — dão preço médio **17,50** (ponderado
pela quantidade), não 15,00 (média aritmética dos preços). Isso é um caso de teste dedicado.

## Regras temporais

- **Saldo e posição são por data**, considerando os fatos até ela, inclusive.
- Movimentações só ocorrem entre a **emissão (inclusive)** e o **vencimento (exclusive)**, de segunda
  a sexta.
- **Não existe calendário de feriados.** Nem o enunciado nem qualquer outro insumo define um, e
  inventá-lo mudaria em silêncio o conjunto de datas aceitas.
- Consultas de lista exigem `dataInicio` e `dataFim`, ambos inclusivos; saldo e posição exigem `data`.
- O preço usado é **o mais recente com data menor ou igual** à consulta. Um preço posterior nunca vaza
  para uma consulta anterior.
- **Sem preço elegível**, a posição responde 200 com `precoMercado`, `valorMercadoTotal` e
  `rendimento` nulos; quantidade, preço médio e lucro seguem calculados. Nunca preço zero, nunca
  preço futuro, nunca omitir o ativo — as três alternativas inventariam um valor financeiro falso ou
  esconderiam uma posição que existe.

**A não negatividade vale para toda data, não só para a atual.** Antes de gravar, a aplicação calcula
a série acumulada por data e verifica o mínimo a partir da data do fato. Uma venda retroativa que
deixaria a quantidade negativa numa data posterior é recusada com 409, mesmo que a data de hoje
ficasse positiva.

A série é agregada **por data**, e não linha a linha: um fato novo entra no fim do seu dia e não
altera os estados intermediários que já aconteceram dentro dele. Minimizar linha a linha rejeitaria
uma compra legítima só porque o saldo tocou zero entre dois lançamentos do mesmo dia.

## Opção A — multiusuário e segurança

**HTTP Basic em toda requisição.** Sem credencial válida, a resposta é `401` com
`WWW-Authenticate: Basic realm="minerva"`.

Pré-cadastrados: `usuario0`–`usuario9` com senhas `senha0`–`senha9`, e o administrador `root` com
senha `spiderman`.

### Estratégia de segurança e decisões

- **Senhas em BCrypt**, nunca em claro. O hash é gerado na inicialização.
- **O cabeçalho `Authorization` nunca aparece em log**, nem em falha de autenticação.
- **Comparação de custo constante para login inexistente.** Quando o login não existe, ainda assim é
  feita uma verificação BCrypt contra um hash descartável. Sem isso, o tempo de resposta diria a um
  atacante quais logins existem.
- **O 401 precede qualquer verificação de existência.** Se uma rota respondesse 404 a um anônimo, o
  próprio código de status enumeraria os recursos.
- **A execução assíncrona de outro usuário responde 404, não 403.** Um 403 confirmaria que aquele id
  existe.
- **O filtro de proprietário está no predicado SQL e no caso de uso**, não na serialização. Filtrar só
  na saída é como dado de um usuário chega à resposta de outro.
- **A autorização por capacidade vive na camada de aplicação**, não na rota: uma segunda porta de
  entrada precisaria da mesma regra, e duplicá-la no adaptador é como uma das cópias fica velha.

### Capacidades

| Rota | `usuarioN` | `root` |
|---|---|---|
| Crédito, débito, compra, venda | permitido, sobre os próprios dados | `403` |
| Saldo, lançamentos, movimentações, posição | somente os próprios | `403` |
| `GET /ativos`, `GET /ativos/{ativo}` | permitido | permitido |
| `POST`/`PUT`/`DELETE /ativos` e preços | `403` | permitido |

Os dois papéis são **disjuntos de propósito**: o administrativo gerencia o acervo compartilhado e não
transaciona; o comum transaciona e enxerga apenas os próprios dados.

## Opção B — consulta assíncrona de alto volume

```
GET /posicao?data=2020-02-28          →  202  {"id":42}
GET /posicao/42                       →  425 enquanto processa
                                      →  200 com o conteúdo, uma única vez
                                      →  404 depois de entregue, expirado ou de outro usuário
```

### Como a memória fica constante

O ponto central: **as movimentações nunca são materializadas**. Não existe `List<Movimentacao>` nem
`collect()` sobre o stream de fatos.

1. A leitura usa `PreparedStatement` com `fetchSize` definido e um `RowCallbackHandler`: o cursor
   avança sob demanda e cada linha é descartada logo após ser somada.
2. O estado acumulado é um `Acumulador` **por ativo** — quatro `long` cada. Com 128 ativos, isso é da
   ordem de kilobytes e **não cresce** com o número de movimentações, sejam 100 ou 200.000.
3. O que fica guardado até a entrega é a lista de posições, limitada ao número de ativos.
4. O resultado é **descartado na primeira entrega**, por remoção atômica. Entre duas consultas
   simultâneas do mesmo id, exatamente uma recebe 200 e a outra 404.
5. Execuções nunca consultadas **expiram em 10 minutos**. Sem isso, "memória constante" valeria para
   uma execução, não para o processo.
6. **Backpressure**: no máximo 4 execuções pendentes por usuário; a próxima recebe `429`.

### Processamento paralelo

O trabalho é particionado **por identificador de ativo** (`ativo_id % N`). A agregação de um ativo é
independente da de outro e cada ativo cai em exatamente uma partição, então a combinação final é uma
concatenação: não há estado compartilhado entre threads nem merge a coordenar.

`N` é `min(4, processadores disponíveis)`, com mínimo 2. As threads chamam-se `posicao-worker-<i>` —
o nome é o que torna o paralelismo observável de fora, sem depender de inferir concorrência a partir
de tempo de resposta.

São **dois pools**, não um. O coordenador de uma execução fica bloqueado esperando as partições dela;
se coordenador e partições disputassem o mesmo pool fixo, bastariam execuções simultâneas suficientes
para que todos os threads estivessem bloqueados esperando tarefas que jamais seriam agendadas — um
deadlock de auto-submissão. Ambos são separados do pool que atende o HTTP, para que uma consulta
pesada não consuma os threads que respondem as demais rotas.

Uma execução que falha responde `500`, **nunca** `425`: apresentar erro como "ainda processando"
faria o cliente esperar para sempre por um resultado que não virá.

## Thread-safety e concorrência

O enunciado pede as alterações necessárias descritas. São estas:

1. **Nenhum estado mutável de requisição em bean singleton.** Os serviços guardam apenas
   dependências. O principal autenticado vive em `ThreadLocal`, definido pelo filtro e **sempre**
   limpo em `finally`, para que a thread devolvida ao pool não carregue o usuário da requisição
   anterior. Guardar o usuário em um campo de bean é como uma aplicação concorrente passa a responder
   os dados de um usuário para outro.
2. **`BEGIN IMMEDIATE` em toda transação que lê para validar e depois escreve.** Em modo `DEFERRED`, a
   promoção de leitura para escrita no meio da transação abre uma janela entre validar e gravar e
   produz `SQLITE_BUSY` não retentável. Com `IMMEDIATE`, o escritor já está serializado quando a
   validação roda — é isso que faz "verificar saldo e depois debitar" ser atômico de verdade.
3. **`journal_mode = WAL`**, para que leitores não bloqueiem o escritor nem uns aos outros.
4. **`busy_timeout = 5000`**, para absorver contenção curta em vez de falhar de imediato.
5. **Nenhum lock de aplicação.** Não há semáforo nem `ReentrantLock` serializando escritores:
   `BEGIN IMMEDIATE` mais `busy_timeout` já fazem isso no nível certo, e um lock em memória não
   protegeria um segundo processo — seria falsa garantia com custo de manutenção.
6. **Estruturas concorrentes** (`ConcurrentHashMap`, `AtomicLong`) no registro de execuções
   assíncronas, com descarte por `remove(chave, valor)` — comparação e remoção atômicas.
7. **Pools dedicados e nomeados** para a opção B, separados do pool do servidor HTTP.

O limite conhecido: SQLite tem **um escritor por vez**. Isso é tensão real com o requisito de
desempenho concorrente, e está tratada por transações curtas, WAL e `busy_timeout` — não escondida.
Leituras, essas sim, correm em paralelo.

## Desempenho: decisões e medições

Decisões tomadas para desempenho sob concorrência:

- Leitura por cursor, sem materializar as movimentações.
- Agregação O(1) por ativo, em aritmética inteira — sem `BigDecimal` no laço quente.
- Índices em `(usuario_id, data)`, `(ativo_id, data)` e um índice único em `(ativo_id, data)` do preço.
- Preço vigente de todos os ativos resolvido em **uma** consulta, não uma por ativo.
- Transações curtas: a validação e a escrita ocorrem na mesma unidade, e nada mais.
- Paralelismo por partição independente, sem estado compartilhado.

### Medições

Ambiente: WSL2, JDK 25 (Corretto 25.0.3), SQLite embarcado, executadas por
`./mvnw -f backend/pom.xml test -Dtest=PosicaoDesempenhoIT`.

| Métrica | Alvo | Medido |
|---|---|---|
| Movimentações processadas | 200.000 | **200.000** |
| Duração da consulta de posição | — | **639 ms** |
| Variação de heap durante a consulta | < 64 MB | **26,4 MB** |
| Threads de agregação distintas | ≥ 2 | **4** (`posicao-worker-0..3`) |

O heap é medido com `Runtime.totalMemory() - freeMemory()` imediatamente antes e depois da consulta,
com um `System.gc()` antes da primeira leitura para reduzir o ruído do lixo acumulado pela carga. A
medição é de heap, sujeita ao ruído do coletor; o que ela demonstra é a **ordem de grandeza** — o
consumo não acompanha o volume lido.

O teste de concorrência do contrato (TC-063) dispara 20 débitos simultâneos de R$ 10,00 sobre um
saldo de R$ 100,00 e verifica que exatamente 10 são aceitos, nenhum termina em erro de servidor e o
saldo final é R$ 0,00.

## Interface web

React 19 com TypeScript, empacotado por Vite. Escolhi client-side porque o enunciado permite
JavaScript/TypeScript nesse caso e porque a interface consome exatamente os mesmos endpoints REST que
um avaliador chamaria por `curl` — não há um segundo caminho, renderizado no servidor, que pudesse
divergir do contrato público.

**Execução em desenvolvimento**, com recarga automática:

```bash
npm --prefix frontend ci
npm --prefix frontend run dev      # http://localhost:5173
```

O Vite faz proxy das rotas da API para `localhost:8080`, então o navegador conversa só com o Vite e
não há CORS a configurar. Em produção a interface é servida pelo próprio Spring Boot, no mesmo host,
e o problema não existe.

| Comando | Efeito |
|---|---|
| `npm --prefix frontend run dev` | servidor de desenvolvimento com proxy |
| `npm --prefix frontend run build` | verificação de tipos e bundle em `frontend/dist` |
| `npm --prefix frontend run check` | só a verificação de tipos |

**Quatro destinos**, guiados por um cursor de data global no cabeçalho: conta corrente, carteira,
ativos e mercado histórico. O cursor é persistido na URL (`?destino=carteira&data=2020-02-28`), de
modo que um link compartilhado e o botão voltar do navegador mostrem a mesma data que a pessoa via.

**Decisões que valem registro:**

- **A credencial fica em memória, nunca em `localStorage`.** HTTP Basic é a senha em claro codificada
  em base64; gravá-la em disco a deixaria legível para qualquer script na origem. O custo aceito é
  ter de entrar de novo ao recarregar a página.
- **Os papéis mudam a interface, não só as permissões.** O `root` não vê conta nem carteira; os
  destinos ficam visíveis, porém desabilitados e com o motivo no tooltip. Esconder rotas faria os
  dois papéis verem aplicações diferentes, e a diferença viraria suporte.
- **`425` é progresso, nunca erro.** O recálculo em segundo plano mostra skeleton enquanto a execução
  não termina, e após 60 segundos oferece continuar aguardando ou cancelar, em vez de uma espera
  infinita e silenciosa.
- **Sem preço de mercado elegível, a tela mostra `—` com explicação**, nunca `R$ 0,00`. Zero seria um
  valor financeiro falso.
- **A aplicação não opera offline e não enfileira escritas.** Um lançamento enfileirado seria aplicado
  numa data e num saldo diferentes dos que a pessoa viu ao criá-lo.
- **Nenhum estado depende só de cor:** entradas e saídas levam seta e sinal, o destino ativo tem barra
  além do realce, e o estado de erro tem ícone.

O design é contrato auditável, não improviso: tokens, tipografia, responsividade e acessibilidade
estão em `docs/design/`, e cada tela, estado e formato foi especificado em
[`docs/design/telas.md`](docs/design/telas.md) **antes** do código. O que continua sem desenho está
declarado em [`docs/design/nao-desenhado.md`](docs/design/nao-desenhado.md) e não foi improvisado.

## Testes

```bash
./mvnw -f backend/pom.xml test
```

A suíte roda repetidamente e em qualquer ordem: cada caso cria os próprios dados com código de ativo
exclusivo, e a limpeza entre casos devolve o banco ao estado do seed, mesmo quando um caso falha.

Essa limpeza é global, não escopada ao caso, o que é correto porque os testes rodam em sequência — e
está dito aqui porque tem consequência: habilitar execução paralela exigiria trocá-la por uma limpeza
por caso. Os casos que exercitam o limite de execuções assíncronas retidas, que é estado em memória e
não em banco, usam um login exclusivo cada.

| Suíte | O que cobre |
|---|---|
| `ValoresMonetariosTest` | escalas, sinais, faixa e conversões de `Dinheiro`, `Quantidade`, `PrecoUnitario` |
| `JanelaDeNegociacaoTest` | emissão inclusiva, vencimento exclusivo, dias úteis, ausência de feriados |
| `CalculoPosicaoTest` | média ponderada, floor no quociente, rendimento, lucro, ausência de preço |
| `ContaCorrenteIT` | crédito, débito, saldo, filtros, validações, débitos concorrentes |
| `AtivoIT` | CRUD, duplicidade sob concorrência, preço por data, seleção histórica |
| `MovimentacaoIT` | compra, venda, atomicidade, janela, fim de semana, inserção fora de ordem |
| `PosicaoIT` | as fórmulas ponta a ponta, incluindo posição sem preço elegível |
| `EscalasIT` | as escalas do enunciado na fronteira HTTP |
| `SegurancaIT` | Basic, capacidades, isolamento entre usuários, concorrência |
| `ContratoN3IT` | as cinco rotas canônicas com o payload literal do enunciado, seed, sanitização de erro |
| `PosicaoAssincronaIT` | 202, 425, 200, descarte, isolamento, backpressure |
| `PosicaoDesempenhoIT` | 200.000 movimentações, heap e paralelismo |

**Testes de integração de verdade.** Cada um sobe o servidor em porta real e chama a API por
`HttpClient` do JDK — exercita servidor HTTP, JSON, controllers, casos de uso e persistência em
SQLite. Um mock de camada web provaria menos do que o enunciado pede.

Cada chamada publica request e response em `artifacts/testes/<commit>/<TC>/requisicoes.jsonl`, com o
cabeçalho `Authorization` reduzido a um booleano — evidência de teste vira artefato de CI, e
credencial em artefato é credencial vazada.

Dois pipelines em `.github/workflows/`: **`review.yml`** verifica as fronteiras de camada, a ausência
de ponto flutuante e a documentação; **`testes-integracao.yml`** roda a suíte **duas vezes** — a
segunda execução sobre o estado deixado pela primeira é o que prova a limpeza própria de cada caso — e
publica as evidências.

## Deploy

A imagem é multi-estágio: a interface é compilada em um estágio Node, o backend em um estágio Maven,
e a execução acontece em um JRE com usuário sem privilégios. O banco fica em volume próprio, para que
os dados não morram com o contêiner. O resultado é **um artefato só**, servindo API e telas.

```bash
docker compose up --build       # http://localhost:8080
```

**❓ LACUNA — deploy em nuvem.** Não foi realizado nesta entrega. A decisão de quando e em qual
plataforma acionar fica com o usuário; nenhum prazo ou provedor está fixado aqui. O que depende desta
entrega está pronto — `Dockerfile`, `docker-compose.yml`, configuração por variável de ambiente
(`SPRING_DATASOURCE_URL`) e healthcheck. Em qualquer plataforma de free tier que aceite um Dockerfile
(Fly.io, Render, Railway, Koyeb são exemplos, não uma escolha feita), o passo é apontar o serviço para
este repositório, expor a porta 8080 e montar um volume em `/app/data`; o passo em si exige
credenciais da plataforma escolhida, que são do dono do repositório.

## Premissas assumidas

Onde o enunciado não especifica, a escolha está isolada e documentada, em vez de escondida:

1. **Valores do seed.** O enunciado fixa os *nomes* `ATIVO0`–`ATIVO127` e a *data* `2020-01-02` do
   valor de mercado, mas não o nome de exibição, o tipo, a janela nem o preço de cada ativo. A regra
   usada é determinística e está concentrada em constantes de `SeedInicial`: tipo ciclando entre
   `RV`, `RF` e `FUNDO` pelo índice; emissão `2020-01-01` e vencimento `2030-01-01`; preço
   `10,00 + índice × 0,37`, de R$ 10,00 a R$ 56,99. Trocar essa regra é editar uma linha.
2. **Rotas de preço de mercado por data.** Não fornecidas no enunciado; a escolha e o motivo estão em
   [Contrato da API](#contrato-da-api).
3. **Fuso oficial `America/Sao_Paulo`**, para a noção de "dia".
4. **Dados criados antes da opção A pertencem a `usuario0`.** Não há migração de propriedade, porque
   não haveria como adivinhar o dono correto.

## O que não foi entregue

- **❓ LACUNA — deploy em nuvem.** Não realizado nesta entrega, por decisão do usuário de adiar; o
  passo final depende de credenciais da plataforma escolhida, ainda não definida. Ver [Deploy](#deploy).
- **Sparkline de patrimônio e exportação em CSV/PDF.** Não há requisito para nenhum dos dois, e ambos
  seguem declarados como não desenhados em `docs/design/nao-desenhado.md`.
- **Tema escuro.** Decisão explícita, não omissão: inverter os tokens automaticamente quebraria as
  relações de contraste verificadas, e um tema escuro exige desenho próprio.
