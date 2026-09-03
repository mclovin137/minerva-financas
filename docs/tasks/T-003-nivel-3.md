---
type: minerva-task
project: Minerva
date: 2026-09-02
status: em-auditoria
tags: [minerva, task]
ai-first: true
---

# T-003: Entregar contratos e operação do nível 3

## Para o futuro agente
Implementar o [FDD-003](../fdds/fdd-003-nivel-3-e-opcoes.md), no qual A **e** B são cumulativos e obrigatórios. Não existe lacuna de escolha. **A dependência de governança está satisfeita: Yoda revisou e aprovou o FDD-003 em 2026-09-02 (versão 1.2).** As decisões D-C1 a D-C6 são normativas; permanece um `❓ LACUNA` de dados do enunciado, listado abaixo.

## Origem e responsabilidade
- Fluxo completo; PRD-001 FR-008 a FR-010; HLD-001; ADR-001 e ADR-002.
- Planejamento: Yoda; implementação: Severino; auditoria: Patrick Jane.

## Escopo fechado
- Cinco rotas canônicas de conta/movimentação/saldo, seed `ATIVO0`–`ATIVO127`, Dockerfile e docker-compose.
- Opção A: Basic, seed de usuários, root, autorização e isolamento.
- Opção B: `GET /posicao` e `GET /posicao/{id}`, `425`, `200`, descarte, 200.000 movimentações, heap abaixo de 64 MB por `Runtime` antes/depois e mínimo de 2 threads observáveis.
- Deploy cloud efetivo e telas ❓ LACUNA permanecem fora.

## Checklist rastreável
- [ ] TC-057–064: cinco rotas exatas, payloads, status, seed e concorrência.
- [ ] TC-065–072: Basic em toda requisição, autorização, isolamento, root, falhas e concorrência A.
- [ ] TC-073–080: criação/id, 425, 200, 404 após descarte, falha, 200.000, heap e 2 threads B.
- [ ] Todos os endpoints têm fixture exclusiva, limpeza própria, execução repetível em qualquer ordem e artefatos em `artifacts/testes/<commit>/<tc-id>/` pelo pipeline `testes-integracao`; o PR referencia cada artefato.
- [x] Yoda aprova FDD-003 antes da implementação — revisão arquitetural de 2026-09-02, FDD-003 v1.2. Patrick Jane audita a entrega; pipeline de review fica verde.
- [ ] Matriz de capacidade por rota (D-C1) implementada e provada por TC-065 a TC-071.
- [ ] Ciclo de vida da execução assíncrona conforme D-C3: isolamento por proprietário, descarte na primeira entrega, expiração em 10 min, limite de 4 pendentes com 429 e falha distinta de 425.
- [ ] Leitura da opção B sem coleção proporcional a 200.000 linhas (D-C4) e partição por `ativo_id` com threads `posicao-worker-<i>` (D-C5).

## Riscos e lacunas
- `❓ LACUNA` **valores do seed `ATIVO0`–`ATIVO127`** — nome, tipo, `dataEmissao`, `dataVencimento` e preço inicial por ativo. Dono: usuário; são dados do enunciado MAPS, não decisão arquitetural. Bloqueia apenas o TC-064; o restante do T-003 pode prosseguir.

## Histórico
- 2026-09-02: A e B tornadas cumulativas e critérios D3 fixados após auditoria.
- 2026-09-02: dependência de governança satisfeita — FDD-003 aprovado por Yoda; acrescentados capacidade por rota, ciclo de vida da execução, restrições de memória/paralelismo e a lacuna de seed.
- 2026-09-03: `status` atualizado para `em-auditoria` — implementação completa, testada (Maven 130/130, `npm run build`, E2E 21/21) e publicada em PR #1 aberto na branch `docs/cadeia-inicial`, aguardando merge. Auditoria independente (Patrick Jane) segue pendente.
