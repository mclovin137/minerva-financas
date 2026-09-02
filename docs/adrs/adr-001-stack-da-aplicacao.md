---
type: minerva-adr
project: Minerva Finanças
date: 2026-09-02
status: aceita
tags:
  - minerva
  - adr
ai-first: true
---

# ADR 001: Stack da aplicação

## Para o futuro agente

Esta ADR fixa a stack escolhida para os lotes 1–4 do desafio MAPS e importa sempre que uma dependência, camada de persistência ou representação de dados for adicionada.

## Contexto

O desafio exige uma aplicação de finanças pessoais com banco embarcado, APIs REST, testes, execução standalone e, no nível 3, segurança, concorrência e consulta assíncrona de alto volume. A stack precisa ser estabelecida antes do código.

## Escopo

- Afetado: backend Java, persistência SQLite, build Maven, frontend React e testes.
- Fora de escopo: domínio, casos de uso, endpoints, autenticação, frontend implementado e deploy, tratados nos lotes seguintes.

## Forças e critérios

- Java 25, Spring Boot, Maven, banco embarcado e custo financeiro zero.
- Validação de invariantes financeiras com escrita atômica.
- Memória constante na consulta assíncrona de alto volume.
- Representação exata de valores monetários e arredondamento explícito.
- Thread-safety e desempenho concorrente para o nível 3.

## Opções consideradas

### Opção 1: Java 25 + Spring Boot + Maven + SQLite + Spring JDBC + React

- Status na análise: escolhida
- Benefícios: stack decidida pelo usuário; SQLite é embarcado; JDBC permite ResultSet em streaming e memória constante; React atende ao frontend.
- Custos e limitações: SQLite tem um escritor por vez; a aplicação precisa coordenar transações e concorrência; JDBC exige SQL e mapeamento explícitos.
- Riscos: o nível 3 exige thread-safety e bom desempenho concorrente, enquanto o nível 2 exige saldo não negativo em nenhuma data. O fluxo ler-validar-escrever precisa ser atômico, e a serialização pode virar gargalo.
- Trade-off: ganha simplicidade operacional, previsibilidade de memória e controle transacional; aceita maior responsabilidade de SQL, serialização de escrita e evolução de schema.
- Evidências: decisões explícitas do usuário neste task e contrato do enunciado MAPS.

### Opção 2: Java 25 + Spring Boot + Maven + JPA/Hibernate + banco relacional embarcado

- Status na análise: candidata
- Benefícios: abstração de persistência, mapeamento de entidades e convenções de transação prontas.
- Custos e limitações: JPA carrega grafo de entidades e mantém cache de primeiro nível; consultas de alto volume podem consumir memória proporcional às movimentações, contrariando a opção B.
- Riscos: maior complexidade para garantir streaming real, controle fino de SQL e invariantes históricas de saldo/posição.
- Trade-off: reduz SQL manual em troca de menor controle de memória e execução, por isso atende pior à memória constante.
- Evidências: comparação arquitetural desta ADR; desempenho será medido nos lotes 2–4.

## Decisão

Adotar Java 25, Spring Boot, Maven, SQLite embarcado, Spring JDBC no backend e React no frontend. O banco será configurado com WAL e `busy_timeout`; transações que alteram saldo ou posição serializarão a escrita e farão a validação na mesma unidade atômica. Consultas de alto volume usarão streaming, sem acumular movimentações.

Valores financeiros serão armazenados como `long` de centavos (SQLite `INTEGER`), nunca `double` nem `BigDecimal` de escala livre. O enunciado não permite frações de centavos e exige arredondamento sempre para baixo. Preço unitário será tratado separadamente como `BigDecimal` com até 8 casas; quantidade terá até 2 casas.

## Consequências

### Positivas

- Execução standalone sem servidor de banco externo.
- Controle explícito de consultas, transações e uso de memória.
- Representação monetária exata e alinhada ao enunciado.

### Negativas

- Um escritor por vez no SQLite limita paralelismo de gravação.
- JDBC exige contratos SQL, mapeadores e cuidados manuais com recursos.

### Riscos residuais

- Throughput limitado pela escrita serializada; tratar com WAL, `busy_timeout`, transações curtas, índices e testes de carga.
- Validação histórica incompleta pode aceitar saldo ou posição negativa; tratar com validação transacional da linha do tempo e testes de integração.
- Conversões de preço/quantidade podem arredondar indevidamente; tratar com escalas fixas e `FLOOR`.

## Impacto nos artefatos

- `backend/pom.xml`, configuração SQLite e schema dos lotes 1–4.
- `docs/lib.md`, `docs/roadmap.md`, README final e testes.
- Nenhum domínio ou endpoint é implementado nesta ADR.

## Relações

- Origem: task “Lote 1 de 4 do desafio MAPS: governança + fundação”.
- ADRs relacionadas: nenhuma.
- Substitui: nenhuma.
- Substituída por: nenhuma.

## Critério para revisitar

Revisitar se Java/Spring deixar de resolver no Maven Central, se testes de carga demonstrarem que SQLite não atende ao nível 3, ou se a memória constante da opção B não puder ser mantida com JDBC em streaming.

## Aceite

- Responsável arquitetural: Yoda
- Decisor: usuário
- Evidência do aceite: decisões de stack explicitamente fixadas no brief desta task.

## Histórico

- 2026-09-02: ADR criada com status aceita.
