## Task ativa

PR #1 concluído nesta rodada: E2E 21/21 verde, hooks `.codex/hooks/` criados e validados, gates finais verdes, commit e push feitos em `docs/cadeia-inicial`.

## Estado atual

Esta rodada é continuação de uma tarefa interrompida sem commit/push. O Severino foi despachado ao Codex (encarnação primária); o turno terminou com `EXIT_CODE_CODEX=0`, mas registrou `error=patch rejected: writing outside of the project; rejected by user approval settings` ao tentar escrever em `.codex/hooks/*.sh`, e o efeito pedido ficou verificavelmente ausente (hooks não criados, sem commit/push) — as duas condições do Caso 2 do fallback (`docs/agentes/severino.md`). **Fallback acionado e anunciado**: a partir daí a execução seguiu por Claude Sonnet, esforço medium.

O Codex já havia confirmado, antes do fallback: `npm run build` verde, empacotamento do jar verde, Maven 130/130 verde, `guard-lib-md.sh` verde, fronteiras DDD verdes. O E2E rodado pelo Codex ficou 0/21 (falha na inicialização do Chromium por ausência de `libasound.so.2`).

Sob o fallback, uma extração local de `libasound2` já existente em `/tmp/minerva-libs/` (baixada sem `sudo`, via `apt-get download` + `dpkg-deb -x`, fora do repositório) foi usada via `LD_LIBRARY_PATH` para destravar o Chromium do Playwright. Com isso, o E2E rodou de verdade: 19/21 na primeira execução, com falha real (não ambiental) em `fluxo-financeiro.spec.ts:21` e `:89` — ambos liam o saldo logo após `lancar()` sem esperar a confirmação (`role="status"`) do lançamento, lendo o valor anterior por corrida, o mesmo padrão que motivou a correção anterior de `useRecurso`. Corrigido em `frontend/e2e/fluxo-financeiro.spec.ts` acrescentando `await expect(page.getByRole('status')).toContainText('Crédito lançado')` nos dois pontos antes de ler o saldo. Reexecutado: **21/21 verde**.

Os três hooks `.codex/hooks/guarda-orquestrador.sh`, `.codex/hooks/sessao-orquestrador.sh` e `.codex/hooks/sincronizar-continuidade.sh` foram criados espelhando `.claude/hooks/`. Única diferença deliberada: `sincronizar-continuidade.sh` não usa `CLAUDE_PROJECT_DIR` (não há variável equivalente documentada no Codex CLI 0.147.0) — a raiz é resolvida por `git rev-parse --show-toplevel`, com o fallback relativo ao script como última rede fora de um repositório git. Smoke test local (replicando `.github/workflows/validar-template.yml`, step "Executar smoke tests dos hooks") passou limpo: os quatro hooks sem stderr, sincronização Claude e Codex idempotentes, invariante de região gerada preservada, marcadores únicos.

Verificação final: Maven isolado 130/130 (mesma contagem de antes); `npm run build` verde; `npm run e2e` 21/21; smoke test dos hooks limpo e idempotente; `guard-lib-md.sh` verde; fronteiras DDD e ausência de `double`/`float` verdes; links de `docs/*.md` íntegros.

## Decisões vigentes

DTOs terminam em `DTO`; enums terminam em `Enum`; testes unitários terminam em `Teste` e testes de integração em `IntegracaoTeste`. O callback global de expiração só deve ocorrer após autenticação bem-sucedida. O foco inicial do Drawer entra nos campos, e a evidência de um teste com múltiplos `TC-###` é replicada para todos os casos identificados. Todo teste E2E que lê um dado alterado por uma ação assíncrona deve esperar a confirmação (`role="status"` ou equivalente) antes de ler esse dado — não basta que `useRecurso` resolva corretamente, o teste também precisa aguardar o sinal de conclusão.

## Riscos e lacunas

`.codex/hooks/*.sh` não está referenciado em `.codex/hooks.json` (que mantém os três hooks como `null` deliberadamente — o Codex CLI não tem o mesmo mecanismo de wiring do Claude Code); os arquivos existem para paridade, smoke test e uso manual/futuro, não para disparo automático. A extração local de `libasound2` em `/tmp/minerva-libs/` é um workaround de sessão (fora do repositório, não versionado, perdido ao limpar `/tmp`); o ambiente de CI/outra máquina pode voltar a bloquear o Chromium sem essa dependência de sistema — não é uma correção permanente do ambiente. A sincronização da pendência no Obsidian continua aberta, conforme `docs/pendencias-obsidian.md`.

## Próximo passo

Nenhum bloqueio remanescente nesta rodada. Push feito para `docs/cadeia-inicial`; PR #1 segue aguardando revisão independente (auditoria) — Severino não aprova nem faz merge do próprio PR. Se o `libasound.so.2` voltar a faltar em execução futura, repetir o workaround de `/tmp/minerva-libs/` ou instalar a dependência de forma permanente no ambiente.

## Região gerada

<!-- minerva-continuity:generated:start -->
<!-- minerva-continuity:generated:end -->
