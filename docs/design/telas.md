# Telas e estados

Versão: 1.0 — 2026-09-02
Responsável: Severino — revisor: Yoda

Este documento fecha as lacunas de tela listadas em [não desenhado](nao-desenhado.md), dentro da
direção aceita na [ADR-002](../adrs/adr-002-direcao-visual.md) (Template A, Linha do Tempo). É
contrato: a implementação segue o que está aqui e não improvisa o que continuar marcado como
`❓ LACUNA`.

Insumos: [tese](tese.md), [princípios de layout](principios-de-layout.md), [tokens](tokens.md),
[tipografia](tipografia.md), [responsividade](responsividade.md) e [acessibilidade](acessibilidade.md).

---

## 1. Formato de data e moeda

| Grandeza | Exibição | Entrada |
|---|---|---|
| Data | `dd/MM/aaaa` (`28/02/2020`) | campo `<input type="date">`, que já é `aaaa-mm-dd` no protocolo |
| Data na URL | `aaaa-mm-dd` | o cursor global é compartilhável, então usa o formato do domínio |
| Moeda | `R$ 1.234,56`, sempre duas casas | `<input inputmode="decimal">`, ponto ou vírgula aceitos |
| Quantidade | `2,50`, sempre duas casas | idem |
| Preço unitário | até `8` casas, zeros à direita suprimidos a partir da terceira (`105,53`, `1,23456789`) | idem |
| Rendimento | `1,0553` com quatro casas, mais o percentual entre parênteses: `1,0553 (+5,53%)` | — |

Regras que valem em toda a interface:

- Todo número monetário usa `font-variant-numeric: tabular-nums`. Sem isso os dígitos mudam de
  largura a cada atualização e a comparação visual entre linhas fica inválida.
- Valor negativo nunca é indicado **apenas** por cor: leva sinal `−` explícito e, em lançamentos,
  o ícone de seta (`↑` entrada, `↓` saída).
- Zero é `R$ 0,00`, nunca vazio nem traço: vazio significaria "não sei", e nós sabemos.
- Campo indisponível (por exemplo, preço de mercado ausente na data) exibe `—` com `title`
  explicando: *"Sem preço de mercado cadastrado até esta data."*

## 2. Cursor global de data

Vive no header, é o único lugar onde a data é escolhida e é **persistido na URL** (`?data=aaaa-mm-dd`).
Sem isso, um link compartilhado e o botão voltar do navegador mostrariam outra data — o risco
registrado na ADR-002.

- Campo de data + botões `◀` e `▶` (um dia) + atalho **Hoje**.
- `◀`/`▶` têm `aria-label` explícito (*"Dia anterior"*, *"Próximo dia"*), não só o ícone.
- Data inválida não é aceita pelo campo; data futura é permitida e mostra estado vazio normal.
- As listagens que exigem intervalo derivam o período do cursor: `dataInicio` = primeiro dia do mês
  do cursor, `dataFim` = o cursor. Um seletor de período no próprio painel permite ampliar.

## 3. Os quatro destinos do trilho

| Ícone | Destino | Rota | Conteúdo |
|---|---|---|---|
| `⇅` | Conta corrente | `/conta` | saldo do dia, lançamentos do período, ações de crédito e débito |
| `◈` | Carteira | `/carteira` | posição na data, com os indicadores e a tabela por ativo |
| `▤` | Ativos | `/ativos` | acervo compartilhado; leitura para todos, edição só para o administrador |
| `⌇` | Mercado | `/mercado` | preços por data de um ativo; escrita só para o administrador |

Estados de cada item do trilho:

| Estado | Tratamento |
|---|---|
| Padrão | ícone em `color-text-secondary`, fundo transparente |
| Hover | fundo `color-primary-light`, ícone em `color-primary`, tooltip após 400 ms |
| Foco | anel de `2px` em `color-primary` com `2px` de deslocamento, visível sobre qualquer fundo |
| Ativo | fundo `color-primary-light`, ícone `color-primary`, **barra de 3px** à esquerda e `aria-current="page"` |
| Indisponível ao papel | não é escondido: fica com opacidade `.45`, `aria-disabled` e tooltip dizendo por quê |

Um destino indisponível continua visível de propósito: esconder rotas faria o `root` e o usuário
comum verem aplicações diferentes, e a diferença viraria suporte.

## 4. Estados de tela

Toda listagem e todo painel tem quatro estados, e nenhum deles é improvisado na implementação.

### 4.1 Carregando (skeleton)

Blocos em `color-border` a 40% de opacidade, com o **formato do conteúdo real** — três linhas de
indicador, seis linhas de tabela na altura de linha final. Nunca um spinner centralizado: o skeleton
com a forma certa evita o salto de layout quando o conteúdo chega. Respeita
`prefers-reduced-motion`, trocando o brilho pulsante por opacidade estática.

### 4.2 Vazio

Ícone do destino em `48px`, título curto, uma linha de explicação e a ação primária correspondente:

| Tela | Título | Ação |
|---|---|---|
| Conta corrente | *"Nenhum lançamento em 28/02/2020"* | **Novo crédito** |
| Carteira | *"Nenhuma posição nesta data"* | **Nova compra** |
| Ativos | *"Nenhum ativo cadastrado"* | **Novo ativo** (só administrador) |
| Mercado | *"Escolha um ativo para ver os preços"* | seletor de ativo |

Vazio por filtro é diferente de vazio por ausência: quando há filtro aplicado, o texto diz
*"Nenhum resultado para o período selecionado"* e oferece **Limpar período** em vez da ação de criar.

### 4.3 Erro

Painel com borda `color-error`, ícone `⚠` e título — cor nunca é o único sinal. O corpo mostra a
mensagem que a API devolveu no campo `mensagem`, que já vem em pt-BR e sem detalhe interno. Botão
**Tentar de novo** refaz a última requisição.

| Situação | Tratamento |
|---|---|
| Rede indisponível (falha de `fetch`) | *"Sem conexão com o servidor."* + **Tentar de novo** |
| `401` | sessão expirada: ver §7 |
| `403` | *"Seu perfil não permite esta ação."* — permanece na tela, sem redirecionar |
| `409` | erro de domínio: fica **abaixo do campo** no drawer, não em painel de tela |
| `429` | *"Há consultas demais em andamento. Aguarde a conclusão das anteriores."* |
| `5xx` | *"O servidor não conseguiu concluir. Tente de novo em instantes."* |

### 4.4 Conteúdo

Layout descrito nos [princípios de layout](principios-de-layout.md): área larga, sem cards aninhados,
cards apenas nos três indicadores.

## 5. Drawers de criação e edição

Todo formulário abre em drawer lateral direito (bottom sheet abaixo de `768px`).

- Largura: `min(480px, 100vw - 32px)`; abaixo de `768px` vira bottom sheet com altura `min(80vh, conteúdo)`.
- Cabeçalho com título e botão fechar (`Esc` também fecha); rodapé fixo com **Cancelar** e a ação
  primária.
- O foco vai para o **primeiro campo** ao abrir, fica preso dentro do drawer enquanto aberto, e
  retorna ao elemento que o abriu quando fecha. Sem isso o teclado se perde atrás do overlay.
- Fechar com alteração não salva pede confirmação: *"Descartar as alterações?"*. Sem alteração,
  fecha direto — perguntar sempre treina o usuário a ignorar a pergunta.
- Enquanto envia: a ação primária vira **Salvando…**, fica desabilitada, e os campos ficam
  somente-leitura. Nunca fechar antes da resposta.
- Sucesso: o drawer fecha, a listagem recarrega e aparece um aviso discreto por 4 s no canto inferior.
- Erro de campo (`400`): mensagem abaixo do campo, `aria-describedby` ligando campo e mensagem, foco
  movido para o primeiro campo inválido.
- Erro de domínio (`409`): mensagem no rodapé do drawer, acima dos botões, com o texto da API. O
  drawer **não fecha** e os valores digitados permanecem.

| Drawer | Campos | Ação primária |
|---|---|---|
| Crédito | valor, descrição (data vem do cursor, exibida e editável) | **Lançar crédito** |
| Débito | idênticos | **Lançar débito** |
| Compra | ativo (busca), quantidade, valor | **Comprar** |
| Venda | idênticos | **Vender** |
| Ativo | código, nome, tipo (`RV`/`RF`/`FUNDO`), emissão, vencimento | **Salvar ativo** |
| Preço de mercado | ativo, data, preço | **Salvar preço** |

Validação no cliente é **espelho** da do servidor, nunca substituta: escala, sinal e obrigatoriedade
são checados antes de enviar para dar retorno imediato, e a resposta do servidor continua sendo a
verdade.

## 6. Confirmação destrutiva

Só remoção de ativo e de preço de mercado. Modal centralizado, não drawer — a interrupção é o ponto.

- Título nomeia o alvo: *"Remover o ativo ATIVO1?"*.
- Corpo diz a consequência irreversível e o que impede: *"O ativo sai do acervo compartilhado. Ativos
  com movimentações não podem ser removidos."*
- Botões: **Cancelar** (foco inicial, para que `Enter` acidental não destrua nada) e **Remover** em
  `color-error`.
- `Esc` cancela. Durante o envio, **Remover** vira **Removendo…** e ambos ficam desabilitados.
- `409 ATIVO_EM_USO` mantém o modal aberto e mostra a mensagem da API no corpo.

## 7. Autenticação e sessão

A API exige HTTP Basic em toda requisição, então não há sessão de servidor: a interface guarda a
credencial em memória pelo tempo da aba.

**Tela de entrada** — coluna única centrada, `max-width: 380px`, sem trilho e sem header:

- Campos: usuário e senha, ambos com label visível.
- Ação **Entrar**; `Enter` submete.
- Credencial inválida: *"Usuário ou senha inválidos."* abaixo do formulário, com `role="alert"`. A
  mensagem é a mesma para usuário inexistente e senha errada — distinguir os dois diria a um atacante
  quais logins existem.
- A validação é feita chamando `GET /ativos`, que ambos os papéis podem acessar.

**Sessão expirada.** Qualquer `401` durante o uso limpa a credencial, leva à tela de entrada e mostra
*"Sua sessão expirou. Entre novamente."*. O caminho que o usuário estava tentando é preservado e
restaurado após o login.

**A credencial fica em memória, não em `localStorage`.** Basic é a senha em claro codificada em
base64: gravá-la em disco a deixaria legível para qualquer script na origem e para quem tiver acesso
ao perfil do navegador. O custo aceito é ter de entrar de novo ao recarregar a página, e ele está
registrado aqui em vez de descoberto depois.

## 8. Progresso da posição assíncrona

A carteira tem duas formas de carregar. Abaixo de um limiar, usa `GET /posicao`. Quando o usuário
pede **Recalcular em segundo plano**, usa a opção B:

1. `GET /posicao?data=` devolve `202` e o id. A tela mostra skeleton com a legenda
   *"Calculando posição…"* e uma barra indeterminada.
2. `GET /posicao/{id}` é repetido a cada `600 ms`. **`425` é progresso, nunca erro**: não muda
   a cor, não mostra ⚠ e não conta como falha.
3. `200` substitui o skeleton pela tabela e a legenda vira *"Atualizada às hh:mm:ss"*.
4. Após `60 s` sem conclusão, a tela oferece **Continuar aguardando** ou **Cancelar** — a espera fica
   sob controle do usuário em vez de ser infinita e silenciosa.
5. `404` na consulta significa resultado já consumido ou expirado: a tela diz *"O resultado expirou."*
   e oferece recalcular.
6. `429` na criação mostra a mensagem de excesso de execuções e **não** inicia polling.

## 9. Comportamento offline

A aplicação **não** funciona offline, e isso é decisão, não omissão: os dados são financeiros e
datados, e uma cópia local desatualizada mostraria saldo errado com aparência de saldo certo.

- Falha de rede exibe o estado de erro de §4.3, com **Tentar de novo**.
- Nenhuma escrita é enfileirada para envio posterior. Um lançamento enfileirado seria aplicado em uma
  data e um estado de saldo diferentes dos que o usuário viu ao criá-lo.
- O evento `offline` do navegador mostra uma faixa persistente no topo: *"Sem conexão. As ações estão
  indisponíveis."*, e as ações primárias ficam desabilitadas enquanto durar.

## 10. Tema e fontes

**Tema claro apenas.** O tema escuro **não** será entregue, e os tokens **não** são invertidos
automaticamente: inversão automática quebra as relações de contraste verificadas em
[acessibilidade](acessibilidade.md) e produziria um tema que ninguém desenhou. Isto encerra a lacuna
por decisão explícita — o tema escuro exige desenho próprio, e não está no escopo.

**Fontes.** `Sora` para display e `Public Sans` para corpo, servidas pelo Google Fonts com
`display=swap`. A pilha de fallback é declarada e o layout é medido com ela: `Sora, "Segoe UI",
system-ui, sans-serif` e `"Public Sans", system-ui, -apple-system, "Segoe UI", sans-serif`. Se as
fontes não carregarem, a aplicação continua legível e as colunas numéricas mantêm `tabular-nums`, que
a pilha do sistema também oferece.

## 11. Foco e quebra de texto

- Anel de foco único em toda a aplicação: `outline: 2px solid color-primary; outline-offset: 2px`.
  Nunca `outline: none` sem substituto visível.
- A ordem de foco segue a ordem visual; o drawer prende o foco enquanto aberto.
- Um link **Pular para o conteúdo** é o primeiro elemento focável da página.
- Código de ativo e valores numéricos **não** quebram (`white-space: nowrap`); nome de ativo e
  descrição quebram por palavra e truncam com reticências e `title` após duas linhas.
- Tabelas largas rolam horizontalmente dentro do próprio contêiner; a página nunca rola na horizontal.

## 12. O que continua sem desenho

- ❓ LACUNA: sparkline de evolução de patrimônio — mencionada nos princípios de layout como opcional,
  não entra nesta entrega e não deve ser improvisada.
- ❓ LACUNA: exportação de dados (CSV, PDF) — não há requisito no PRD nem no enunciado.

## Histórico

- 2026-09-02: documento criado, fechando 13 das 15 lacunas de `nao-desenhado.md`; tema escuro e
  offline encerrados por decisão explícita de não escopo.
