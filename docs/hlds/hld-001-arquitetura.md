### HLD: Arquitetura Minerva Finanças

Versão: 1.1
Data: 2026-09-02
Responsável: Yoda
Revisor: Patrick Jane

---

### Objetivo técnico

> **Atualização de 2026-09-02:** a organização concreta do backend — pastas, camadas e nomenclatura —
> passou a ser decidida pela [ADR-003](../adrs/adr-003-estrutura-do-backend.md), que substitui a
> estrutura de portas e adaptadores descrita adiante. O que segue valendo deste HLD é a **direção de
> dependência** (o domínio não conhece transporte nem persistência) e todo o restante do documento:
> fluxos, modelo de dados, segurança, observabilidade e riscos.

Organizar o sistema em camadas, isolando o domínio de transporte HTTP, JSON, framework e persistência. O domínio usa invariantes financeiros e contratos próprios; adaptadores traduzem entradas e saídas sem importar detalhes de infraestrutura para o núcleo.

Dependências com outros sistemas
- Interface React com TypeScript, consumidora dos contratos REST.
- Banco embarcado SQLite e processo standalone.
- Dockerfile e docker-compose no nível 3; as opções A (Basic/isolamento) e B (posição assíncrona) são cumulativas e obrigatórias.

---

### Arquitetura geral

Topologia de um processo standalone com frontend consumidor e API. A direção de dependência é interface e infraestrutura -> aplicação -> domínio; o domínio não depende de Spring, JDBC, SQLite, HTTP ou React. Portas de entrada e saída definem contratos, e adaptadores implementam essas portas.

Ambiente de implantação
- Standalone inicialmente; artefatos de container e possibilidade de cloud no nível 3.
- SQLite local com WAL, `busy_timeout` e escrita serializada. A capacidade do escritor único é tensão explícita com thread-safety e concorrência do nível 3, tratada por coordenação e testes, não escondida.

Tecnologias principais
- Java 25, Spring Boot 4.1.1 e Maven, conforme [ADR-001](../adrs/adr-001-stack-da-aplicacao.md).
- Spring JDBC em vez de JPA: a opção B exige leitura incremental com limite de heap; JPA pode carregar grafo e manter cache de primeiro nível proporcional à unidade de trabalho.
- SQLite embarcado; React e TypeScript na fronteira do consumidor.

Padrões adotados
- DDD, portas e adaptadores, transações explícitas, leitura incremental e função de janela SQL.
- Dinheiro como `long` de centavos dentro do domínio. Preços unitários de maior escala só são convertidos para o cálculo monetário com floor definido no PRD.

---

### Componentes e responsabilidades
| Componente | Responsabilidades | Dependências |
|---|---|---|
| Domínio | entidades, valores, regras, fórmulas e invariantes | nenhuma de transporte/persistência |
| Aplicação | casos de uso, transação, autorização por capacidade e orquestração | portas de entrada/saída, domínio |
| Adaptador REST | JSON, rotas, status, Basic auth e mapeamento de erro | aplicação, framework |
| Persistência | SQL, mapeamento, transações e leitura incremental | portas, JDBC, SQLite |
| Worker de posição | id de execução, paralelismo, 425 e descarte | aplicação, portas, executor |
| React/TypeScript | cursor de data na URL, telas e acessibilidade | API REST, contrato design |

---

### Fluxo de requisições e de dados
**Fluxo de requisição**
- REST recebe JSON e autentica quando opção A estiver ativa.
- Adaptador valida forma e delega à aplicação; domínio valida invariantes.
- Porta de persistência executa transação; adaptador converte resultado ou erro em contrato HTTP.

**Fluxo de dados**
- Entrada -> valor de domínio -> caso de uso -> fatos de lançamento/movimento -> SQLite.
- Consulta -> SQL filtrado por data -> agregação incremental -> DTO de saída.
- Validação de não negatividade: usar `SUM(...) OVER (ORDER BY data)` e `MIN` sobre datas maiores ou iguais à data do lançamento para detectar qualquer saldo ou quantidade negativa futura antes de confirmar.
- Opção B percorre resultados incrementalmente, particiona trabalho independente e não mantém todas as movimentações em memória.

---

### Modelo de dados (alto nível)
Entidades principais
- Conta, lançamento, ativo, movimento, preço de mercado, usuário e execução de posição.

Relações
- Ativo possui movimentos e preços históricos.
- Conta possui lançamentos; compra/venda cria fato financeiro correlato.
- Execução referencia data e estado, sem reter resultado após entrega.
- Usuário é proprietário dos dados privados na opção A; ativos e preços de mercado são compartilhados conforme autorização.

Fonte de verdade
- Fatos persistidos no SQLite. Memória, cache de request e resultado assíncrono não são fonte de verdade.
- Não existe tabela de conta com saldo materializado: a conta corrente é a projeção dos lançamentos do proprietário ([FDD-001](../fdds/fdd-001-nivel-1.md), D-A5). `Conta` é agregado de domínio, não tabela.
- Preço unitário e quantidade são persistidos como inteiros escalados (10⁻⁸ e 10⁻²), nunca com afinidade `NUMERIC`/`REAL`, para não violar a proibição de ponto flutuante da [ADR-001](../adrs/adr-001-stack-da-aplicacao.md) ([FDD-001](../fdds/fdd-001-nivel-1.md), D-A2).

---

### Interfaces públicas
| Nome | Tipo | Protocolo | Exposição | SLAs/Limites |
|---|---|---|---|---|
| Conta corrente | API | REST JSON | Externa | filtros e payloads do PRD; meta de latência TBD |
| Ativos e mercado | API | REST JSON | Externa | campos e escalas do PRD |
| Movimentação | API | REST JSON | Externa | quantidade até 2 casas; operação atômica |
| Posição síncrona/assíncrona | API | REST JSON | Externa | opção B responde 425 sem bloquear a consulta |

---

### Considerações de escalabilidade e disponibilidade
Abordagem geral
- Consultas paginadas ou incrementais e índices temporais reduzem materialização; a opção B separa criação da execução e usa processamento paralelo.

Técnicas aplicadas
- WAL para leitores concorrentes, `busy_timeout` para contenção curta e escrita serializada pelo próprio SQLite. Toda transação que lê para validar e depois escreve abre em `BEGIN IMMEDIATE` ([FDD-001](../fdds/fdd-001-nivel-1.md), D-A7); não há lock de aplicação, por ser falsa garantia fora do processo. Thread-safety exige não compartilhar estado mutável de request e coordenar o ponto único de escrita.
- Backpressure e limite de execuções são necessários. O cenário verificável da opção B é 200.000 movimentações, variação de heap abaixo de 64 MB medida por `Runtime` antes/depois e mínimo de 2 threads observáveis; reproduzir pelo comando `./mvnw -pl backend -Dtest=PosicaoDesempenhoIT test`.

Meta de disponibilidade
- TBD; execução standalone é requisito, não promessa de SLA.

---

### Segurança
Autenticação
- Opção A usa HTTP Basic em todas as requisições; armazenamento e proteção operacional das credenciais de produção são ❓ LACUNA.

Autorização
- Administrador gerencia ativos/mercado e não transaciona; usuário comum transaciona e recebe somente seus dados. O filtro de proprietário ocorre antes da saída.

Proteção de dados
- Não expor credenciais em logs. Criptografia em trânsito, repouso e retenção são ❓ LACUNA conforme escopo não desenhado.

Gestão de segredos
- ❓ LACUNA: mecanismo de gestão de segredos não foi decidido.

---

### Observabilidade
Logs
- Logs estruturados com correlação, rota, status, duração, usuário pseudonimizado, motivo de rejeição e id de execução; nunca senha ou payload financeiro completo.

Métricas
- Contagem e latência por endpoint, rejeições por regra, contenção/busy timeout, fila do escritor, memória do worker, paralelismo e quantidade de respostas 425.

Tracing
- Spans de entrada REST, caso de uso, transação SQL e worker; amostragem e exportador são TBD.

Dashboards e alertas
- Erros HTTP, conflitos de escrita, crescimento de fila, memória da opção B e taxa de 425; limiares são TBD.

---

### Riscos arquiteturais e mitigação
#### Escritor único limita concorrência
- **Probabilidade:** alta
- **Impacto:** contenção, timeout ou falsa percepção de desempenho.
- **Mitigação:** WAL, `busy_timeout`, transações curtas, serialização explícita e teste concorrente.
- **Plano de contingência:** limitar operações simultâneas e registrar ADR caso outro armazenamento seja necessário.

#### ORM materializa memória excessiva
- **Probabilidade:** média
- **Impacto:** opção B excede a variação de heap de 64 MB.
- **Mitigação:** Spring JDBC, cursores/leitura incremental e medição de heap.
- **Plano de contingência:** bloquear opção B até reduzir materialização.

#### Fronteira permite vazamento entre usuários
- **Probabilidade:** média
- **Impacto:** violação grave de privacidade.
- **Mitigação:** autorização no caso de uso, filtros por proprietário e testes cruzados.
- **Plano de contingência:** retirar a rota afetada e preservar auditoria do incidente.

---

### ADRs e próximos passos
ADRs associados
- [ADR-001 Stack da aplicação](../adrs/adr-001-stack-da-aplicacao.md).
- [ADR-002 Direção visual](../adrs/adr-002-direcao-visual.md).
- [ADR-003 Estrutura de pacotes e camadas do backend](../adrs/adr-003-estrutura-do-backend.md), que substitui a organização de pacotes deste HLD.

Decisões pendentes
- As opções A e B são obrigatórias e cumulativas; não há decisão pendente sobre alternativa.
- ❓ LACUNA: metas de latência, disponibilidade e tracing não são necessárias para o critério D3 desta rodada. O limite de execuções pendentes deixou de ser lacuna: está fixado em quatro por usuário no [FDD-003](../fdds/fdd-003-nivel-3-e-opcoes.md), D-C3.
- ❓ LACUNA: gestão de segredos e criptografia em trânsito/repouso. **O tratamento de ausência de preço histórico deixou de ser lacuna**: está decidido no [FDD-002](../fdds/fdd-002-nivel-2-datas.md), D-B3, na revisão arquitetural de 2026-09-02.
- Nomes de pacote e direção de dependência entre camadas deixaram de ser pendência: com a ADR-001 aceita, o mapa normativo está no [FDD-001](../fdds/fdd-001-nivel-1.md), item 7, com pacote base `br.com.minerva.financas` e gate verificável no pipeline `review`. Este HLD não mantém segunda cópia da lista, para não divergir.

Próximos passos
- FDD-001, FDD-002 e FDD-003 foram revisados e **aprovados por Yoda em 2026-09-02**; T-001, T-002 e T-003 estão liberadas quanto à dependência de governança. Falta a revisão independente do HLD por Patrick Jane e a decisão do usuário sobre os valores de seed do nível 3.

### Histórico

- 2026-09-02: versão 1.0.
- 2026-09-02: versão 1.1 — revisão arquitetural dos três FDDs. Fechadas as lacunas de ausência de preço histórico, limite de execuções pendentes e fronteiras de pacote; registradas a projeção da conta, a representação exata de preço/quantidade e o modo de transação `BEGIN IMMEDIATE`.
