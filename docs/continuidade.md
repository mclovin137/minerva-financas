## Task ativa

Consolidar a entrega dos níveis 1-3 do desafio MAPS (opções A e B) sobre a estrutura ADR-003, incluindo a correção do contrato da opção B (posição assíncrona diretamente em `/posicao` e `/posicao/{id}`, removendo `/posicao/assinc`), a limpeza de código morto associada (`ConsultarPosicaoActor`, `PosicaoService`, `PosicaoActorFactory.sincrono()`), e abrir o PR de `docs/cadeia-inicial` para `main`, com o deploy em nuvem explicitamente adiado por decisão do usuário.

## Estado atual

Implementação, testes e auditorias concluídos nesta sessão; commit, push e abertura de PR feitos agora, por decisão explícita do usuário de fechar esta entrega sem aguardar o deploy em nuvem. O controller público único (`PosicaoController`) inicia execuções em `GET /posicao?data=...` com 202, entrega resultado em `GET /posicao/{id}` com 425/200 e descarta a execução após a entrega. O frontend faz polling (`consultarPosicao` em `frontend/src/api/cliente.ts`) com estado de carregamento nas telas que consomem posição. Testes de posição, segurança e filtros de data foram adaptados ao novo contrato.

Nesta limpeza subsequente, confirmou-se que `ConsultarPosicaoActor` e `PosicaoService` não tinham nenhum chamador real (nem produção, nem teste) além de si mesmos — eram código morto remanescente da migração, não um caso de uso interno intencional. Ambas as classes foram removidas, junto com o método `sincrono()` e o campo `ConsultarPosicaoActor` de `PosicaoActorFactory`, cujo construtor passou a receber apenas `SolicitarPosicaoAssincronaActor` e `ConsultarPosicaoAssincronaActor`. `PosicaoBuilder` e `PosicaoResposta` foram conferidos e permanecem em uso vivo por `ConsultarPosicaoAssincronaActor`, portanto não são órfãos e não foram tocados.

Evidências executadas de fato nesta sessão:
- `./mvnw -f backend/pom.xml test`: BUILD SUCCESS, 126 testes, 0 falhas, 0 erros (125 antes de acrescentar o teste explícito do achado não bloqueante b; +1 depois; a remoção do código morto não alterou a contagem).
- `npm run build` em `frontend/`: TypeScript (`tsc -b`) e Vite verdes.
- TC-078/079/080 (`PosicaoDesempenhoIT`): 200.000 movimentações processadas, variação de heap de 14,6 MB (limite 64 MB), 4 threads de agregação.
- Busca no código não encontrou `/posicao/assinc` nem `PosicaoAssincronaController.java` em fontes versionadas (apenas em `backend/target/`, artefato de build).
- Gate manual de continuidade (`.github/scripts/verificar-continuidade.py`) reprovou o formato anterior deste arquivo (seção `## Evidências` fora do conjunto declarado e ausência de `## Região gerada`); corrigido e reexecutado com `APROVADO`.
- Gate manual de fronteiras (greps equivalentes a `.github/workflows/review.yml`: direção de dependência, ausência de pacote legado, prefixo `I` em interface, ausência de `double`/`float`, links relativos de `docs/`, presença de `README.md`) reexecutado após a correção — todos OK.
- `grep -rn "ConsultarPosicaoActor\|PosicaoService\b" backend/src` após a remoção: sem ocorrências.

## Decisões vigentes

Não há mais caminho síncrono de posição, nem exposto via REST nem interno: `ConsultarPosicaoActor` e `PosicaoService` eram código morto e foram removidos nesta limpeza, não uma decisão de desempenho deliberada de manter dois caminhos. O worker assíncrono (`PosicaoAssincronaService`) mantém agregação particionada e streaming próprios, dimensionados para a meta de alto volume — isso segue vigente, mas por não haver mais nenhum caminho síncrono para reaproveitar, não por escolha de não reutilizá-lo. `PosicaoActorFactory` agora expõe apenas `solicitacao()` e `consulta()`. Não há rota pública síncrona nem `/posicao/assinc`.

## Riscos e lacunas

Deploy em nuvem continua `❓ LACUNA`, adiado por decisão explícita do usuário nesta rodada, sem prazo nem plataforma fixados; ver seção Deploy do `README.md`. Permanecem as lacunas de dados exatos do seed MAPS e de estados visuais já documentadas em `docs/design`. A sincronização da cópia canônica no Obsidian segue pendência existente, a registrar em `docs/pendencias-obsidian.md` dentro do prazo da regra de ferro 3.

## Próximo passo

Sincronizar a pendência Obsidian e encaminhar o PR para auditoria independente; deploy em nuvem fica para decisão futura do usuário, fora desta entrega.

## Região gerada

<!-- minerva-continuity:generated:start -->
Estado gerado para sincronização: entrega dos níveis 1-3 do desafio MAPS (opções A e B) sobre a estrutura ADR-003 consolidada, com a correção do contrato assíncrono da posição (Opção B) e a remoção do código morto do caminho síncrono (`ConsultarPosicaoActor`, `PosicaoService`); commit, push e PR abertos nesta sessão; deploy em nuvem adiado por decisão do usuário (❓ LACUNA), aguardando sincronização Obsidian.
<!-- minerva-continuity:generated:end -->
