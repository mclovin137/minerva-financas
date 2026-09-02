# Responsividade

Toda faixa abaixo tem comportamento obrigatório, não apenas uma versão encolhida.

| Faixa | Comportamento |
|---|---|
| `>=1280px` | layout completo; trilho pode expandir no hover; área principal larga |
| `1024-1279px` | layout completo com trilho fixo em 64px; não expande no hover |
| `768-1023px` | trilho vira barra inferior; indicadores em duas colunas |
| `<768px` | barra inferior com quatro itens; cursor de data em barra fixa própria; drawer vira bottom sheet com swipe e confirmação de descarte; posição vira lista de cartões com nome+tipo, quantidade+valor e rendimento como chip |

❓ LACUNA: dimensões intermediárias de drawer, regras de quebra de texto e gesto exato de swipe não foram desenhadas. Não improvisar esses detalhes ao implementá-los.

