---
type: minerva-task
project: Minerva
date: 2026-09-02
status: proposta
tags: [minerva, task]
ai-first: true
---

# T-001: Entregar APIs financeiras do nível 1

## Para o futuro agente
Implementar somente o contrato fechado no [FDD-001](../fdds/fdd-001-nivel-1.md). **A dependência de governança está satisfeita: Yoda revisou e aprovou o FDD-001 em 2026-09-02 (versão 1.2).** As decisões D-A1 a D-A10 e as fronteiras de pacote do item 7 são normativas; nada nelas deve ser reinterpretado na implementação.

## Origem e responsabilidade
- Fluxo completo; PRD-001 FR-001 a FR-004; HLD-001; ADR-001 e ADR-002 quando aplicável.
- Planejamento: Yoda; implementação: Severino; auditoria: Patrick Jane.

## Escopo fechado
- Conta: `POST /contacorrente/credito`, `POST /contacorrente/debito`, `GET /contacorrente/saldo?data=...`.
- Ativos: CRUD em `/ativos`; compra/venda em `/movimentacao`; consultas com período; posição síncrona `/posicao?data=...`.
- `data` é persistida no N1; temporalidade completa fica no N2. Excluir Basic, async, Docker e UI não desenhada.

## Passos
1. Remover a árvore vazia `backend/src/**/java/br/com/maps/` e criar as camadas sob `br.com.minerva.financas`, conforme FDD-001 item 7.
2. Ajustar `backend/src/main/resources/schema.sql` para a representação exata da decisão D-A2 (`preco_mercado_e8` e `quantidade_e2` como `INTEGER`) e o perfil `test` de `application.yml` para arquivo temporário por execução (D-A10).
3. Implementar contratos, validações, contrato de erro único e persistência atômica com `BEGIN IMMEDIATE` (D-A7).
4. Implementar preço médio ponderado, `rendimento` e `lucro` conforme D1, com floor somente no quociente e nas escalas do item 4 do FDD-001.
5. Persistir o `precoMercado` do N1 como preço na data-âncora `0001-01-01` (D-A1) e resolver o proprietário para `usuario0` (D-A9).
6. Executar TC-001 a TC-032, em qualquer ordem e repetidamente, coletar artefatos e abrir PR.

## Checklist rastreável
- [ ] TC-001–008: lançamentos, saldo, bordas, validação, conflitos e concorrência.
- [ ] TC-009–016: CRUD de ativos, tipos, escalas, inexistência, dependência e concorrência aplicável.
- [ ] TC-017–024: compra/venda, atomicidade, saldo/quantidade, bordas e concorrência.
- [ ] TC-025–032: posição, média ponderada, floor, rendimento, lucro e ativo sem compra.
- [ ] Cada endpoint tem integração REST/persistência, fixture exclusiva, limpeza própria, execução repetível em qualquer ordem e request/response publicado em `artifacts/testes/<commit>/<tc-id>/` pelo pipeline `testes-integracao`; o PR referencia esses caminhos.
- [x] FDD-001 aprovado por Yoda antes do código — revisão arquitetural de 2026-09-02, FDD-001 v1.2. Patrick Jane audita depois da implementação.
- [ ] Gate de direção de dependência do item 7 do FDD-001 verde no pipeline `review`.

## Definição de pronto
- [ ] Todos os TCs acima verdes no pipeline `testes-integracao`.
- [ ] Evidências request/response e relatório existem por TC e estão referenciadas no PR.
- [ ] Diff contém somente o escopo desta task; documentação/nota Obsidian atualizadas.
- [ ] Auditoria independente aprovada; pipeline de review verde.

## Histórico
- 2026-09-02: rotas e critérios fechados; responsabilidade de precisão e evidências particionada por TCs após auditoria.
- 2026-09-02: dependência de governança satisfeita — FDD-001 aprovado por Yoda; passos ampliados com schema exato, perfil de teste, pacotes e âncora de preço.
