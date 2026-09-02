# Skills do projeto Minerva

**Autor:** Cristóvão Augusto

## Para o futuro agente

Esta pasta contém as definições canônicas, independentes de ferramenta, das skills usadas no projeto Minerva. Os adaptadores específicos do Claude Code ficam em `.claude/skills/` e devem apenas apontar para estes arquivos.

Leia a definição correspondente antes de executar uma atividade que se enquadre em sua finalidade. Alterações de procedimento devem ser feitas aqui, e não nos adaptadores.

## Índice

| Skill | Finalidade |
|---|---|
| [api-design](api-design.md) | Planejar e revisar contratos de API. |
| [atualizar-obsidian](atualizar-obsidian.md) | Sincronizar mudanças documentais com a base oficial do Obsidian. |
| [auditar-dependencias](auditar-dependencias.md) | Auditar dependências diretas sem alterar o projeto. |
| [auditoria](auditoria.md) | Auditar a entrega entre o PR e o merge. |
| [criar-migration](criar-migration.md) | Planejar migrations reversíveis e verificáveis. |
| [criar-task](criar-task.md) | Derivar uma task fechada de PRD e FDD aprovados. |
| [deep-research](deep-research.md) | Preparar uma Deep Research ou reestruturar integralmente pesquisa importada. |
| [deployment-patterns](deployment-patterns.md) | Implementar e revisar deploy conforme as decisões vigentes. |
| [error-handling](error-handling.md) | Modelar falhas, exceções e respostas de erro. |
| [gerar-adr](gerar-adr.md) | Analisar e registrar decisões arquiteturais. |
| [gerar-fdd](gerar-fdd.md) | Detalhar o funcionamento interno de uma feature. |
| [gerar-hld](gerar-hld.md) | Definir a organização, as fronteiras e os contratos do sistema. |
| [gerar-prd](gerar-prd.md) | Entrevistar e gerar requisitos de uma feature. |
| [investigar-sistemas-redes](investigar-sistemas-redes.md) | Investigar comunicação end-to-end por TRACE, hipóteses e evidências por camada. |
| [mapear-codebase](mapear-codebase.md) | Mapear o repositório somente por leitura. |
| [refinar-prompts](refinar-prompts.md) | Classificar e aprimorar prompts com escopo, limites e estrutura explícitos. |
| [refinar-task](refinar-task.md) | Refinar um épico antes do primeiro PRD, quando o risco exigir. |
| [security-review](security-review.md) | Revisar segurança de FDD, código e configuração. |
| [security-scan](security-scan.md) | Auditar agentes, hooks, skills e integrações de ferramenta. |
| [tdd-workflow](tdd-workflow.md) | Implementar por RED, GREEN e REFACTOR. |

As definições de responsabilidade dos agentes ficam em [`../agentes/`](../agentes/), e o contrato de governança fica em [`../rules.md`](../rules.md).
