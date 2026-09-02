# Skill: auditoria

**Autor:** Cristóvão Augusto

**Definição canônica e independente de ferramenta.** Adaptador de descoberta em `.claude/skills/auditoria/SKILL.md`, sem conteúdo próprio.

## Para o futuro agente

Auditar uma entrega depois do PR e antes do merge, comparando primeiro a cadeia aprovada e só depois o diff. A auditoria pertence à responsabilidade de **planejar/revisar** e precisa ser feita por agente diferente de quem implementou.

A auditoria requer a definição e a responsabilidade de planejar/revisar; quem implementou não audita.

## Origem e adaptação

Adaptada da skill `auditoria` do projeto [mclovin137/TGM2](https://github.com/mclovin137/TGM2/tree/abc15b18bdc3e814c1dffa5e4e1eb99de7a7e292/.claude/skills/auditoria), commit `abc15b18bdc3e814c1dffa5e4e1eb99de7a7e292`. A origem não declarou licença própria. A adaptação remove referências a `roles.md`, `state.md`, `plan.md`, pré-push e branch específica, e aplica o fluxo e os gates do projeto Minerva.

## Entradas e bloqueios

- PR identificado; task identificada, exceto na exceção enxuta da regra 9.
- Classificação da mudança e artefatos correspondentes: PRD, HLD, FDD e ADRs aplicáveis no fluxo completo; justificativa, superfície permitida e validações na via rápida; ou autorização explícita do usuário, justificativa de elegibilidade, escopo e validações registradas no PR para a exceção enxuta.
- Diff completo, inclusive arquivos não versionados relevantes.
- Resultado dos dois pipelines obrigatórios: review e execução de casos de teste.
- Evidências dos testes de integração definidos pela ADR por endpoint.

Bloquear se faltar artefato obrigatório do caminho classificado, se a exceção enxuta não tiver autorização explícita do usuário ou tocar exclusão da regra 9, se o autor estiver revisando a própria implementação ou se não houver evidência suficiente para um veredito. Via rápida e exceção enxuta não dispensam revisão independente nem a especialidade acionada pelo risco.

## Procedimento

1. Ler `docs/rules.md`, `docs/continuidade.md` e `docs/lib.md`.
2. Identificar a task quando aplicável, validar a classificação e reconstruir o caminho: PRD, HLD, FDD e ADRs no fluxo completo; objetivo, superfície, exclusões, validações e pareceres aplicáveis na via rápida; ou registro no PR, elegibilidade e ausência de gatilhos na exceção enxuta.
3. Extrair escopo, critérios de aceite ou validações mecânicas, invariantes, contratos e decisões vigentes antes de abrir o diff.
4. Inspecionar o diff completo e relacionar cada arquivo ao escopo autorizado.
5. Conferir os resultados dos dois pipelines sem substituir julgamento por automação.
6. Conferir a tabela de gatilhos de `docs/skills/atualizar-obsidian.md` e abrir cada nota declarada.
7. Classificar cada item como `APROVADO`, `REPROVADO` ou `BLOQUEADO`, sempre com evidência.
8. Emitir um único veredito. Depois de correções, revalidar os itens afetados e qualquer efeito colateral, sem presumir que o restante permaneceu válido se o diff mudou materialmente.

## Checklist obrigatório

- A implementação respeita os artefatos aplicáveis do fluxo completo, a superfície, exclusões e validações da via rápida, ou os limites estritos e o registro no PR da exceção enxuta.
- Não há regra de negócio inventada nem divergência arquitetural silenciosa.
- O domínio permanece isolado de framework, transporte e persistência conforme DDD.
- Todo arquivo alterado pertence ao escopo ou tem autorização documental explícita.
- Os critérios de aceite têm evidência reproduzível.
- Todo endpoint possui teste de integração idempotente, com evidência publicada pelo pipeline de casos de teste conforme a ADR aplicável.
- Toda mudança que crie endpoint ou regra de domínio possui Relatório de evidência TDD com as colunas RED/GREEN/REFACTOR.
- Fluxos críticos, falhas, bordas, autorização, concorrência e invariantes aplicáveis estão cobertos.
- Não há segredo, dado sensível em log, vulnerabilidade relevante conhecida ou dependência não analisada.
- Migration, quando houver, tem ida e volta testadas e nota Obsidian por objeto DDL.
- Código e documentação de comportamento mudaram juntos.
- As notas Obsidian estão sincronizadas e conferidas, ou cada pendência tem origem, destino, responsável e prazo de criação dentro de 24 horas em `docs/pendencias-obsidian.md`; reprazo posterior só vale nos termos do regime de reprazo da regra 3, com decisão do usuário, data e motivo registrados. Pendência vencida bloqueia conclusão e trabalho dependente.
- O pipeline de review e o pipeline de execução de casos de teste estão verdes.
- Publicação e deploy devem aderir ao workflow aprovado e à regra de custo financeiro zero.

## Formato do relatório

```markdown
# Auditoria: <task ou PR>

Data: AAAA-MM-DD
Auditor: <agente diferente do implementador>
Veredito: APROVADO | REPROVADO | BLOQUEADO

| Item | Resultado | Evidência | Correção ou bloqueio |
| --- | --- | --- | --- |
| Cadeia documental | <resultado> | <referências> | <ação ou nenhuma> |
| Escopo e domínio | <resultado> | <referências> | <ação ou nenhuma> |
| Testes e evidências | <resultado> | <referências> | <ação ou nenhuma> |
| Segurança | <resultado> | <referências> | <ação ou nenhuma> |
| Dados e migrations | <resultado> | <referências> | <ação ou nenhuma> |
| Documentação e Obsidian | <resultado> | <referências> | <ação ou nenhuma> |
| Pipelines e deploy | <resultado> | <referências> | <ação ou nenhuma> |

## Correções necessárias
1. <correção, dono e evidência esperada>
```

Não fazer merge, não aprovar trabalho próprio e não declarar sucesso para comando ou pipeline que não foi executado.
