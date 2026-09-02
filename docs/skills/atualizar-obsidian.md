# Skill — atualizar-obsidian

**Autor:** Cristóvão Augusto

**Definição canônica.** Adaptador de descoberta em `.claude/skills/atualizar-obsidian/SKILL.md` — ponteiro, sem conteúdo próprio.

## Para o futuro agente

Procedimento que mantém a base Obsidian do projeto em dia sempre que uma mudança toca um artefato-gatilho (regra de ferro 3). É **autossuficiente**: só precisa de leitura e escrita de arquivo. Não depende de MCP, plugin, script, `git`, PR, pipeline, índice, manual, nem de qualquer nota já existir. Tudo que ela precisa saber está escrito aqui.

Esta skill aplica a regra 3 e consulta `docs/continuidade.md`.

## Onde fica a base

```
Windows : C:\Users\mclov\OneDrive\Documentos\Obsidian Vault\mclov\Documents\SecondBrain\Bases\Minerva
WSL     : /mnt/c/Users/mclov/OneDrive/Documentos/Obsidian Vault/mclov/Documents/SecondBrain/Bases/Minerva
```

A base fica **fora do repositório**. Consequência que mais importa: o diff do PR não prova que você a atualizou. Ao tocar um gatilho, registre imediatamente em `docs/pendencias-obsidian.md` a origem, destino, responsável e prazo; a sincronização ocorre em até 24 horas, ou antes se o usuário pedir, e só é declarada após conferir o caminho escrito.

Se o caminho estiver inacessível (OneDrive fora do ar, permissão negada), **diga isso ao usuário e não afirme que documentou.** Mantenha a pendência rastreada no prazo; não invente caminho alternativo. Pendência vencida bloqueia conclusão e trabalho dependente.

## Quando usar

Sempre que o que você mudou aparecer na tabela abaixo. Também ao auditar: rode a mesma tabela contra a mudança alheia.

**Quando não usar:** mudança que não toca gatilho nenhum — formatação, typo em comentário, renomeação interna sem efeito em regra. A exceção enxuta da regra 9 só pode dispensar documentação formal nessa situação, após autorização explícita do usuário para a mudança, e mantém o registro obrigatório no PR. Declarar `nenhum gatilho tocado` custa poucos segundos; pular a declaração não é permitido.

## Tabela de gatilhos

`<BASE>` = o caminho acima.

| Você mudou… | Nota obrigatória |
|---|---|
| Migração ou DDL: `CREATE`/`ALTER`/`DROP` de tabela, índice, trigger ou função | `<BASE>/banco-de-dados/<tipo>-<nome>.md` — uma nota **por objeto** |
| Manifesto ou lockfile de dependências (build, pacote, ferramenta de CI) | `<BASE>/dependencias/dependencias.md` |
| Código que define ou altera regra de negócio: agregado, invariante, validação, cálculo, máquina de estados | `<BASE>/regras-de-negocio/rn-NNN-<slug>.md` |
| Decisão estrutural ou de tecnologia (stack, hospedagem, CI, banco, padrão de arquitetura) | `<BASE>/adrs/adr-NNN-<slug>.md` |
| Escopo, critério de aceite ou requisito | `<BASE>/prds/prd-NNN-<slug>.md` |
| Como o sistema se organiza: partes, fronteiras, contratos entre elas | `<BASE>/hlds/hld-<slug>.md` |
| Como uma feature funciona por dentro | `<BASE>/fdds/fdd-<slug>.md` |
| Direção do produto, fase ou prioridade macro | `<BASE>/roadmap/roadmap.md` |
| Responsabilidade de um role | `<BASE>/roles/<role>.md` |
| Definição de agente ou adaptador de ferramenta | `<BASE>/agentes/<agente>.md` |
| Qualquer skill, inclusive esta | `<BASE>/skills/<skill>.md` |
| Task criada, concluída, cancelada ou com escopo alterado | `<BASE>/tasks/t-NNN-<slug>.md` |
| Qualquer regra de ferro | `<BASE>/regras-de-ferro.md` |

Na dúvida entre dois destinos, escreva nos dois e cruze os links. Nota a mais é barata; gatilho não documentado reprova o PR.

## Procedimento

Quatro passos. Nenhum deles pressupõe que algo já exista.

**1. Liste os gatilhos.** Percorra o que você mudou e marque as linhas da tabela que se aplicam.

**2. Resolva o caminho.** Monte `<BASE>/<pasta>/<arquivo>.md`. Pasta inexistente: crie. Arquivo inexistente: crie. Nome em ASCII minúsculo com hífens, sem acento e sem espaço — o título legível vai no `# H1`, não no nome do arquivo. Antes de criar, procure por nome parecido na pasta: duplicata é pior que nota faltando.

**3. Escreva a nota.** Toda nota tem este cabeçalho:

```markdown
---
type: minerva-<tipo>
project: Minerva
date: AAAA-MM-DD
tags:
  - minerva
  - <tipo>
ai-first: true
---

# <Título legível>

## Para o futuro agente

<2 a 3 frases: o que é isto e quando importa.>
```

`<tipo>` é um de: `adr`, `prd`, `hld`, `fdd`, `task`, `roadmap`, `epico`, `regra-de-negocio`, `objeto-de-banco`, `dependencia`, `role`, `agente`, `skill`, `governanca`.

A nota é **autocontida**: quem lê só ela entende o assunto sem abrir o repositório. Nada de nota que só diz "ver o código". Se o artefato já tinha nota, edite o corpo e acrescente uma linha em `## Histórico` com a data e o que mudou — o valor de topo reflete o estado atual, o histórico preserva o resto. Artefato removido não vira arquivo apagado: a nota recebe `status: arquivado` e o motivo no histórico.

Nunca invente fato, número, data ou decisão que não foi estabelecido. Desconhecido é `TBD` e seção vazia é resposta correta.

**4. Declare o estado real.** Na descrição do PR quando houver PR, e sempre na resposta ao usuário, informe os caminhos conferidos ou a pendência registrada:

```markdown
## Base Obsidian (regra de ferro 3)
- Gatilhos tocados: <lista, ou "nenhum">
- Notas sincronizadas e conferidas: <caminhos completos, ou "nenhuma">
- Pendências: <origem, destino, responsável e prazo, ou "nenhuma">
```

## Verificação

1. **Você**, ao registrar a pendência no mesmo turno e ao sincronizar dentro do prazo. É a barreira principal, porque a base está fora do repositório e nenhuma automação a enxerga.
2. **A auditoria**, no papel de planejar/revisar: roda a tabela de gatilhos contra a mudança, confere as notas declaradas e verifica prazo, origem, destino e responsável das pendências. Pendência completa dentro do prazo é aceitável; vencida bloqueia conclusão ou trabalho dependente.

Não há terceira camada. Um pipeline não consegue verificar um diretório que não está no repositório — é exatamente por isso que os dois primeiros não se negociam.

## Racionalizações que não valem

| Desculpa | Realidade |
|---|---|
| "É só refactor, não mudou regra" | Se não mudou gatilho, você declara `nenhum` em dez segundos. Você só está lendo esta linha porque teve dúvida — e dúvida significa que mudou. |
| "Documento depois" | Só é aceitável como pendência registrada, com destino, responsável e prazo máximo de 24 horas; sem isso, a falta fica invisível. |
| "O código já é a documentação" | O código não diz por que a regra existe, quem decidiu, nem o que foi descartado. |
| "É índice, não é tabela" | Índice, trigger e função estão nomeados um a um na regra 3. |
| "A dependência é só de teste" | Toda dependência carrega custo, e custo zero é regra de ferro 5. |
| "É hotfix, é urgente" | Hotfix exige PR e revisão independente. Só dispensa nota se houver autorização explícita do usuário, cumprir integralmente a exceção enxuta da regra 9 e não tocar gatilho; se houver dúvida, a nota é obrigatória. |
| "O usuário pediu só o código" | O usuário fixou a regra 3 antes de pedir código. O pedido já inclui registrar a pendência e sincronizá-la no prazo. |
| "A nota já existe" | Existir não é estar atualizada. Artefato mudou, nota muda junto, com histórico. |
| "A pasta não existe ainda" | Crie a pasta. O passo 2 existe para isso. |
| "Nenhum pipeline verifica mesmo" | Correto, e é o motivo pelo qual isto depende de você. A próxima sessão lê a base como verdade; base errada produz trabalho errado. |

## Sinais de alerta — pare e volte

- Você pensou "depois eu documento" sem registrar pendência na continuidade.
- Você mudou migração, manifesto de pacote ou arquivo de domínio sem pendência rastreada ou nota conferida em `<BASE>`.
- Sua resposta ao usuário não tem o bloco de declaração.
- Você escreveu regra nova dentro de `CLAUDE.md`, `AGENTS.md` ou `.claude/` em vez do canônico.
- Você declarou caminhos que não conferiu que existem.

Qualquer um deles: a entrega não está pronta.
