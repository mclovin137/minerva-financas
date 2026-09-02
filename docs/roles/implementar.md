---
type: minerva-role
project: Minerva
date: 2026-08-15
tags:
  - minerva
  - role
  - implementar
ai-first: true
author: Cristóvão Augusto
---

# Role — Implementar

## Para o futuro agente

Terceira das três responsabilidades da regra de ferro 4. Implementar é produzir o artefato — código, teste, pipeline, configuração — junto com a evidência que prova que funciona e a nota que documenta o que mudou. A entrega só está pronta com as três coisas; teste e documentação não são fase posterior.

## O que a responsabilidade abrange

- Implementar a task dentro das camadas e fronteiras definidas em [planejar-revisar](planejar-revisar.md).
- Escrever o **teste de integração** de todo endpoint tocado: idempotente, gerando as evidências definidas pela ADR de testes (regra de ferro 7).
- Construir e manter os dois pipelines (regra 8) e o workflow de deploy aprovado, sem alterar recursos externos fora da task autorizada.
- Monitorar o consumo do free tier e avisar antes de encostar no limite (regra 5).
- Registrar imediatamente em `docs/pendencias-obsidian.md` a pendência de todo artefato-gatilho tocado e sincronizar a base Obsidian em até 24 horas, ou antes por pedido do usuário (regra 3, ver [atualizar-obsidian](../skills/atualizar-obsidian.md)).
- Abrir o PR com link para task e PRD, evidências anexadas e a declaração dos caminhos escritos na base.
- Responder ao review e corrigir o que a auditoria apontar.

## O que a responsabilidade proíbe

- **Aprovar ou fazer merge do próprio PR.**
- Começar sem PRD e task — se a task não existe, devolve para quem [orquestrar](orquestrar.md).
- Ampliar o escopo da task por conta própria: trabalho a mais vira task nova.
- Adicionar dependência ou serviço pago, ou de custo incerto (regra 5); contratar qualquer coisa.
- Escolher ou alterar tecnologia, configuração, topologia ou CI mantida como `TBD`: isso é ADR, e implementar decisão estrutural sem ADR é recusa obrigatória.
- Entregar endpoint sem teste de integração com evidência publicada como artefato.
- Deixar documentação para depois — "abro outro PR para a base" não existe.
- Adotar deploy manual como procedimento, ou credencial estática de deploy quando existir alternativa sem chave.

## Quem a exerce

| Agente | Recorte |
|---|---|
| [severino](../agentes/severino.md) | Todo o código da aplicação, back-end e front-end, migrations, pipeline-as-code, testes e documentação associada |
| [jarvis](../agentes/jarvis.md) | SRE / DevOps — gates pré-deploy, deploy e rollback, ambientes, observabilidade, backup/restore e incidente após a liberação |

A fronteira entre os dois é explícita: Severino implementa e mantém os arquivos do pipeline conforme ADR/HLD; Jarvis define as garantias pré-deploy e é dono do deploy e da operação após a liberação. Nenhum decide arquitetura.

## Estado em 2026-08-15

As regras vigentes definem somente as fundações do template; a base arquitetural de uma aplicação consumidora depende de suas próprias ADRs. Ainda não existe task de produto derivada de PRD e FDD; por isso implementação de feature continua bloqueada, embora tarefas operacionais e documentais explicitamente delegadas possam executar decisões aceitas.

## Histórico

- 2026-08-15 — Nota criada a partir da regra de ferro 4 e das definições de agente do repositório.
- 2026-08-16 — Histórico externo: fronteira Severino/Jarvis, decisões e artefatos de continuidade foram atualizados. Esses registros não são dependências do template limpo.
