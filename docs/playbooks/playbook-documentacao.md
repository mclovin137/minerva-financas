# Playbook — qual documento criar

**Autor:** Cristóvão Augusto

## Para o futuro agente

Consulta seletiva sobre **tipos de documento**: quais o projeto reconhece além da cadeia principal, quando cada um cabe e quando é legado.

**Este playbook não contém regra.** A obrigação — proporcionalidade, declaração de origem, reciprocidade de links e a proibição de tratar documento obsoleto como contexto vigente — vive em [`docs/rules.md`](../rules.md), seção *Fluxo de trabalho → Documentos: qual criar, e por quê*, e é lá que ela muda (regra de ferro 1). Aqui está apenas o material de referência que antes ocupava o contrato e era carregado por todo agente em todo turno, sem ser consultado em todo turno.

A cadeia principal — PRD → HLD → FDD → LLD, com RFC antes da decisão e ADR depois dela — está na `rules.md` e não é repetida aqui.

## Mapa por categoria

| Categoria | Relevantes no fluxo | Contextuais ou emergentes | Legados ou de uso restrito |
|---|---|---|---|
| Produto | PRD | épico e user story, conforme a gestão de produto | FRD, quando houver acervo ou contrato herdado a preservar |
| Design e arquitetura | HLD, FDD, ADR e modelo C4 | RFC, LLD, AI Design Doc e Prompt Spec | TRD e LLD no formato RUP, salvo migração ou obrigação externa |
| Conhecimento e referência | engineering guidelines, playbooks e Security Design Doc | documento de avaliação de IA | plano e caso de teste formais isolados, salvo necessidade de auditoria ou contrato externo |
| Operação e infraestrutura | runbook, playbook e documentação de incidente | observabilidade, capacidade e telemetria | Design Doc de infraestrutura e CI/CD quando não forem escopo da task ou responsabilidade do time |

## Produto

**PRD** é a referência de problema, valor e critério de aceite. Épicos e user stories podem complementar a gestão de produto, mas **não substituem critérios verificáveis**.

**FRD** é tratado como nomenclatura ou legado: se existir, preserva-se a rastreabilidade e mapeia-se sua função para o encadeamento vigente, sem criar um paralelo ritualístico. Sua função é semelhante à do FDD.

## Design e arquitetura

**HLD, FDD, ADR e C4** são os artefatos centrais para organizar a solução. **RFC e LLD** seguem o critério proporcional da `rules.md`.

**AI Design Doc e Prompt Spec** são usados quando um comportamento, avaliação, contexto, limitação ou operação de IA fizer parte do sistema. Devem declarar objetivo, entradas, saídas, limites, avaliação e revisão humana. **Não são exigidos por um uso incidental de ferramenta de IA no desenvolvimento** — escrever código com auxílio de LLM não gera Prompt Spec; embarcar uma capacidade de IA no produto, sim.

**TRD e LLD no formato RUP** são legado: só aparecem sob migração ou obrigação externa, e não são criados por iniciativa própria.

## Conhecimento e referência

**Engineering guidelines, skills e playbooks** preservam práticas reutilizáveis.

**Security Design Doc** é criado quando o risco de segurança precisar de desenho próprio além de PRD, HLD, FDD e do parecer do Neo — não como etapa fixa.

**Documento de avaliação de IA** descreve métricas, conjunto de avaliação, critérios de aprovação, riscos e regressões, quando a aplicação usar IA.

**Plano e caso de teste formais isolados** são de uso restrito. Estratégia, casos e evidências de teste continuam obrigatórios pelas regras de QA da `rules.md`; o que é contextual é o documento formal **separado** — não a obrigação de testar.

## Operação e infraestrutura

**Runbooks e playbooks** orientam operação, incidente e recuperação.

**Documentos de observabilidade, capacidade, infraestrutura e CI/CD** são criados quando a mudança tocar esses domínios ou quando Jarvis os exigir. **Não são dispensados por serem secundários à codificação.** A responsabilidade depende da task: Severino implementa o pipeline-as-code atribuído, e Jarvis define operação, garantias de liberação e pós-deploy.
