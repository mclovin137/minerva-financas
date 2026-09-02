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
Implementar somente datas, mercado histórico e consultas temporais do FDD-002, após T-001 e aprovação independente.

## Origem
- Caminho: fluxo completo, por regras, persistência e contratos.
- PRD: PRD-001, FR-005 a FR-007 e critérios temporais.
- FDD aprovado: FDD-002, aprovação de Yoda pendente.
- HLD: HLD-001.
- ADRs aplicáveis: ADR-001 e ADR-002.

## Resumo decisório mínimo
- Objetivo: tornar saldo, posição e movimentos corretos por data.
- Decisão: janela [emissão, vencimento), dias úteis e preço <= consulta.
- Evidências: testes fora de ordem, filtros e seleção temporal.
- Riscos e lacunas: ausência de preço elegível é ❓ LACUNA.
- Próximo passo: aprovar FDD-002 e executar após T-001.

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
- T-001 concluída; FDD-002 e HLD-001 aprovados.

## Passos de implementação
1. Definir contratos temporais derivados do FDD.
2. Implementar validação de janela e consulta histórica.
3. Validar janela SQL e produzir evidências.

## Critérios de aceite
- [ ] Emissão inclusiva, vencimento exclusivo e apenas dias úteis.
- [ ] Consultas respeitam inclusive/inclusive e data de corte.
- [ ] Preço escolhido é o mais recente <= consulta.
- [ ] Nenhuma data fica negativa, inclusive inserção fora de ordem.

## Testes obrigatórios
- [ ] Unitários de datas, escalas e invariantes.
- [ ] Integração reexecutável de cada endpoint temporal, com fixture isolada e limpeza, rollback ou reset; sem exigir idempotência semântica do POST.

## Evidências obrigatórias
- [ ] Relatórios com casos de fronteira e SQL temporal.
- [ ] Imagem e/ou vídeo de endpoints publicado pelo pipeline, quando aplicável.

## Documentação e Obsidian
- Arquivos `.md`: FDD-002, PRD-001 e HLD-001.
- Notas Obsidian: `Minerva/tasks/t-002-nivel-2.md` e notas relacionadas pendentes.

## Riscos e lacunas
- ❓ LACUNA: ausência de mercado elegível; dono Yoda; bloqueia regra de consulta.

## Definição de pronto
- [ ] Critérios implementados e testados.
- [ ] Evidências e documentação produzidas.
- [ ] Auditoria independente e pipelines verdes.

## Histórico
- 2026-09-02: task criada com status proposta.
