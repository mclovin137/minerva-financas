---
type: minerva-task
project: Minerva
date: 2026-09-02
status: proposta
tags:
  - minerva
  - task
ai-first: true
---

# T-001: Entregar APIs financeiras do nível 1

## Para o futuro agente
Implementar somente o nível 1 fechado no FDD-001. Conclui quando critérios, testes, evidências e documentação forem aprovados por auditor distinto.

## Origem
- Caminho: fluxo completo, pois há comportamento, regras, persistência e endpoints.
- PRD: PRD-001, FR-001 a FR-004 e critérios de precisão/integridade.
- FDD aprovado: FDD-001, aprovação de Yoda ainda pendente.
- HLD: HLD-001.
- ADRs aplicáveis: ADR-001; ADR-002 para telas desenhadas.

## Resumo decisório mínimo
- Objetivo: conta, ativos, movimentação e posição do nível 1.
- Decisão: escopo fechado no nível 1; datas e opções N3 ficam fora.
- Evidências: testes unitários, REST, persistência e pipeline.
- Riscos e lacunas: rotas N1 não fornecidas no enunciado; Yoda deve fechar antes do código.
- Próximo passo: Yoda aprovar FDD-001; Severino implementar.

## Responsáveis
- Planejamento: Yoda
- Implementação: Severino
- Auditoria: Patrick Jane

## Resultado esperado
APIs REST JSON do nível 1 com invariantes financeiras demonstradas.

## Escopo
### Incluído
- FR-001 a FR-004, centavos, floor, tipos e escalas.

### Excluído
- Datas, Basic, assíncrono, Docker e interface de lacunas.

## Dependências e bloqueios
- FDD-001 e HLD-001 precisam de revisão; ADR-001 vigente.

## Passos de implementação
1. Fechar rotas N1 e contratos de erro.
2. Implementar casos de uso, persistência e integração REST.
3. Produzir testes e evidências.

## Critérios de aceite
- [ ] Saldo nunca negativo e operações correlatas atômicas.
- [ ] Escalas e floor conforme PRD; posição e fórmulas conferidas.
- [ ] Todos os endpoints têm integração idempotente.

## Testes obrigatórios
- [ ] Unitários de dinheiro, fórmulas e invariantes.
- [ ] Integração REST e persistência por endpoint, repetível e isolada.

## Evidências obrigatórias
- [ ] Relatórios de testes e payload/status.
- [ ] Imagem e/ou vídeo dos testes de endpoint publicado pelo pipeline, quando aplicável.

## Documentação e Obsidian
- Arquivos `.md` do repositório: FDD-001, PRD-001 e HLD-001.
- Notas Obsidian: `Minerva/tasks/t-001-nivel-1.md` e notas relacionadas pendentes.

## Riscos e lacunas
- ❓ LACUNA: rotas N1 e política para ativo sem compra; dono Yoda; bloqueia contrato.

## Definição de pronto
- [ ] Implementação respeita PRD, FDD e HLD.
- [ ] Testes e evidências produzidos.
- [ ] Documentação e notas atualizadas.
- [ ] PR auditado por Patrick Jane.
- [ ] Pipelines obrigatórios verdes.

## Histórico
- 2026-09-02: task criada com status proposta.

