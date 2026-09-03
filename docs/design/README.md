# Design system Minerva

## Para o futuro agente

Este diretório é o contrato auditável da interface decidido na [ADR-002](../adrs/adr-002-direcao-visual.md). A direção visual é a Linha do Tempo: a data é cursor global, as ações abrem em drawer e a interface deve obedecer tokens, tipografia, layout, acessibilidade e faixas responsivas abaixo.

## Índice

- [Tese](tese.md): princípio organizador.
- [Tokens](tokens.md): valores normativos de cor, espaço, raio e elevação.
- [Tipografia](tipografia.md): famílias, tamanhos e números tabulares.
- [Princípios de layout](principios-de-layout.md): composição e interação.
- [Acessibilidade](acessibilidade.md): piso verificável.
- [Responsividade](responsividade.md): comportamento por faixa.
- [Não desenhado](nao-desenhado.md): lacunas que bloqueiam implementação ao serem tocadas.

## Contrato versus sugestão

São contrato: tokens HEX e escalas definidos em [tokens](tokens.md), tipografia e `tabular-nums`, estrutura de header/trilho/drawer, regras de erro e progresso, requisitos de acessibilidade e comportamento de cada faixa responsiva. Divergência precisa de decisão registrada e revisão.

São sugestões condicionais: sparkline, rascunho automático, microinterações e detalhes de ícone não fixados nos documentos. Só podem ser adotados se não contradisserem o contrato. Item ausente em [não desenhado](nao-desenhado.md) é ❓ LACUNA e não pode ser improvisado.

