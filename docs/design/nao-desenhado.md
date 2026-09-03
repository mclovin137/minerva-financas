# Não desenhado

Cada item abaixo é ❓ LACUNA. A lacuna bloqueia a implementação que tocar o item; este documento não
inventa solução.

As telas, os estados e os formatos foram fechados em [telas](telas.md) em 2026-09-02. O que segue é
o que **continua** sem desenho.

- ❓ LACUNA: sparkline de evolução de patrimônio, citada como opcional nos [princípios de
  layout](principios-de-layout.md). Fora do escopo desta entrega; não improvisar.
- ❓ LACUNA: exportação de dados em CSV ou PDF. Não há requisito no PRD nem no enunciado.
- ❓ LACUNA: gesto exato de swipe do bottom sheet e dimensões intermediárias de drawer entre as faixas
  declaradas em [responsividade](responsividade.md).

## Encerradas por decisão explícita, não por desenho

Estas duas deixaram de ser lacuna porque foram **decididas**, e a decisão está registrada em
[telas](telas.md):

- **Tema escuro:** não será entregue, e os tokens não são invertidos automaticamente. Inversão
  automática quebraria as relações de contraste verificadas em [acessibilidade](acessibilidade.md).
- **Comportamento offline:** a aplicação não opera offline e não enfileira escritas. Dado financeiro
  datado em cópia local desatualizada mostraria saldo errado com aparência de saldo certo.

## Encerradas por desenho

Fechadas em [telas](telas.md): formato de data e moeda; cursor global de data; os quatro destinos do
trilho e seus estados de padrão, hover, foco, ativo e indisponível; estados de carregando, vazio,
erro e conteúdo de cada listagem; conteúdo, ações, validação, fechamento e estados dos drawers de
criação e edição; confirmações destrutivas em modal; tela de entrada e sessão expirada; telas do
papel administrativo; UI de progresso da posição assíncrona, incluindo o tratamento de `425` como
progresso; carregamento e fallback das fontes; anel de foco e regras de quebra de texto.

Não há sincronização declarada com o Obsidian nesta task; a obrigação está registrada em
[`docs/pendencias-obsidian.md`](../pendencias-obsidian.md).
