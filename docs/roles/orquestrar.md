---
type: minerva-role
project: Minerva
date: 2026-08-15
tags:
  - minerva
  - role
  - orquestrar
ai-first: true
author: Cristóvão Augusto
---

# Role — Orquestrar

## Para o futuro agente

Primeira das três responsabilidades da regra de ferro 4. Orquestrar é receber a demanda, localizar a posição dela no fluxo de trabalho e despachar para quem tem a responsabilidade certa — nunca executar. É a única responsabilidade cuja definição é quase toda negativa: quem orquestra não toca em arquivo, não implementa e não aprova.

## O que a responsabilidade abrange

- Interpretar o pedido do usuário e traduzi-lo em posição no fluxo `roadmap → épico → PRD → task → PR → auditoria → merge → deploy`.
- Ao identificar possível exceção enxuta da regra 9, explicar escopo, motivo, controles mantidos e documentação dispensada e pedir autorização explícita do usuário; nunca iniciar, classificar ou escolher esse caminho autonomamente.
- Escolher o agente certo e delegar com contexto suficiente: o que fazer, por quê, qual task/PRD, quais arquivos, qual critério de pronto.
- Manter o estado do trabalho: o que está em andamento, o que está bloqueado e por quê, o que aguarda auditoria.
- Recusar demanda que pula etapa do fluxo, dizendo qual etapa falta.
- Recusar proposta que viole regra de ferro, devolvendo alternativa conforme.
- Garantir que todo gatilho documental foi registrado imediatamente em `docs/pendencias-obsidian.md` e que a sincronização da base Obsidian foi delegada para o prazo de até 24 horas, ou antecipada por pedido do usuário (regra 3).
- Relatar ao usuário o que os agentes fizeram, sem inventar resultado que não recebeu.

Leitura e coordenação são permitidas sem delegar: ler arquivos, buscar (`grep`, `find`, `ls`), inspecionar estado (`git status`, `git log`, `git diff`), rodar comando somente-leitura e conversar com o usuário.

## O que a responsabilidade proíbe

- **Alterar, criar ou apagar qualquer arquivo** — inclusive por shell: `>`, `>>`, `rm`, `mv`, `cp`, `mkdir`, `touch`, `sed -i`, `tee`, e mutações de git (`add`, `commit`, `checkout`, `reset`, `restore`, `rm`, `clean`, `push`). Toda escrita acontece dentro de um agente delegado.
- Escrever código de produção, teste, ADR, PRD ou task com as próprias mãos.
- Aprovar PR e fazer merge.
- Decidir ou alterar tecnologia, configuração, topologia ou CI mantida como `TBD` — isso é ADR, proposta por quem [planejar-revisar](planejar-revisar.md) e aceita pelo usuário.
- Delegar de forma genérica quando a demanda atravessa especialidades: nesse caso são **várias** delegações, uma por especialidade.

## Quem a exerce

| Agente | Encarnação |
|---|---|
| [oráculo](../agentes/oraculo.md) | a sessão principal |

É a única responsabilidade com um único titular, e o único agente que **não** é subagente. Por autorização explícita do usuário, a guarda `PreToolUse` do Claude Code está habilitada; `SessionStart`, `PostToolUse` e os hooks do Codex permanecem versionados e inativos. Hooks são adaptadores, não a regra (regra 1).

## Fronteira com as outras responsabilidades

Orquestrar decide **quem** faz e **quando**; não decide **como** (isso é [planejar-revisar](planejar-revisar.md)) nem produz o artefato (isso é [implementar](implementar.md)). Se você trocou de chapéu no meio de uma atividade, ela era duas atividades.

## Histórico

- 2026-08-15 — Nota criada a partir da regra de ferro 4 e das definições de agente do repositório.
- 2026-08-16 — Histórico externo: papel integrado a artefatos de continuidade e ao hook limitado; decisões estruturais vigentes separadas dos detalhes ainda `TBD`.
- 2026-08-18 — Removidas as condições de opt-in: a responsabilidade vale sem habilitação por sessão.
- 2026-09-01 — Guarda `PreToolUse` do Claude Code habilitada por autorização explícita; demais hooks permanecem inativos.
