# Playbook: Backend e padrões de sistema

**Autor:** Cristóvão Augusto

> Consulta seletiva. Use o índice gatilho para abrir somente a seção necessária. Este playbook é diagnóstico e decisório. As ADRs e o HLD são prescritivos. Anti-overengineering é o critério dominante.

| # | Tópico | Consultar quando a task envolver… |
|---|--------|-----------------------------------|
| 1 | [Idempotência](#1-idempotência) | webhook, consumer de fila, endpoint de escrita, retry |
| 2 | [Transações distribuídas](#2-transações-distribuídas) | fluxo que toca banco + serviço externo/fila |
| 3 | [Consistência eventual](#3-consistência-eventual) | decidir o que pode atrasar vs. o que é imediato |
| 4 | [Réplicas de leitura](#4-réplicas-de-leitura) | escala de leitura, atraso de replicação |
| 5 | [Teorema CAP](#5-teorema-cap) | comportamento sob falha ou partição |
| 6 | [Processamento efetivamente único](#6-processamento-efetivamente-único) | garantia de entrega em fila ou webhook |
| 7 | [Controle de pressão](#7-controle-de-pressão) | consumidor, trabalhadores concorrentes, pico de tráfego |
| 8 | [Efeito manada](#8-efeito-manada) | falha simultânea de cache, expiração em massa |
| 9 | [Invalidação de cache](#9-invalidação-de-cache) | qualquer escrita em dado cacheado |
| 10 | [Linhas quentes e efeito celebridade](#10-linhas-quentes-e-efeito-celebridade) | contenção concentrada em registro ou partição |
| 11 | [Disjuntor de chamadas](#11-disjuntor-de-chamadas) | chamada síncrona a serviço externo |
| 12 | [Chaves de funcionalidade](#12-chaves-de-funcionalidade) | ligar ou desligar comportamento sem publicação |
| 13 | [Evolução de esquema](#13-evolução-de-esquema) | alteração em estrutura existente |
| 14 | [Preenchimento retroativo](#14-preenchimento-retroativo) | completar dados novos com histórico |
| 15 | [Escritas duplas](#15-escritas-duplas) | escrever em dois destinos |
| 16 | [Tabelas espelho](#16-tabelas-espelho) | mudança estrutural arriscada, migração de dados |
| 17 | [Limitação de taxa](#17-limitação-de-taxa) | ponto público, login, proteção de abuso |
| 18 | [Princípios de invalidação de cache](#18-princípios-de-invalidação-de-cache) | reforço transversal do item 9 |
| 19 | [Inicialização a frio](#19-inicialização-a-frio) | instância hibernada, início da aplicação |
| 20 | [Checklist de arquitetura de sistema](#20-checklist-de-arquitetura-de-sistema) | todo fluxo novo, antes de implementar |
| 21 | [Anti-overengineering](#21-anti-overengineering) | propor padrão, camada, serviço, abstração ou infraestrutura nova |

---

## 1. Idempotência

**Onde aparece**: webhooks de terceiros (gateways de pagamento reenviam eventos até receber 2xx); consumers de fila (entrega at-least-once — redelivery após crash reprocessa a mesma mensagem); endpoints de escrita sob duplo clique/retry do cliente.

**Formas de resolver**
1. **Operação naturalmente idempotente** — desenhar como estado absoluto (`UPDATE ... SET status='x'`, `UPSERT`), não como incremento/append.
2. **Tabela de eventos processados** — `processed_events(event_id UNIQUE)`; inserir o id **na mesma transação** do efeito; violação de unique = já processado, ack e descarta.
3. **Chave de idempotência do cliente** — header `Idempotency-Key` gerado pelo cliente; servidor guarda chave → resposta e devolve a resposta original em repetição.

**Qual usar quando**: webhook e consumer → **(2)**, sempre (defesa canônica contra at-least-once). Endpoint de criação disparado por usuário → **(3)**. Todo o resto → **(1)** por design; é grátis e elimina a classe de problema.

## 2. Transações distribuídas

**Onde aparece**: qualquer fluxo que toque o banco **e** um sistema sem transação compartilhada (gateway de pagamento, e-mail, fila, outra API). Não existe commit atômico entre eles.

**Formas de resolver**
1. **2PC/XA** — commit coordenado. Quase sempre descartar: serviços externos não participam, e 2PC é frágil mesmo onde existe.
2. **Saga** — sequência de transações locais + compensações; orquestrada (um coordenador dirige) ou coreografada (cada passo reage a eventos).
3. **Outbox** (ver §15) — o efeito externo vira linha na mesma transação do dado, publicada depois.

**Qual usar quando**: fluxo com pagamento/confirma-depois → **saga orquestrada simples via máquina de estados** da entidade (`pendente → processando → concluído`, compensação explícita), sem mecanismo genérico de saga. Efeitos secundários pós-commit (e-mail, notificação, indexação) → **outbox**. Coreografia por eventos → só com múltiplos serviços autônomos; num monólito modular, não usar.

## 3. Consistência eventual

**Onde aparece**: decidir, por dado, o que precisa ser imediato (saldo, estoque, status de pagamento — fonte da verdade transacional) e o que pode atrasar segundos/minutos (e-mails, relatórios, contadores, catálogos cacheados).

**Como conviver**: definir explicitamente a janela de staleness aceitável por dado (0 = forte); a UI comunica o assíncrono ("enviaremos por e-mail em instantes") em vez de fingir sincronismo; read-your-writes onde o autor precisa ver a própria escrita (invalidar cache na escrita, §9).

**Regra prática**: dinheiro e recursos disputados → forte, sem exceção. Notificação, relatório, visualização → eventual, **dito no PRD** para o QA testar a janela, não a instantaneidade.

## 4. Réplicas de leitura

**Onde aparece**: quando a leitura domina e um único banco não basta. Em projeto pequeno, **não aplicar** — o passo anterior e suficiente é cache (§9).

**Formas (quando existir)**: réplica assíncrona + roteamento leitura/escrita na camada de dados; mitigar o **replication lag** com read-your-writes (rotear ao primário logo após a escrita do próprio usuário) ou sticky no primário por N segundos pós-escrita.

**Qual usar quando**: só leituras tolerantes a staleness vão à réplica (catálogo, relatórios); escrita crítica e leitura que alimenta decisão de escrita **sempre** no primário.

## 5. Teorema CAP

**A pergunta prática por fluxo**: "se eu não conseguir confirmar no banco, o que faço?" Domínios com dinheiro/recurso disputado são tipicamente **CP**: sob falha/partição, **falhar a operação** (503, tente de novo) em vez de arriscar resposta possivelmente errada (vender duas vezes). Leituras de catálogo podem se comportar como AP (servir do cache).

**Regra**: a postura CP/AP por fluxo é decisão de descoberta/ADR — nunca decidir inline no código. Registrar desvios em ADR.

## 6. Processamento efetivamente único

**A realidade**: exactly-once fim-a-fim **não existe** em sistema distribuído com falhas. O que existe é **at-least-once + processamento idempotente = effectively once**.

**Como aplicar**: no consumer, `ack` **somente após** o efeito persistido; dedup pela tabela de eventos processados (§1.2) na mesma transação. At-most-once (ack antes de processar) só quando perder mensagem for aceitável — raro. Se um PRD pedir "garantir processamento único", traduzir para: at-least-once + dedup por `message_id` + efeito repetido inofensivo.

## 7. Controle de pressão

**Onde aparece**: consumer mais rápido que o recurso downstream (SMTP, API externa); API em pico com mais requests simultâneos que o pool de conexões suporta.

**Formas de resolver**
1. **Prefetch/QoS na fila** — consumer só recebe N mensagens sem ack; a fila É o buffer, e é onde o excesso deve ficar.
2. **Limites explícitos no app** — pool de conexões dimensionado, worker pool/semáforo para trabalho concorrente, timeout de contexto em toda chamada externa.
3. **Rejeitar rápido** — limite estourou → 503/`Retry-After` imediato, em vez de enfileirar sem limite (fila implícita infinita = latência infinita e OOM).

**Qual usar quando**: consumer → **(1)** com prefetch baixo. API síncrona → **(2)** para dimensionar e **(3)** como válvula. Nunca "resolver" pico aumentando buffer sem limite — só move o colapso para mais tarde.

## 8. Efeito manada

**Onde aparece**: chave de cache quente expira e todos os requests vão juntos ao banco recalcular o mesmo valor (cache stampede); app reinicia com cache 100% frio; evento de pico previsível (lançamento, promoção).

**Formas de resolver**
1. **Coalescência de requisições** — N requisições concorrentes pela mesma chave viram uma consulta; as demais esperam o resultado.
2. **TTL com jitter** — TTL base + aleatório, para chaves criadas juntas não expirarem em coro.
3. **Refresh antecipado** — serve o valor velho e renova em background antes de expirar; só para chave comprovadamente quente.

**Qual usar quando**: **(1)** como defesa default no caminho cache-miss→banco; **(2)** sempre que houver famílias de chaves; **(3)** só com métricas mostrando picos na expiração — não implementar preventivamente.

## 9. Invalidação de cache

**Formas de resolver**
1. **TTL puro** — simples, tolera staleness até o TTL.
2. **Delete-on-write** — toda escrita deleta as chaves afetadas; próxima leitura repovoa (cache-aside). Preferir **deletar** a atualizar o valor (atualizar tem race de versão velha vencer a nova).
3. **Versionamento de chave** — `entidade:v{N}`; escrita incrementa N; invalidação em massa barata.

**Qual usar quando**: dado de leitura dominante que muda por ação administrativa → **(1)+(2)** combinados (TTL de segurança com jitter + delete nas escritas). Dado disputado que muda a cada operação (estoque, saldo, reserva) → **não cachear**: fonte da verdade é o banco com travas; cache aí cria exatamente a inconsistência que o domínio proíbe. **(3)** → só quando a invalidação precisar varrer famílias inteiras de chaves.

## 10. Linhas quentes e efeito celebridade

**Onde aparece**: mesmo sem sharding, a versão local existe: **hot rows** — um evento popular concentra todas as transações nas mesmas linhas (o registro do produto em promoção, o contador do evento).

**Formas de resolver**
1. **Granularidade fina de trava** — travar a menor unidade disputada (o item, não o agregado; `SELECT ... FOR UPDATE` nas linhas exatas, em ordem determinística para evitar deadlock).
2. **`FOR UPDATE NOWAIT` / `SKIP LOCKED`** — falhar/pular na hora se já travado, devolvendo "indisponível" em vez de enfileirar espera de lock.
3. **Evitar contadores quentes** — não manter agregado incrementado na linha "pai" (toda operação disputaria essa linha); derivar por `COUNT` ou manter eventual.
4. Sharding/particionamento real — só em escala que o justifique; descartar por default.

**Qual usar quando**: **(1)+(2)** são o desenho de qualquer checkout/reserva concorrente; **(3)** é regra de modelagem desde a primeira migration.

## 11. Disjuntor de chamadas

**Onde aparece**: serviço externo fora do ar (gateway, SMTP, API de terceiro) — sem breaker, cada request espera o timeout inteiro ocupando conexão/thread; falha externa vira lentidão interna.

**Formas de resolver**
1. **Breaker clássico** (fechado → aberto após N falhas → semiaberto para teste) — somente quando o comportamento de falha justificar o estado adicional.
2. **Timeout + retry com backoff exponencial e jitter** — suficiente quando a chamada não está no caminho síncrono do usuário.
3. **Fallback** — definir o que responder com o breaker aberto.

**Qual usar quando**: chamada síncrona com usuário esperando → **(1)** + timeout curto; fallback = erro honesto com estado preservado. Chamada assíncrona (consumer) → **(2)** basta: retry com backoff + DLQ. Banco primário → nem um nem outro: sem banco não há fallback útil; timeout + 503.

## 12. Chaves de funcionalidade

**Formas de resolver**
1. **Config/env no boot** — mudar exige restart; simples, auditável, zero dependência.
2. **Tabela no banco** — mutável em runtime via admin.
3. **Serviço externo** (LaunchDarkly etc.) — targeting, rollout %.

**Qual usar quando**: projeto pequeno → **(1)** como default. **(2)** apenas se alguém precisar alternar sem deploy. **(3)** só com múltiplos usuários de flag e rollout gradual real. Anti-padrão: flag que vive para sempre — toda flag nasce com critério de remoção no PRD.

## 13. Evolução de esquema

**Formas de resolver**
1. **Expandir → migrar → contrair** (o padrão com aplicação em operação): introduzir a estrutura nova sem quebrar a anterior → fazer a transição de leitura e escrita → preencher o histórico (§14) → tornar a regra obrigatória → retirar a estrutura anterior em entrega posterior.
2. Mudança destrutiva num passo (rename/drop direto) — aceitável **somente** sem produção/dados reais.

**Qual usar quando**: antes de dados reais → **(2)** pode ser pragmático, mas com reversão documentada. A partir do primeiro uso com dados reais → **(1)** vira regra; nunca remover ou renomear estrutura ainda usada na mesma entrega que deixa de consumi-la. Em volume relevante, separar cada etapa para limitar impacto.

## 14. Preenchimento retroativo

**Formas de resolver**
1. **Na própria mudança estrutural** — simples, transacional, mas pode reter recursos pela duração.
2. **Em lotes fora da mudança estrutural** — processo idempotente e retomável, com seleção determinística de itens e pausa entre lotes.

**Qual usar quando**: tabela pequena → **(1)**. Tabela grande (lock dói) ou backfill que chama serviço externo (**nunca** chamar API externa dentro de migration) → **(2)**. Regra em ambos: backfill **idempotente** (§1) — re-execução não pode corromper.

## 15. Escritas duplas

**O anti-padrão canônico**: no handler, gravar no banco **e** publicar na fila/cache, um após o outro. Se o processo cai entre os dois: dado sem evento (ou evento de dado que sofreu rollback). Qualquer conforto ("quase nunca falha") é ilusão.

**Formas de resolver**
1. **Outbox** — o evento é persistido na mesma transação do dado; um publicador separado o entrega e registra a conclusão. Falha em qualquer ponto permite retry seguro (o consumidor deduplica, §6).
2. **Captura de mudanças** — a publicação parte do registro de alterações; é infraestrutura pesada e só cabe em escala comprovada.

**Qual usar quando**: **(1)** como decisão de projeto — todo efeito externo disparado por escrita no banco passa pela outbox, sem exceção. Detectar o anti-padrão em revisão: qualquer `publish`/`send` no mesmo fluxo de um `INSERT/UPDATE` fora da outbox é bug de design, não estilo.

## 16. Tabelas espelho

**Formas de resolver**
1. **Shadow table de migração** — cria a tabela nova, escreve nas duas (idealmente via trigger — fica dentro da transação, seguro), compara consistência por um período, corta a leitura, aposenta a velha. É o expand/contract (§13) para reestruturação de tabela inteira.
2. **Shadow/audit table de histórico** — trigger `AFTER UPDATE` copia a versão anterior para `entidade_historico`; trilha de auditoria de mudanças.

**Qual usar quando**: **(1)** só com dados de produção valiosos em jogo — antes disso, migration direta (§13.2). **(2)** quando o requisito for "quem mudou o quê"; alternativa mais simples: tabela de eventos da entidade. Não confundir com dual write de aplicação (§15).

## 17. Limitação de taxa

**Formas de resolver**
1. **Token bucket local** — permite pico controlado em uma única instância.
2. **Janela fixa ou deslizante em contador compartilhado** — necessária com mais de uma instância.
3. **Respeitar o rate limit dos provedores** — tratar 429 externo com backoff, nunca martelar.

**Qual usar quando**: instância única → **(1)**: por IP nos endpoints públicos; por conta+IP no login com janela mais dura (proteção brute-force — exigência de security). Escala horizontal → migrar contadores para **(2)**. Resposta sempre `429` + `Retry-After`; logar excesso (roles.md §6.8) para distinguir ataque de pico legítimo.

## 18. Princípios de invalidação de cache

Reforço transversal do §9, como regras de revisão:

1. **Cache é otimização, nunca fonte de verdade.** Dado que só existe no cache é bug (exceções deliberadas e documentadas: contadores de rate limit).
2. **Todo caminho de escrita conhece suas chaves.** Ao revisar um `UPDATE`: "quais chaves de cache isso invalida?" — resposta "nenhuma" precisa ser demonstrável.
3. **Deletar > atualizar** o cache na escrita.
4. **Invalidação em massa lembra o herd** (§8): mil deletes = mil misses simultâneos; jitter/single-flight lá.
5. **TTL sempre, mesmo com delete-on-write** — backstop contra a chave que o item 2 esqueceu.

## 19. Inicialização a frio

**Onde aparece**: free tiers/serverless hibernam a instância; a primeira request paga boot + pool + TLS + cache frio. Também em todo deploy.

**Formas de resolver**
1. **Aceitar e minimizar** — init enxuto, nada de warm-up pesado bloqueando o boot.
2. **Warming seletivo** — pré-carregar só o essencial, em background, sem bloquear o listen.
3. **Ping externo anti-hibernação** — verificar ToS/custo da plataforma; decisão do usuário, não do dev.
4. **Pool lazy com falha rápida** — 1–2 conexões no boot (detecta banco fora), crescer sob demanda.

**Qual usar quando**: **(1)+(4)** sempre; **(2)** só se a primeira impressão importar em métrica real; **(3)** escalar como pergunta.

## 20. Checklist de arquitetura de sistema

Antes de implementar qualquer fluxo novo, responder por escrito no PRD (ou apontar a seção daqui que responde):

1. **Fonte da verdade** — qual store manda neste dado? (cache nunca — §18.1)
2. **Consistência** — o que precisa ser imediato e o que pode ser eventual? (§3, §5)
3. **Falha externa** — cada chamada a terceiro: o que acontece se falhar, demorar ou **repetir**? (§1, §11)
4. **Atomicidade** — o fluxo escreve em mais de um lugar? Se sim: outbox/saga, nunca dual write (§2, §15)
5. **Hot path** — onde o pico bate? Existe lock grosso ou contador quente escondido? (§8, §10)
6. **Limites** — o que impede este fluxo de consumir recursos sem teto? (§7, §17)
7. **Evolução** — a mudança de schema segue expand/contract? O backfill é idempotente? (§13, §14)
8. **Simplicidade** — qual padrão estou aplicando **sem** cenário que o justifique? Cortar (roles.md §6).

## 21. Anti-overengineering

**Objetivo:** resolver o problema atual com a menor solução que preserve corretude,
segurança, operação e evolução previsível. Simplicidade não é omitir validação, teste,
autorização, observabilidade necessária ou decisão já aprovada; é não antecipar complexidade
sem evidência.

**Antes de adicionar algo**, registrar na task ou no FDD:

1. qual problema observável existe hoje e qual evidência o demonstra;
2. qual alternativa mais simples foi considerada;
3. qual limite mensurável exige a solução proposta; e
4. como a solução poderá ser removida ou simplificada depois.

**Preferências padrão:** monólito modular antes de microsserviços; chamada direta dentro da
fronteira já definida antes de evento; transação local antes de saga; regra de integridade local antes de
cache ou fila; implementação concreta antes de abstração; configuração estática antes de feature flag
dinâmica; métrica antes de otimização de escala. A preferência deixa de valer quando o cenário
concreto apontar necessidade, como falha entre banco e efeito externo (§15), repetição de mensagem
(§1), limite de capacidade (§7) ou migração com dados reais (§13).

**Sinais de alerta:** camada que só repassa chamada, interface com uma única implementação sem
fronteira, feature flag sem plano de remoção, retry sem classificação de erro, cache sem métrica ou
invalidação, fila sem executor comprovado, evento sem consumidor autônomo e dependência criada
para resolver um caso ainda hipotético. Nesses casos, remover a complexidade ou escalar a decisão
para ADR/HLD quando o impacto for estrutural.

**Checklist de revisão:** “o que quebra se eu não criar isto agora?”, “qual dado prova que a
complexidade é necessária?” e “qual a menor versão correta?”. Sem respostas objetivas, não adicionar.

---

# Padrões de aplicação

## A. Arquitetura em camadas

Fonte da verdade: **este playbook** (convenção de projeto, sem ADR — baixo impacto e reversível).

| Camada | Responsabilidade | Regra prática |
|---|---|---|
| **Controller** (`api/`) | recebe a requisição HTTP | decodifica em DTO, valida, chama o Actor, serializa DTO de resposta. Nunca expõe entidade de domínio. |
| **Actor** | orquestra o caso de uso | componente concreto; coordena as chamadas e decide o que deve ser feito. |
| **Resolver** | define o fluxo | componente concreto; resolve dependências e escolhe caminhos. CRUD simples → passthrough **documentado**. |
| **Service** (`service/`) | regra de negócio | lógica principal, testável sem HTTP/DB. |
| **DAO** | acesso a dados e integrações | contrato com implementação real e dublê de teste; consultas isoladas e parametrizadas. |

**Regra anti-ritual:** a camada sempre existe como responsabilidade nomeada, mas só ganha
**interface** quando há segunda implementação ou decisão real. Interface fica reservada a **DAO**
e, para a camada `Service`, a módulos com cenário crítico de QA.

## A.1. DDD tático em módulos críticos

Em módulos críticos, mantenha invariantes junto do domínio: dados inválidos não devem ser
construíveis pelo caminho normal; regras puras não carregam dados nem acionam integrações; e a
orquestração não calcula regra de negócio. A defesa em profundidade só é mantida quando cada barreira
protege uma falha distinta e essa razão está documentada.

**Checklist de revisão:** a fronteira de responsabilidade está clara; o valor de domínio tem
invariante real; o agregado protege suas regras de construção; regras puras recebem dados prontos;
nenhum objeto concentra responsabilidades incompatíveis; e os testes cobrem os cenários críticos.

### Checklist de revisão — capacidades essenciais

Antes de aceitar uma mudança backend, verificar:

1. **Fronteira de responsabilidade clara?** A entrada traduz o contrato externo, a orquestração
   coordena o caso de uso, o domínio concentra regras e invariantes, e a infraestrutura realiza
   persistência ou integração sem absorver regra de negócio.
2. **Value objects têm invariante real?** A construção valida o que precisa ser sempre válido; o
   estado interno não pode ser alterado por caminhos que contornem essa validação.
3. **O agregado protege invariantes por construção?** Há uma única fábrica ou operação coerente para
   estados válidos, os relacionados necessários são protegidos, e o agregado não virou um objeto
   com responsabilidades sem relação.
4. **Domain services são puros quando a regra permitir?** Recebem dados prontos, não carregam
   dependências de infraestrutura e não fazem I/O; quando isso não for possível, a dependência e a
   razão ficam explícitas.
5. **A direção de dependência aponta para dentro?** Interface e domínio não dependem de detalhes de
   transporte, persistência ou framework; adaptadores dependem dos contratos internos.
6. **Gates estão na profundidade correta?** A borda valida forma, a infraestrutura pode filtrar por
   eficiência e o domínio impede estados estruturalmente inválidos. Toda defesa duplicada protege
   uma falha distinta e tem motivo documentado.
7. **A decisão é verificável?** Os testes cobrem invariantes, fluxos de erro, idempotência,
   concorrência e operações irreversíveis aplicáveis ao contrato; a estratégia de teste é a definida
   pela aplicação consumidora, sem pressupor linguagem ou ferramenta.

## B. Contratos de entrada, saída e falha

- **Todo request e response passa por DTO** — nunca serializar ou bindar entidade de domínio direto.
  DTO de entrada tem allowlist explícita de campos, evitando mass assignment; validar tipo, tamanho e
  enum na borda, com zero-trust de input.
- **DAO retorna erros tipados** (`ErrNotFound`, `ErrConflict`), nunca o erro cru do driver. O
  Controller mapeia para status HTTP; erro de banco não vaza na resposta.
- **Códigos HTTP:** `200` para leitura ou atualização; `201` para criação; `204` sem corpo; `400`
  para DTO inválido; `401` sem credencial (gate `X-App-Key`); `403` sem permissão; `404` para recurso
  inexistente; `409` para conflito ou constraint; `422` para semântica inválida; `429` para rate
  limit; `500` para erro interno sem detalhe sensível. Toda listagem tem paginação com limite máximo.

## C. Disciplina de implementação (Object Calisthenics)

Disciplina de legibilidade e encapsulamento, **não um conjunto de dogmas**. Aplique quando reduzir
acoplamento ou tornar invariantes visíveis; não crie wrappers, métodos, tipos ou arquivos que só
aumentem navegação. A seção 21 prevalece.

| Princípio | Aplicação | Evitar |
|---|---|---|
| Baixa indentação | prefira saídas antecipadas e extração de trecho quando o fluxo ficar difícil de seguir | fragmentar um bloco simples apenas para cumprir métrica |
| Caminho feliz explícito | depois de tratar falha ou condição de parada, siga pelo fluxo principal; use alternativa quando ela deixar duas possibilidades simétricas mais claras | duplicar condição ou criar estado temporário apenas para evitar uma alternativa clara |
| Primitivos com significado | crie tipo ou objeto quando houver unidade, semântica de domínio, validação ou fronteira | envolver todo valor básico sem comportamento |
| Coleções com comportamento | dê comportamento à coleção que protege regra, ordenação ou fronteira | criar coleção nomeada sem método ou invariante |
| Navegação legível | quebre cadeias que escondem efeito, erro ou mudança de contexto | tratar todo acesso encadeado simples como violação automática |
| Nomes claros | nomeie pelo papel de negócio; abreviações consagradas em escopo curto são aceitáveis | expandir nomes até perder legibilidade |
| Unidades coesas | mantenha uma intenção por unidade; extraia quando níveis de abstração se misturarem ou o teste ficar difícil | dividir uma unidade coesa só para atingir limite fixo |
| Encapsulamento com propósito | exponha comportamento de negócio e proteja invariantes; estruturas puramente de transporte podem expor dados | acessores mecânicos que apenas repetem um campo |
| Poucas colaborações | injete somente as dependências usadas; agrupe-as quando formarem uma fronteira coesa | impor quantidade fixa de campos ou dependências |

**Revisão:** confirme que a alteração tornou a regra mais explícita, facilitou o teste e não criou
abstração especulativa. Se não, simplifique.

## D. Contrato e documentação da API

- **Todo endpoint documentado** em OpenAPI. A ferramenta de geração da especificação é decidida
  pela aplicação consumidora e registrada no inventário de dependências antes de entrar no build.
- Anti-drift: gerar a especificação no CI e falhar se houver divergência com o arquivo versionado.
- Adotar especificação como fonte da verdade somente se a API abrir para terceiros consumindo esse
  contrato.

## E. Internacionalização das mensagens

- Mensagens originadas pela API — erros de validação e status — usam dicionário JSON próprio por
  locale (`pt-BR.json`, `en.json`, `es.json`), carregado no boot. A interpretação do header
  `Accept-Language` segue BCP47. Sem catálogo internacional completo, por anti-overengineering.
- Texto que o app Flutter compõe fica no i18n do app (`intl`/ARB); não duplicar tradução entre API
  e app.
