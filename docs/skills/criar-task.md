# Skill: criar-task

**Autor:** Cristóvão Augusto

**Definição canônica.** Adaptador de descoberta em `.claude/skills/criar-task/SKILL.md`, sem conteúdo próprio.

## Para o futuro agente

Derivar uma unidade executável do fluxo completo ou uma task curta da via rápida, sempre com escopo fechado, dono implementador, validações, evidências e revisão independente. A task traduz especificação aprovada ou manutenção delimitada em trabalho verificável; não cria requisito de negócio nem decisão arquitetural.

Se a task recorrer a agentes ou documentos, explique as dependências antes de usá-las.

## Responsabilidade e limites

- Exercer a responsabilidade de **planejar/revisar**.
- Não implementar a task planejada quando houver agente implementador disponível.
- Não designar o implementador como auditor ou aprovador da própria entrega.
- No fluxo completo, não iniciar sem PRD e FDD aprovado quando houver comportamento, regra, integração, contrato público ou risco relevante. Mudança estrutural também exige HLD vigente. Na via rápida, exigir task curta e confirmação explícita de que não há comportamento de produto nem decisão estrutural. A exceção enxuta da regra 9 não passa por esta skill: só pode ser usada quando houver autorização explícita do usuário para aquela mudança e não houver gatilho documental, requisito legal/obrigatório, risco de segurança ou qualquer exclusão descrita em `docs/rules.md`.
- Não inventar regra de negócio, arquitetura, stack, banco, hospedagem ou CI. Usar `❓ LACUNA` e devolver ao dono do artefato adequado.
- Não ampliar o escopo para aproveitar a entrega. Trabalho independente vira outra task.
- Separar implementação e auditoria em tasks distintas quando ambas precisarem ser rastreadas.

## Classificação e entradas obrigatórias

- **Fluxo completo:** PRD identificado, FDD aprovado quando aplicável, HLD quando a entrega tocar estrutura, fronteiras ou contratos entre componentes, e implementador/auditor distintos.
- **Via rápida:** objetivo, superfície permitida, exclusões, validações proporcionais, evidência esperada, justificativa de ausência de comportamento de produto e revisor independente.
- Em ambos: ADRs aplicáveis, riscos/lacunas e agentes especializados acionados ou dispensados com justificativa.

Se o fluxo completo não tiver PRD, bloquear. Se o FDD aplicável não existir ou não estiver aprovado, devolver para elaboração ou revisão. Se a mudança for estrutural e não houver HLD vigente, bloquear. Se a via rápida tocar comportamento de produto, decisão estrutural ou risco não coberto, reclassificar para fluxo completo. Urgência não cria terceira via: só pode usar a exceção enxuta com autorização explícita do usuário e dentro da regra 9; colisão material, inclusive em produção parada, segue a regra 10.

## Coleta

Quando informações não estiverem nos documentos, fazer **uma pergunta por vez**. Ao fim de cada etapa, resumir em 3 a 6 linhas e pedir confirmação. Não perguntar novamente o que já está explícito.

1. Classificar a mudança e registrar a justificativa; identificar PRD, FDD, HLD e ADRs aplicáveis ou a superfície limitada da via rápida.
2. Definir resultado único, observável e pequeno o bastante para uma entrega.
3. Fechar incluído, excluído e arquivos ou módulos esperados sem impor layout ainda não decidido.
4. Mapear dependências, bloqueios e ordem de execução.
5. Nomear implementador e auditor, garantindo separação.
6. Derivar critérios de aceite do PRD/FDD no fluxo completo ou validações mecânicas proporcionais na via rápida, sem criar significado novo.
7. Derivar testes por comportamento, invariantes e endpoints. Todo endpoint exige integração idempotente com evidência definida na ADR de testes.
8. Definir evidências objetivas de implementação, testes, pipeline e documentação.
9. Mapear notas Obsidian obrigatórias, registrar cada pendência em `docs/pendencias-obsidian.md` com origem, destino, responsável e prazo máximo de 24 horas, e apontar riscos ou `❓ LACUNA`.

## Procedimento

1. Ler `docs/rules.md`, `docs/continuidade.md`, `docs/lib.md` e toda a cadeia aplicável antes de criar a task.
2. Procurar task equivalente na base e determinar o próximo `T-NNN` sem duplicar trabalho.
3. Conduzir a coleta apenas para dados ausentes.
4. Executar as checagens de consistência.
5. Renderizar exatamente o molde Markdown, registrar a pendência correspondente em `docs/pendencias-obsidian.md` e sincronizar em `<BASE>/tasks/t-NNN-<slug>.md` no prazo da regra 3.
6. Perguntar se o usuário deseja exportação JSON. Só exportar se solicitado, com chaves em inglês, valores textuais em pt-BR e sem campos vazios.

## Checagens de consistência

- O caminho está classificado e justificado.
- No fluxo completo, PRD existe e cada critério de aceite aponta para sua origem; FDD aplicável está aprovado por revisor diferente do autor; HLD existe quando há impacto estrutural.
- Na via rápida, a superfície é fechada, não há comportamento de produto e as validações proporcionais são reproduzíveis.
- Resultado é único, executável e não contém decisão pendente disfarçada.
- Incluído e excluído não se contradizem.
- Dependências e bloqueios estão explícitos.
- Implementador e auditor são pessoas ou agentes distintos.
- Critérios de aceite são objetivos e verificáveis.
- Testes cobrem comportamento e invariantes; endpoint inclui integração idempotente e evidência.
- Mudanças documentais e notas Obsidian foram listadas.
- Toda lacuna tem dono e bloqueio declarado.

## Molde Markdown

```markdown
---
type: minerva-task
project: Minerva
date: AAAA-MM-DD
status: proposta|pronta|em-andamento|bloqueada|em-auditoria|concluída|cancelada
tags:
  - minerva
  - task
ai-first: true
---

# T-NNN: <título orientado a resultado>

## Para o futuro agente
<Resultado da task, por que ela existe e quando está concluída.>

## Origem
- Caminho: <via-rápida|fluxo-completo e justificativa>
- PRD: <identificador e critério, ou não aplicável na via rápida>
- FDD aprovado: <identificador e evidência de aprovação, ou não aplicável com justificativa>
- HLD: <identificador ou não aplicável com justificativa>
- ADRs aplicáveis: <identificadores ou nenhuma>

## Resumo decisório mínimo
- Objetivo: <resultado e limite>
- Decisão: <classificação, decisões aplicadas ou pendentes>
- Evidências: <validações e artefatos consultados>
- Riscos e lacunas: <itens ou nenhum identificado>
- Próximo passo: <ação, dono e condição>

## Responsáveis
- Planejamento: <agente>
- Implementação: <agente>
- Auditoria: <agente distinto>

## Resultado esperado
<Uma entrega observável.>

## Escopo

### Incluído
- <item>

### Excluído
- <item>

## Dependências e bloqueios
- <dependência, estado e dono>

## Passos de implementação
1. <passo verificável>
2. <passo verificável>

## Critérios de aceite
- [ ] <critério objetivo e origem PRD/FDD>

## Testes obrigatórios
- [ ] <teste por comportamento ou invariante>
- [ ] <teste de integração idempotente por endpoint, quando aplicável>

## Evidências obrigatórias
- [ ] <saída verificável>
- [ ] Imagem e/ou vídeo dos testes de endpoint publicado pelo pipeline, quando aplicável

## Documentação e Obsidian
- Arquivos `.md` do repositório: <itens ou nenhum>
- Notas Obsidian: <caminhos previstos>

## Riscos e lacunas
- <risco, mitigação e dono>
- ❓ LACUNA: <questão, dono e efeito bloqueante>

## Definição de pronto
- [ ] Implementação respeita os artefatos aplicáveis ou a superfície e as validações da via rápida.
- [ ] Testes e evidências obrigatórias foram produzidos.
- [ ] Documentação e notas Obsidian estão atualizadas.
- [ ] PR foi auditado por agente diferente do implementador.
- [ ] Pipelines obrigatórios estão verdes.

## Histórico
- AAAA-MM-DD: task criada com status <status>.
```

## JSON opcional

```json
{
  "meta": {
    "id": "T-NNN",
    "title": "",
    "status": "proposed|ready|in_progress|blocked|in_audit|completed|cancelled",
    "date": "YYYY-MM-DD"
  },
  "sources": {
    "prd": "",
    "fdd": "",
    "hld": "",
    "adrs": []
  },
  "owners": {
    "planner": "",
    "implementer": "",
    "auditor": ""
  },
  "expected_outcome": "",
  "scope": {
    "included": [],
    "excluded": []
  },
  "dependencies_blockers": [],
  "implementation_steps": [],
  "acceptance_criteria": [],
  "required_tests": [],
  "required_evidence": [],
  "documentation": [],
  "risks_gaps": []
}
```

## Entrega

Informar se a task está pronta ou bloqueada, lacunas, separação de responsabilidades e caminho completo da nota. Não iniciar implementação nem declarar aceite em nome do auditor.
