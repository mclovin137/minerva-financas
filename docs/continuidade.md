## Task ativa

Concluir a task de padronização de nomes e correções de evidência, autenticação e acessibilidade no PR #1, mantendo o estado real das verificações e sem declarar conclusão enquanto a suíte completa e o E2E não forem validados.

## Estado atual

Os 11 DTOs foram renomeados com sufixo `DTO`, os três enums com sufixo `Enum` (`TipoAtivoEnum`, `TipoLancamentoEnum` e `TipoMovimentacaoEnum`) e as classes de teste unitário/integração com `Teste`/`IntegracaoTeste`. As referências foram atualizadas e o Surefire passou a incluir `**/*Teste.java` e `**/*IntegracaoTeste.java`.

A identificação de evidências agora coleta todos os `TC-###` do `@DisplayName` e publica a mesma evidência em cada pasta; o caso `TC-078/079/080` passa a cobrir os três IDs. O login suprime a notificação global de expiração no 401 da validação de credencial. O foco inicial do Drawer e o trap ficam restritos aos campos, preservando Tab, Shift+Tab e Escape. O fluxo de saldo aguarda a recarga antes de exibir sucesso, e os ajustes de sincronização/seletores dos cenários E2E foram aplicados.

As verificações de backend passaram: `mvn compile test-compile` e a suíte Maven ficaram verdes com 130 testes. O gate de `docs/lib.md`, as fronteiras de arquitetura e o `npm run build` também passaram. O E2E foi executado contra o jar real em 21 casos, mas não terminou verde: a primeira execução ficou em 16/21 por ausência de `libasound.so.2`; com o ambiente habilitado, ficou em 15/21, com 5 falhas. O fluxo financeiro isolado teve 1/5 na execução inicial ou falhas em cadeia nas tentativas seguintes.

`docs/adrs/adr-003-estrutura-do-backend.md` foi atualizado com as três convenções e a regra do construtor único sem `@Autowired`; `docs/lib.md` registrou `@playwright/test`, `@types/node`, `@types/react` e `@types/react-dom`. O compilador de backend passou em `compile test-compile` (85 fontes de produção e 16 de teste recompiladas). A suíte completa Maven, os gates finais e o E2E permanecem em andamento; não há contagem final nem push declarados nesta atualização.

## Decisões vigentes

DTOs terminam em `DTO`; enums terminam em `Enum`; testes unitários terminam em `Teste` e testes de integração em `IntegracaoTeste`. O callback global de expiração só deve ocorrer após autenticação bem-sucedida. O foco inicial do Drawer entra nos campos, e a evidência de um teste com múltiplos `TC-###` é replicada para todos os casos identificados.

## Riscos e lacunas

Permanece uma lacuna no E2E: há indícios de sincronização/estado persistido entre cenários, e o teste de preço falha quando não há dado visível para a consulta. A execução com jar real confirmou a infraestrutura habilitada, mas não permitiu declarar os 21 casos verdes. A checagem de evidências múltiplas deve continuar sendo acompanhada nas execuções de integração. A sincronização da pendência no Obsidian continua aberta, conforme `docs/pendencias-obsidian.md`.

## Próximo passo

Investigar e estabilizar as lacunas restantes do E2E (sincronização/estado entre cenários e ausência de dado visível no teste de preço), repetir a suíte completa e então criar commit em pt-BR e fazer push da branch `docs/cadeia-inicial`. Ainda não houve commit nem push.

## Região gerada

<!-- minerva-continuity:generated:start -->
Estado gerado para sincronização: renomes de DTOs, enums e testes e correções de evidência, login, foco do Drawer e sincronização de saldo aplicados; ADR-003 e docs/lib.md atualizados; compile/test-compile e Maven com 130 testes, guard-lib, fronteiras e build frontend verdes; E2E real em 21 casos não concluído verde, com lacunas de estado entre cenários e dado visível no teste de preço; sem commit/push.
<!-- minerva-continuity:generated:end -->
