### HLD: Arquitetura Minerva Finanças

Versão: 1.0
Data: 2026-09-02
Responsável: Yoda
Revisor: Patrick Jane

---

### Objetivo técnico

Organizar o sistema em camadas DDD, isolando o domínio de transporte HTTP, JSON, framework e persistência. O domínio usa invariantes financeiros e contratos próprios; adaptadores traduzem entradas e saídas sem importar detalhes de infraestrutura para o núcleo.

Dependências com outros sistemas
- Interface React com TypeScript, consumidora dos contratos REST.
- Banco embarcado SQLite e processo standalone.
- Dockerfile e docker-compose no nível 3.

---

### Arquitetura geral

Topologia de um processo standalone com frontend consumidor e API. A direção de dependência é interface e infraestrutura -> aplicação -> domínio; o domínio não depende de Spring, JDBC, SQLite, HTTP ou React. Portas de entrada e saída definem contratos, e adaptadores implementam essas portas.

Ambiente de implantação
- Standalone inicialmente; artefatos de container e possibilidade de cloud no nível 3.
- SQLite local com WAL, `busy_timeout` e escrita serializada. A capacidade do escritor único é tensão explícita com thread-safety e concorrência do nível 3, tratada por coordenação e testes, não escondida.

Tecnologias principais
- Java 25, Spring Boot 4.1.1 e Maven, conforme [ADR-001](../adrs/adr-001-stack-da-aplicacao.md).
- Spring JDBC em vez de JPA: a opção B exige memória constante; JPA pode carregar grafo e manter cache de primeiro nível proporcional à unidade de trabalho.
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
- WAL para leitores concorrentes, `busy_timeout` para contenção curta e fila/escritor serializado para SQLite. Thread-safety exige não compartilhar estado mutável de request e coordenar o ponto único de escrita.
- Backpressure e limite de execuções são necessários; valores quantitativos permanecem TBD.

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
- **Impacto:** opção B viola memória constante.
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

Decisões pendentes
- ❓ LACUNA: escolher opção A ou B para a entrega final do nível 3, se ambas não forem mantidas.
- ❓ LACUNA: metas quantitativas de latência, concorrência, disponibilidade, tracing e limites de fila.
- ❓ LACUNA: gestão de segredos, criptografia e tratamento de ausência de preço histórico.

Próximos passos
- Revisão independente do HLD, elaboração/revisão dos FDDs e só então execução das tasks correspondentes.
