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

# T-002: Entregar temporalidade e mercado do nível 2

## Para o futuro agente
Implementar somente datas, mercado histórico e consultas temporais do FDD-002, após T-001 e aprovação independente. **A dependência de governança está satisfeita: Yoda revisou e aprovou o FDD-002 em 2026-09-02 (versão 1.1).** As decisões D-B1 a D-B5 são normativas. A e B do nível 3 são cumulativas, mas continuam fora desta task.

## Origem
- Caminho: fluxo completo, por regras, persistência e contratos.
- PRD: PRD-001, FR-005 a FR-007 e critérios temporais.
- FDD aprovado: FDD-002 v1.1, **aprovado por Yoda em 2026-09-02**.
- HLD: HLD-001.
- ADRs aplicáveis: ADR-001 e ADR-002.

## Resumo decisório mínimo
- Objetivo: tornar saldo, posição e movimentos corretos por data.
- Decisão: janela [emissão, vencimento), dias úteis e preço <= consulta.
- Evidências: TC-033 a TC-048, com testes fora de ordem, filtros e seleção temporal.
- Riscos e lacunas: a ausência de preço elegível **deixou de ser lacuna** — está decidida em FDD-002, D-B3. Risco remanescente: âncora `0001-01-01` sobreviver à migration e mascarar D-B3.
- Próximo passo: executar após T-001, seguindo D-B1 a D-B5.

## Responsáveis
- Planejamento: Yoda
- Implementação: Severino
- Auditoria: Patrick Jane

## Resultado esperado
Nível 2 temporalmente consistente e verificável.

## Escopo
### Incluído
- Emissão/vencimento, mercado por data, filtros e não negatividade em toda data.

### Excluído
- Opções A/B, Docker e estados de UI não desenhados.

## Dependências e bloqueios
- T-001 concluída; FDD-002 aprovado por Yoda em 2026-09-02; HLD-001 v1.1.

## Passos de implementação
1. Implementar `PUT` e `DELETE /ativos/{ativo}/precos/{data}` conforme FDD-002, item 5.1.
2. Migrar `/ativos` para o payload do N2: `dataEmissao`/`dataVencimento` obrigatórias, `precoMercado` rejeitado com 400 (D-B2).
3. Migration: remover as âncoras `data = '0001-01-01'`, tornar `data_emissao`/`data_vencimento` `NOT NULL` e reexecutar o seed determinístico.
4. Implementar validação de janela `[emissão, vencimento)`, dia útil sem feriados (D-B4) e status 400 para ambos (D-B5).
5. Implementar a seleção do preço mais recente `<= data` e o tratamento de ausência de preço elegível conforme D-B3.
6. Atualizar os casos TC-009, TC-012 e TC-015 da matriz para o payload do N2, conforme FDD-002, item 8.
7. Validar janela SQL e produzir evidências.

## Critérios de aceite
- [ ] TC-033–040: preço por data, emissão/vencimento, validações, ausência e concorrência.
- [ ] TC-041–048: consultas com período obrigatório, limites inclusivos, fim de semana e inserção fora de ordem.
- [ ] Cada caso usa fixture exclusiva, limpeza própria e execução repetível em qualquer ordem.

## Testes obrigatórios
- [ ] Unitários de datas, escalas e invariantes.
- [ ] Integração reexecutável de cada endpoint temporal, com fixture isolada e limpeza, rollback ou reset; sem exigir idempotência semântica do POST.

## Evidências obrigatórias
- [ ] Relatórios dos TC-033–048 e request/response em `artifacts/testes/<commit>/<tc-id>/`, publicados pelo pipeline `testes-integracao` e referenciados no PR.

## Documentação e Obsidian
- Arquivos `.md`: FDD-002, PRD-001 e HLD-001.
- Notas Obsidian: `Minerva/tasks/t-002-nivel-2.md` e notas relacionadas pendentes.

## Riscos e lacunas
- Resolvido: a ausência de preço de mercado elegível está decidida em FDD-002, D-B3 — `precoMercado`, `valorMercadoTotal` e `rendimento` vêm `null`, com `200 OK`; sem preço zero, sem preço posterior e sem omitir o ativo. Não bloqueia mais a regra de consulta.
- Risco aberto: quebras de compatibilidade do item 8 do FDD-002 não refletidas na matriz produzem falso verde.

## Definição de pronto
- [ ] Critérios implementados e testados.
- [ ] Evidências e documentação produzidas.
- [ ] Auditoria independente e pipelines `testes-integracao` e `review` verdes.

## Histórico
- 2026-09-02: task criada com status proposta.
- 2026-09-02: dependência de governança satisfeita — FDD-002 aprovado por Yoda; lacuna de preço elegível fechada e passos ampliados com rotas, migration e compatibilidade.
