# Princípios de layout

Este documento é contrato de composição e interação.

- Header fixo: logo | cursor de data com campo, setas `◀` e `▶` e atalho “hoje” | usuário e papel.
- Trilho lateral de `64px`, somente ícones com tooltip, expandindo a `220px` no hover; quatro destinos.
- Área principal larga, sem cards aninhados. Card existe somente para os três indicadores.
- Toda criação e edição abre drawer lateral direito. Modal é reservado a confirmação destrutiva.
- Não há breadcrumb: a hierarquia tem um nível e breadcrumb seria cerimônia vazia.
- Listagens não têm coluna de botões. O menu contextual `⋯` concentra ações por registro; clique na linha abre detalhe em drawer; barra de lote só aparece quando há seleção.
- Erro de validação fica sempre abaixo do campo, com mensagem do domínio e caminho de correção. Exemplo canônico: “Este débito deixaria o saldo negativo em 2020-03-04.”
- HTTP 425 da posição assíncrona é progresso, nunca erro: mostrar skeleton/progresso honesto.
- Chips de filtro ativo e aviso de rascunho não salvo são empréstimos do Template D; sparkline só entra se sair barato.

❓ LACUNA: estados de foco detalhados de cada componente e desenho dos quatro destinos ainda não foram especificados; implementação que os tocar deve parar.

