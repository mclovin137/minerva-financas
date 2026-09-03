# Acessibilidade

Este é o piso verificável do contrato visual:

- contraste mínimo de `4.5:1` para texto normal e `3:1` para texto grande;
- foco visível em todo elemento interativo;
- alvo de toque mínimo de `44px`;
- nenhum estado depende só de cor. Crédito e débito usam também sinal ou ícone;
- cabeçalho ordenável usa `aria-sort`;
- erro usa `role="alert"`;
- respeitar `prefers-reduced-motion`;
- ordem de foco igual à ordem visual;
- todo campo tem label visível, nunca placeholder como label.

Cada critério deve ser verificado em teste automatizado quando possível e em inspeção manual quando não for. Detalhes não presentes em [não desenhado](nao-desenhado.md) permanecem ❓ LACUNA.

