---
type: minerva-adr
project: Minerva
date: 2026-09-02
status: aceita
tags:
  - minerva
  - adr
ai-first: true
---

# ADR 002: Direção visual Template A, Linha do Tempo

Revisor: Patrick Jane

## Para o futuro agente

Esta ADR decide a direção visual da aplicação consumidora Minerva Finanças. Ela importa sempre que uma tela, componente ou interação for desenhada; o contrato verificável está em [`docs/design/README.md`](../design/README.md) e nos documentos ligados a ele.

## Contexto

O nível 2 torna tudo datado: saldo e posição são consultados por data, o período é filtro obrigatório e o valor de mercado usado é o mais recente com data igual ou anterior à consulta. A data é, portanto, o eixo do domínio. As quatro ações mais frequentes são crédito, débito, compra e venda, com formulários de três ou quatro campos.

Também existem dois papéis com capacidades disjuntas: o administrativo gerencia ativos e não transaciona; o usuário comum transaciona e vê apenas os próprios dados. As validações são ricas, incluindo saldo negativo em alguma data, janela de emissão e vencimento, dias úteis e quantidade insuficiente. A posição pode ser leitura pesada e assíncrona na opção B, respondendo HTTP 425 enquanto processa.

## Escopo
- Afetado: header, cursor global de data, navegação, listagens, formulários de crédito, débito, compra e venda, posição e estados de validação/progresso.
- Fora de escopo: implementação frontend, tema escuro, telas administrativas detalhadas, login e estados não desenhados; estes permanecem em [`docs/design/nao-desenhado.md`](../design/nao-desenhado.md) como ❓ LACUNA.

## Forças e critérios
- Trocar a data de análise deve custar uma interação, pois é a operação repetida com maior frequência.
- As quatro ações principais devem permanecer acessíveis com formulários curtos.
- A separação visual entre papéis não pode sugerir capacidades que o domínio proíbe.
- Estados de validação e o HTTP 425 devem ser comunicados sem depender apenas de cor.
- A superfície deve permanecer pequena, responsiva e auditável.

## Opções consideradas

### Opção 1: A Linha do Tempo
- Status na análise: escolhida
- Benefícios: data como cursor global no header, trilho de ícones, drawer para criação e menor superfície, aproximadamente 18 componentes.
- Custos e limitações: exige tooltip e expansão do trilho para descoberta; o cursor global precisa de estado compartilhável.
- Riscos: uma data apenas em memória quebra links compartilhados e o voltar do navegador.
- Trade-off: ganha uma interação para trocar a data e simplicidade; aceita descoberta inicial mais fraca e a complexidade de persistir o cursor na URL.
- Evidências: escolha explícita do usuário em 2026-09-02 e requisitos temporais do enunciado MAPS.

### Opção 2: B Navegação Primeiro
- Status na análise: descartada
- Benefícios: sidebar completa rotulada, páginas limpas, modal e maior descoberta; implementação mais barata.
- Custos e limitações: data tratada como filtro por página, três interações para trocar a data e cinco ou seis para lançar.
- Riscos: repetição excessiva pode induzir lançamentos na data errada.
- Trade-off: ganha descobribilidade e menor custo inicial; perde eficiência na operação temporal repetida.
- Evidências: comparação de interação fornecida no briefing.

### Opção 3: C Mesa de Operações
- Status na análise: descartada
- Benefícios: paleta de comandos ⌘K, split view, edição inline e eficiência objetiva, duas interações e zero cliques em fluxos treinados.
- Custos e limitações: exige aprender sintaxe; no celular a vantagem desaparece e seriam necessárias duas interfaces.
- Riscos: erro de comando e custo de manutenção responsiva.
- Trade-off: ganha velocidade para especialistas; perde acessibilidade de descoberta e consistência entre dispositivos.
- Evidências: comparação de interação e restrição móvel fornecidas no briefing.

### Opção 4: D SaaS Moderno
- Status na análise: descartada
- Benefícios: sidebar, header contextual, breadcrumb e drawers formam padrão seguro e escalável.
- Custos e limitações: aproximadamente 28 componentes e breadcrumb decorativo em uma hierarquia de dois níveis.
- Riscos: cerimônia visual pode esconder o eixo temporal e aumentar manutenção.
- Trade-off: ganha teto de escala e familiaridade; aceita complexidade acima da necessidade atual.
- Evidências: comparação de superfície fornecida no briefing.

## Decisão

Adotar a **Opção A, Linha do Tempo**, escolhida explicitamente pelo usuário em 2026-09-02. A data de análise será um cursor global no header, persistido na URL, com trilho lateral de ícones e drawers para criação e edição.

Como empréstimos pontuais do Template D, adotar chips de filtro ativo nas listagens, rascunho automático com aviso ao fechar drawer com alteração não salva e sparkline nos indicadores somente se sair barato. Isso não transforma a base A em D.

Decisão futura possível, fora do escopo atual: adicionar a paleta de comandos do C como atalho opcional sobre a base A, sem obrigar ninguém a escolhê-la.

## Consequências

### Positivas
- A data corresponde ao eixo mental e visual do domínio.
- Trocas de data e ações frequentes ficam curtas.
- Drawers preservam contexto da listagem e limitam a superfície.
- A base permanece adequada aos quatro ou cinco destinos atuais.

### Negativas
- A descoberta é mais fraca que em B; tooltip e expansão no hover são necessários.
- O teto de escala é aproximadamente seis ou sete áreas de navegação; se o produto dobrar, a direção deverá migrar para D.
- O cursor global precisa viver na URL. Sem isso, links compartilhados quebram e o botão voltar perde semântica. Este é o principal custo técnico introduzido.
- Sparkline e rascunho podem elevar custo de implementação; devem ser omitidos se não forem baratos.

### Riscos residuais
- Usuários podem não descobrir ícones: mitigar com tooltip, expansão e estados de foco; revisar com teste de usabilidade quando desenhado.
- URL pode divergir da memória: tratar URL como fonte de estado navegável e cobrir voltar, avançar e compartilhamento nos testes.

## Impacto nos artefatos
- [`docs/design/README.md`](../design/README.md) e documentos de design: contrato visual e responsivo.
- [`docs/prds/prd-001-financas-pessoais.md`](../prds/prd-001-financas-pessoais.md): requisito de data como eixo e critérios de interface.
- [`docs/hlds/hld-001-arquitetura.md`](../hlds/hld-001-arquitetura.md): fronteira do frontend e estado temporal na URL.
- [`docs/fdds/fdd-001-nivel-1.md`](../fdds/fdd-001-nivel-1.md), [`fdd-002-nivel-2-datas.md`](../fdds/fdd-002-nivel-2-datas.md) e [`fdd-003-nivel-3-e-opcoes.md`](../fdds/fdd-003-nivel-3-e-opcoes.md): fluxos de operação e estados.

## Relações
- Origem: escolha explícita do usuário em 2026-09-02 e enunciado MAPS.
- ADRs relacionadas: [`ADR-001 Stack da aplicação`](adr-001-stack-da-aplicacao.md).
- Substitui: nenhuma.
- Substituída por: nenhuma.

## Critério para revisitar

Revisitar se o número de áreas de navegação superar sete, se testes mostrarem que usuários não encontram ações mesmo com tooltip e foco, ou se a paleta de comandos opcional se tornar requisito obrigatório.

## Aceite
- Responsável arquitetural: Yoda
- Decisor: usuário
- Evidência do aceite: escolha explícita do Template A registrada no briefing de 2026-09-02.

## Histórico
- 2026-09-02: ADR criada com status aceita.
