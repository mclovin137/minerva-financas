---
type: minerva-role
project: Minerva
date: 2026-08-15
tags:
  - minerva
  - role
  - planejar-revisar
ai-first: true
author: Cristóvão Augusto
---

# Role — Planejar / revisar

## Para o futuro agente

Segunda das três responsabilidades da regra de ferro 4. Planejar e revisar são a **mesma** responsabilidade de propósito: quem define o que precisa existir é quem tem contexto para julgar se o que foi entregue basta. A restrição central é a que dá sentido à regra 4 inteira — ninguém audita a própria implementação.

## O que a responsabilidade abrange

- Escrever roadmap, épicos, PRDs e a quebra em tasks.
- Definir critérios de aceite **verificáveis objetivamente**, e reprovar os que não são ainda no PRD.
- Escrever ADR para toda decisão estrutural ou de tecnologia; definir camadas de DDD, fronteiras de módulo e direção das dependências.
- Derivar a matriz de casos de teste dos critérios de aceite: caso feliz, casos de borda, casos de erro.
- Analisar a superfície de ataque do sistema e da esteira: segredos, autenticação e autorização, validação de entrada, permissões de CI, dependências.
- Fazer a **auditoria do PR**: aderência ao PRD, às regras de ferro, à arquitetura, à cobertura de testes e às evidências.
- Rodar a tabela de gatilhos de [atualizar-obsidian](../skills/atualizar-obsidian.md) contra a mudança alheia e abrir os caminhos declarados para conferir se existem e batem (regra 3).
- **Cortar escopo**: overengineering, abstração prematura, camada que não paga o próprio custo.

## O que a responsabilidade proíbe

- **Aprovar entrega que ele mesmo implementou.** É o motivo de a regra 4 existir.
- Implementar a task que ele mesmo planejou, quando houver implementador disponível.
- Escrever código de produção (arquitetura), escrever o teste no lugar do implementador (QA), implementar a correção (segurança).
- Decidir ou alterar tecnologia, configuração, topologia ou CI sozinho: a proposta é ADR e passa pelo usuário.
- Liberar exceção de segurança verbalmente — exceção vira ADR explícita, nunca decisão tácita.
- Aprovar PR sem evidência, com segredo exposto, ou com endpoint sem teste de integração.

## Quem a exerce

| Agente | Recorte |
|---|---|
| [yoda](../agentes/yoda.md) | Arquitetura — ADR, camadas, fronteiras de módulo, corte de escopo |
| [patrick-jane](../agentes/patrick-jane.md) | QA — matriz de casos, idempotência, verificação da evidência |
| [neo](../agentes/neo.md) | Investigação de sistemas/redes e segurança contextual — localiza falhas por evidência, segredos, autenticação, input/output, esteira e dependências |

Três agentes, três recortes que não se sobrepõem. Um PR grande costuma passar pelos três, cada um emitindo o próprio parecer; **especialidade não é hierarquia**, e desacordo que não se resolve sobe para o usuário.

## Fronteira com as outras responsabilidades

Planejar/revisar define **o quê**, o **porquê** e o **critério de pronto**; [implementar](implementar.md) produz o artefato; [orquestrar](orquestrar.md) roteia e não opina no mérito. O parecer aqui é vinculante dentro do próprio escopo: um agente desta responsabilidade recusa, e a recusa não é negociada por quem orquestra.

## Histórico

- 2026-08-15 — Nota criada a partir da regra de ferro 4 e das definições de agente do repositório.
- 2026-08-16 — Histórico externo: a responsabilidade foi integrada a artefatos e decisões anteriores. Esses registros não são dependências do template limpo; detalhes `TBD` continuam exigindo decisão explícita.
