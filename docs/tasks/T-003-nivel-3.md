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

# T-003: Entregar contratos e operação do nível 3

## Para o futuro agente
Fechar os endpoints N3 e uma opção operacional explicitamente escolhida, sem ampliar o escopo para deploy cloud efetivo.

## Origem
- Caminho: fluxo completo, com contrato público, segurança, concorrência e operação.
- PRD: PRD-001, FR-008 a FR-010 e requisitos não funcionais N3.
- FDD aprovado: FDD-003, aprovação de Yoda pendente.
- HLD: HLD-001.
- ADRs aplicáveis: ADR-001 e ADR-002; decisão A/B ainda é lacuna.

## Resumo decisório mínimo
- Objetivo: contratos exatos, seed, Docker e opção A ou B.
- Decisão: cloud adiado; A/B não escolhida no briefing e bloqueia o caminho correspondente.
- Evidências: contrato REST, segurança ou carga/memória/paralelismo.
- Riscos e lacunas: escolha da opção e metas quantitativas TBD.
- Próximo passo: usuário decidir A/B; Yoda aprovar FDD-003.

## Responsáveis
- Planejamento: Yoda
- Implementação: Severino
- Auditoria: Patrick Jane

## Resultado esperado
Nível 3 executável standalone, com Docker e comprovação da opção escolhida.

## Escopo
### Incluído
- Cinco rotas síncronas exatas, seed ATIVO0..ATIVO127, Dockerfile/docker-compose e A ou B.

### Excluído
- Deploy cloud efetivo e qualquer tela ainda ❓ LACUNA.

## Dependências e bloqueios
- T-002 concluída; escolha A/B; contratos e metas aprovados.

## Passos de implementação
1. Congelar contrato e opção escolhida.
2. Implementar operação, concorrência e Docker.
3. Executar testes de contrato, segurança ou memória/carga.

## Critérios de aceite
- [ ] Rotas, payloads, seed e saldo JSON são literais.
- [ ] Opção A prova Basic, isolamento e capacidades; ou B prova id/425/200, descarte, memória constante e paralelo.
- [ ] Concorrência não corrompe dados; Docker executa standalone.

## Testes obrigatórios
- [ ] Integração reexecutável de cada endpoint, com fixture isolada e limpeza, rollback ou reset; sem alterar payload/endpoint para adicionar idempotência semântica.
- [ ] Concorrência e teste específico da opção A ou B escolhida.

## Evidências obrigatórias
- [ ] Evidência de requests/responses e seed.
- [ ] Imagem e/ou vídeo dos testes de endpoint publicado pelo pipeline, quando aplicável.

## Documentação e Obsidian
- Arquivos `.md`: FDD-003, PRD-001, HLD-001 e ADRs.
- Notas Obsidian: `Minerva/tasks/t-003-nivel-3.md` e notas relacionadas pendentes.

## Riscos e lacunas
- ❓ LACUNA: escolha entre A/B e metas de desempenho; dono usuário/Yoda; bloqueia implementação correspondente.

## Definição de pronto
- [ ] Implementação respeita cadeia aprovada.
- [ ] Testes, segurança/carga e evidências produzidos.
- [ ] Notas e documentação atualizadas.
- [ ] Auditoria por Patrick Jane e pipelines verdes.

## Histórico
- 2026-09-02: task criada com status proposta.
