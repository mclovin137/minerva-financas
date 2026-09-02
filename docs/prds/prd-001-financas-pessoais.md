### PRD: Minerva Finanças pessoais

Versão: 1.0
Data: 2026-09-02
Responsável: Yoda
Revisor: Patrick Jane

---

### Resumo

Permitir registrar e consultar conta corrente e carteira de ativos financeiros, preservando integridade financeira por data. O produto atende o desafio MAPS em níveis progressivos e entrega APIs REST JSON, interface responsiva conforme o contrato de design e execução standalone.

---

### Contexto e problema

Público-alvo
- Usuário comum que registra entradas, saídas, compras e vendas e consulta seus próprios dados.
- Usuário administrativo que gerencia ativos e valores de mercado, sem transacionar nem consultar dados financeiros confidenciais.

Cenários de uso chave
- Consultar saldo e posição na data de análise.
- Registrar crédito, débito, compra ou venda e receber rejeição objetiva quando uma regra temporal ou financeira for violada.
- Administrar ativos e preços de mercado conforme o nível entregue.

Onde essa feature será implantada
- Sistema novo, executável standalone e, no nível 3, preparado para deploy em cloud, sem custo financeiro de infraestrutura assumido.

Problemas priorizados
- Registros financeiros sem visão temporal podem produzir saldo ou posição incorretos, prioridade alta.
- Operações inválidas podem gerar saldo negativo, quantidade negativa ou cálculo financeiro inconsistente, prioridade alta.
- Contratos diferentes entre níveis podem quebrar clientes e testes, prioridade alta.

---

### Objetivos e métricas

| Objetivo | Métrica | Meta |
|---|---|---|
| Preservar integridade monetária | casos de centavos, arredondamento e invariantes aprovados | 100% dos casos obrigatórios passam |
| Tornar consultas temporais confiáveis | casos de data, janela útil e preço histórico | 100% dos casos obrigatórios passam |
| Expor contratos consumíveis | endpoints com payload/status documentados e testados | 100% dos endpoints possuem teste de integração reexecutável, com fixture isolada e limpeza, rollback ou reset entre casos |
| Suportar uso concorrente no nível 3 | testes concorrentes sem isolamento ou corrupção | zero violações observadas; meta de latência p95 é TBD |

---

### Escopo

Incluso
- Nível 1: saldo, crédito, débito, CRUD de ativos, compra, venda e posição.
- Nível 2: datas de movimento, emissão e vencimento, preço por data e consultas temporais.
- Nível 3: endpoints exatos, seed, autenticação e isolamento da opção A ou consulta assíncrona da opção B, Dockerfile e docker-compose.
- Valores em reais sem fração de centavo, validações e arredondamento para baixo.
- REST, JSON, códigos HTTP adequados, testes unitários e testes de integração de API e persistência.
- Interface seguindo [design system](../design/README.md), quando a tela correspondente estiver desenhada.

Fora de escopo
- Escolher arquitetura, banco, hospedagem, CI ou framework além das decisões já registradas nas ADRs; isso pertence ao HLD/ADRs.
- Tema escuro, offline, login visual detalhado, estados vazios, sessão expirada, erros de rede e skeletons ainda não desenhados. Ver [lacunas](../design/nao-desenhado.md).
- Features não presentes no enunciado MAPS, relatórios, transferências entre contas ou múltiplas moedas.

---

### Requisitos funcionais

#### FR-001 Saldo e lançamentos do nível 1
Permitir consultar o saldo da conta corrente, incluir crédito com valor positivo e descrição e incluir débito com valor positivo e descrição. Crédito aumenta e débito reduz o saldo. O saldo não pode ficar negativo.

**Fluxo principal**
- Receber valor e descrição válidos.
- Validar o lançamento contra o saldo resultante.
- Persistir atomicamente e consultar o saldo atualizado.

**Fluxos alternativos e exceções**
- Débito que deixa o saldo negativo é rejeitado sem persistência.
- Entrada inválida é rejeitada com mensagem abaixo do campo na interface e erro JSON na API.

**Erros previstos**
- Valor ausente, não positivo, não numérico ou com fração de centavo.
- Descrição ausente.
- Saldo insuficiente.

**Prioridade:** alta

#### FR-002 Cadastro de ativos do nível 1
Permitir CRUD de ativos com nome, preço de mercado e tipo. Todos são obrigatórios; tipos válidos são `RV`, `RF` e `FUNDO`; preço unitário aceita até oito casas decimais.

**Fluxo principal**
- Validar campos e tipo permitido.
- Criar, consultar, alterar ou remover o ativo.

**Fluxos alternativos e exceções**
- Ativo inexistente retorna não encontrado.
- Remoção ou alteração que viole dependência de movimentações retorna conflito conforme contrato.

**Erros previstos**
- Campo obrigatório ausente, tipo inválido ou preço fora da escala.

**Prioridade:** alta

#### FR-003 Movimentação de ativos do nível 1
Permitir compra, venda e consulta com ativo, quantidade de até duas casas decimais e valor da movimentação. Compra e venda geram lançamento correspondente na conta corrente; venda não pode tornar a quantidade total negativa.

**Fluxo principal**
- Validar ativo, quantidade e valor.
- Validar saldo para compra ou quantidade disponível para venda.
- Persistir movimentação e lançamento espelho na mesma operação lógica.

**Fluxos alternativos e exceções**
- Compra reduz a conta; venda aumenta a conta.
- Venda acima da quantidade disponível é rejeitada sem efeito parcial.

**Erros previstos**
- Ativo inexistente, quantidade não positiva ou acima de duas casas.
- Saldo insuficiente ou quantidade insuficiente.

**Prioridade:** alta

#### FR-004 Posição e cálculos do nível 1
Consultar um registro por ativo com nome, tipo, quantidade total, valor de mercado total e rendimento. Quantidade total é compras menos vendas. Valor de mercado total é quantidade total multiplicada pelo preço de mercado. Rendimento é preço de mercado dividido pelo preço médio das compras. Lucro do ativo é soma das vendas menos soma das compras.

**Fluxo principal**
- Agregar movimentações por ativo.
- Calcular quantidade, valor de mercado, preço médio, rendimento e lucro com regras monetárias do PRD.
- Retornar a lista de posições.

**Fluxos alternativos e exceções**
- Ativo sem compras não cria divisão inválida; comportamento de exibição é ❓ LACUNA.

**Erros previstos**
- Ativo inexistente em consulta específica e dados incompatíveis.

**Prioridade:** alta

#### FR-005 Datas e valores de mercado do nível 2
Adicionar emissão e vencimento, exigindo emissão anterior ao vencimento, e remover o preço de mercado direto do ativo. Permitir definir e excluir preço de mercado em uma data.

**Fluxo principal**
- Validar relação das datas.
- Persistir o preço histórico por ativo e data.
- Consultar usando o preço mais recente cuja data seja menor ou igual à consulta.

**Fluxos alternativos e exceções**
- Ausência de preço elegível é reportada como erro de domínio ou lacuna contratual conforme decisão futura.

**Erros previstos**
- Emissão igual ou posterior ao vencimento; data inválida; preço com escala acima de oito casas.

**Prioridade:** alta

#### FR-006 Temporalidade de lançamentos e movimentações
Lançamentos e movimentações recebem data do movimento e só afetam saldo ou posição a partir dela, inclusive. Movimentações só ocorrem entre emissão inclusive e vencimento exclusive, em segunda a sexta-feira. Consultas de lançamentos e movimentações exigem data início e data fim, ambas inclusive; saldo e posição exigem data.

**Fluxo principal**
- Validar data, janela do filtro e dia útil.
- Incluir no cálculo apenas fatos com data menor ou igual à consulta.
- Validar não negatividade no histórico inteiro, não apenas na data do lançamento.

**Fluxos alternativos e exceções**
- Movimento em fim de semana, antes da emissão ou na data de vencimento é rejeitado.
- Lançamento futuro não altera saldo anterior.

**Erros previstos**
- Filtro ausente, intervalo invertido, data não parseável e violação da janela.
- Saldo ou posição negativos em qualquer data posterior afetada.

**Prioridade:** alta

#### FR-007 Precisão e arredondamento
Representar dinheiro sem fração de centavo. Valores monetários de entrada devem ter no máximo duas casas; preços unitários, até oito; quantidades, até duas. Todo arredondamento necessário em cálculos financeiros ocorre sempre para baixo, de modo determinístico.

**Fluxo principal**
- Validar escala antes de persistir.
- Calcular em unidade inteira de centavos para dinheiro e aplicar floor quando uma divisão exigir redução.

**Fluxos alternativos e exceções**
- Valor com mais casas do que a escala permitida é rejeitado ou, quando o cálculo gerar fração intermediária, reduzido para baixo segundo a regra.

**Erros previstos**
- Overflow, sinal inválido, escala excedente e número não finito.

**Prioridade:** alta

#### FR-008 Endpoints obrigatórios do nível 3
Disponibilizar exatamente as rotas e payloads abaixo, em JSON. Os POST não precisam devolver conteúdo, somente status adequado.

**Fluxo principal**
- Aceitar os corpos exatos, autenticar quando a opção A estiver ativa e responder o status documentado.

**Fluxos alternativos e exceções**
- Corpo ou rota fora do contrato retorna erro de cliente.

**Erros previstos**
- Não autenticado, proibido, inválido, conflito de saldo/posição e erro interno sem revelar dados.

**Prioridade:** alta

#### FR-009 Opção A multiusuário
Todas as requisições usam HTTP Basic. Pré-cadastrar `usuario0` até `usuario9`, com senhas `senha0` até `senha9`, e `root` com senha `spiderman`. Ativos são compartilhados; somente administrativo pode criar, alterar e remover ativos. Usuários comuns veem apenas seus próprios dados; root não gera lançamentos/movimentos nem consulta dados confidenciais.

**Fluxo principal**
- Autenticar e autorizar pelo papel antes do caso de uso.
- Aplicar o filtro de proprietário em todo dado privado.

**Fluxos alternativos e exceções**
- Falha de credencial retorna não autorizado; capacidade proibida retorna proibido.

**Erros previstos**
- Ausência de Basic, credencial inválida e operação incompatível com o papel.

**Prioridade:** alta, se a opção A for escolhida

#### FR-010 Opção B de posição assíncrona
`GET /posicao?data=2020-02-28` retorna um id de execução, por exemplo `{"id":42}`. `GET /posicao/42` retorna imediatamente HTTP 425 enquanto não concluído e HTTP 200 com o conteúdo quando concluído; o conteúdo é descartado após a execução. O processamento deve usar memória constante, independente da quantidade de movimentações, e processamento paralelo.

**Fluxo principal**
- Criar execução para a data válida e retornar id.
- Processar em paralelo lendo de forma incremental.
- Consultar o id até concluir e descartar o resultado após entrega.

**Fluxos alternativos e exceções**
- Id inexistente ou expirado retorna não encontrado.
- Falha de execução não é confundida com 425.

**Erros previstos**
- Data ausente/inválida, execução inexistente e falha de processamento.

**Prioridade:** alta, se a opção B for escolhida

---

### Requisitos não funcionais

Performance
- Nível 3 deve demonstrar bom desempenho sob uso concorrente e thread-safety, sem condição de corrida que viole saldo, posição ou isolamento. Meta numérica de latência e volume concorrente: TBD, não inventada pelo PRD.
- Opção B deve processar centenas de milhares de movimentações com memória constante, não proporcional ao total armazenado, e usar processamento paralelo.

Disponibilidade
- Executável standalone em todos os níveis. Meta de disponibilidade externa: TBD, condicionada à implantação gratuita.

Segurança e autorização
- Na opção A, HTTP Basic em todas as requisições, autorização por papel e isolamento por usuário; senhas de seed não são segredo de produção e não devem ser reutilizadas fora do ambiente previsto.

Observabilidade
- Registrar logs estruturados sem valores sensíveis desnecessários, métricas de latência, erro, rejeições por regra, filas/execuções 425 e concorrência; tracing é requerido para fluxos assíncronos quando suportado.

Confiabilidade e integridade de dados
- Operações que geram fatos correlatos são atômicas; saldo e posição jamais ficam negativos em qualquer data aceita; consultas repetidas são consistentes.

Compatibilidade e portabilidade
- Java 8 ou superior, compilação, testes e execução via Maven ou Gradle, banco embarcado, REST e JSON. A stack aprovada no [ADR-001](../adrs/adr-001-stack-da-aplicacao.md) usa Java 25, Spring Boot 4.1.1, Maven, SQLite e React com TypeScript.

Compliance
- ❓ LACUNA: política legal de retenção, exportação e eliminação de dados pessoais ainda não foi definida.

Acessibilidade no frontend consumidor
- Cumprir integralmente o contrato em [design/acessibilidade.md](../design/acessibilidade.md), [responsividade.md](../design/responsividade.md) e [nao-desenhado.md](../design/nao-desenhado.md).

---

### Arquitetura e abordagem

Abordagem
- Capacidades requeridas: API REST JSON, persistência embarcada, domínio financeiro isolado e interface React responsiva. A organização das camadas e escolhas técnicas estão no [HLD-001](../hlds/hld-001-arquitetura.md); este PRD não as decide.

Componentes
- Interface consumidora com cursor global de data.
- API para conta, ativos, movimentação e posição.
- Persistência como fonte de verdade dos fatos financeiros.

Integrações
- Cliente frontend consome a API REST.
- Deploy standalone e, no nível 3, artefatos Docker requeridos.

### Decisões e trade-offs

#### Decisão: direção visual A
- **Justificativa:** a [ADR-002](../adrs/adr-002-direcao-visual.md) registra que a data é o eixo e reduz a repetição do filtro.
- **Trade-off:** descoberta inicial menor e cursor exige URL; os detalhes estão na ADR.

#### Decisão: stack registrada
- **Justificativa:** a [ADR-001](../adrs/adr-001-stack-da-aplicacao.md) registra o contexto tecnológico já aprovado.
- **Trade-off:** consultar o HLD para fronteiras e limitações; este PRD não amplia a decisão.

---

### Dependências

#### técnica: HLD e FDD
O HLD precisa definir fronteiras e contratos antes da implementação; os FDDs precisam detalhar cada nível e ser revisados pelo Yoda.

#### técnica: design system
As telas só podem implementar estados desenhados; lacunas em [não desenhado](../design/nao-desenhado.md) bloqueiam o fluxo que as tocar.

#### organizacional: revisão independente
Patrick Jane audita critérios e testes observáveis; o implementador não aprova a própria entrega.

---

### Riscos e mitigação

#### Arredondamento inconsistente altera saldos
- **Probabilidade:** alta
- **Impacto:** corrupção de valores e reprovação dos invariantes.
- **Mitigação:** centavos inteiros, validação de escala, casos de fronteira e testes de propriedade.
- **Plano de contingência:** bloquear novos lançamentos, identificar dados afetados e executar correção versionada após decisão.

#### Consulta histórica permite negatividade posterior
- **Probabilidade:** alta
- **Impacto:** saldo ou posição inválidos em datas não consultadas.
- **Mitigação:** validar a série temporal completa antes de aceitar o fato; teste com lançamentos fora de ordem.
- **Plano de contingência:** rejeitar a operação e preservar a transação anterior.

#### Concorrência corrompe integridade no nível 3
- **Probabilidade:** média
- **Impacto:** dupla venda, saldo incorreto ou vazamento entre usuários.
- **Mitigação:** testes concorrentes, transação e isolamento definidos no HLD, observabilidade de conflitos.
- **Plano de contingência:** reduzir capacidade, serializar a operação crítica e investigar evidências.

#### Estado assíncrono consome memória proporcional
- **Probabilidade:** média
- **Impacto:** indisponibilidade com centenas de milhares de movimentos.
- **Mitigação:** leitura incremental, métricas de memória e teste de carga controlado.
- **Plano de contingência:** rejeitar novas execuções, drenar fila e manter resultado descartável.

---

### Critérios de aceitação

- Todos os requisitos FR-001 a FR-007 têm testes unitários de regras e integração de persistência.
- Dinheiro com mais de duas casas é rejeitado; preço aceita no máximo oito casas; quantidade aceita no máximo duas casas; todo arredondamento intermediário é para baixo.
- Um débito ou venda rejeitado não cria efeito parcial e não deixa saldo ou posição negativos em nenhuma data, inclusive quando fatos são inseridos fora de ordem.
- Movimentação na emissão é aceita, no vencimento é rejeitada, fim de semana é rejeitado e dias úteis dentro da janela são aceitos.
- Consulta temporal exige os filtros obrigatórios e usa fatos até a data inclusive; preço de mercado escolhido é o mais recente com data menor ou igual à consulta.
- Rendimento, quantidade, valor de mercado total e lucro correspondem às fórmulas FR-004, com casos de compra e venda verificáveis.
- Os cinco endpoints exatos do FR-008 aceitam os payloads definidos no enunciado e retornam status e corpo conforme o FDD-003.
- Opção A prova Basic em todas as requisições, seed de usuários, papel root e isolamento por usuário; root não transaciona nem acessa dados privados.
- Opção B prova id, 425 imediato, 200 concluído, descarte do resultado, centenas de milhares de movimentações, memória constante e processamento paralelo.
- Integração REST e persistência executa repetidamente e em qualquer ordem, com evidência publicada pelo pipeline; nenhum arquivo de aplicação desta cadeia documental é necessário para este PRD.
- Interface implementada só usa estados previamente desenhados e passa o piso de acessibilidade e responsividade.

---

### Testes e validação

Tipos de teste obrigatórios
- Testes unitários de parsing, escala, centavos, floor, fórmulas, datas úteis, janela e invariantes.
- Testes de integração para cada endpoint, incluindo persistência, códigos HTTP e payloads, com fixture isolada e reexecução segura por limpeza, rollback ou reset entre casos. Não exigir idempotência semântica dos POSTs.
- Testes de contrato para as rotas exatas do nível 3.
- Testes de segurança da opção A para autenticação, autorização e isolamento.
- Testes concorrentes para thread-safety e testes de carga/memória para a opção B.
- Testes de acessibilidade e responsividade por faixa para cada tela desenhada.

Estratégia de validação
- Derivar casos dos critérios acima, com dados próprios por teste, transações limpas e evidências de request/response publicadas pelo pipeline. Validar série temporal com inserção fora de ordem e comparar com uma referência independente. Medir memória e paralelismo em cenário controlado; se a meta numérica não estiver definida, registrar TBD em vez de declarar sucesso quantitativo.
