# Análise e evolução estrutural do Neo

**Autor:** Cristóvão Augusto

**Data da análise:** 2026-08-25

**Baseline:** `origin/main` em `adedfd01bd9dcf9b9ee6025f48ee22b2ca407ce9`

## Escopo e método

Foram lidos o agente canônico e seu adapter, os catálogos e regras de roteamento, skills de
segurança, playbooks relacionados, papéis, agentes vizinhos, continuidade, configuração de hooks,
ADRs e tasks que citam o Neo. O baseline foi comparado com a arquitetura do CAI apenas em fontes
primárias do repositório oficial. Nenhuma decisão foi preservada apenas por compatibilidade.

Fluxo reconstruído no baseline:

```text
PRD/HLD/FDD/diff/configuração
  → Oráculo aciona Neo por gatilho de segurança
  → Neo aplica conhecimento adversarial embutido no próprio documento
  → consulta playbooks de security/backend/database
  → especifica TCs ou emite parecer/achado
  → Severino corrige ou Yoda registra decisão
  → Neo valida evidência de correção
```

O fluxo descrevia bem revisão de segurança, mas não possuía ciclo operacional explícito
`observação → hipótese → teste → evidência → adaptação`, inventário de capacidades diagnósticas,
estado de investigação, contrato de handoff nem benchmark de localização por camada.

# 1. Estado atual do Neo no baseline

## Arquitetura e responsabilidade

O Neo era um subagente de responsabilidade `planejar/revisar`, definido canonicamente em
`docs/agentes/neo.md` e descoberto pelo adapter `.claude/agents/neo.md`. Oráculo o acionava para
segredo, autenticação, permissão e dependência vulnerável. O catálogo e `docs/rules.md` o nomeavam
“Segurança” e limitavam sua descrição a auditoria/testes de segurança.

Sua responsabilidade prática era:

- revisar FDD, endpoints, autenticação, autorização, input/output, injeção e pipeline;
- analisar dependências;
- criar TCs negativos na suíte compartilhada;
- emitir parecer, achados e exigências;
- validar correção, sem implementá-la;
- encaminhar decisão estrutural de segurança ao Yoda.

## Conhecimento embutido no prompt

O documento concentrava identidade, método e referência em um único texto. O conhecimento permanente
era majoritariamente AppSec: cliente hostil, validação server-side, allowlist, classes de injeção,
acesso horizontal/vertical, credenciais, dependências e esteira.

Também continha conhecimento permanente de domínio acadêmico: proteção prioritária de nota,
atividade e matrícula; exemplos de aluno, professor e turma; pendências sobre integrações com dados
de alunos.

## Capacidades efetivamente disponíveis

- Leitura dos artefatos do repositório e das evidências entregues.
- Consulta aos playbooks `security`, `backend` e `database`.
- Uso das ferramentas genéricas oferecidas pelo harness, sem contrato de capacidade próprio.
- Produção de documentos: TCs, achados e parecer.

O adapter declarava modelo/esforço e apontava ao canônico; não declarava ferramentas. Hooks estavam
explicitamente inativos em `.claude/settings.json` e `.codex/hooks.json`. Portanto, nenhuma
observação, captura, correlação, memória ou iteração era garantida operacionalmente pela arquitetura.

## Memória, handoffs e agentes relacionados

`docs/continuidade.md` fornecia contexto compartilhado do projeto, não estado de investigação. O
Neo não possuía Hypothesis Ledger, TRACE ou Decision Log. O único handoff explícito era propor ADR
ao Yoda quando segurança alterasse uma decisão técnica; correção por implementador era fronteira
textual, sem payload mínimo transferível.

Fronteiras relacionadas já existentes e preservadas:

- Severino implementa código/configuração versionada e testes;
- Jarvis opera ambiente, deploy, rollback e incidente;
- Yoda decide estrutura e tecnologia;
- Patrick Jane define e valida comportamento/cobertura funcional;
- Oráculo roteia e mantém estado global.

# 2. Problemas identificados

As referências de baseline abaixo usam arquivo e linha no commit `adedfd0`; o conteúdo novo não é
usado como prova retrospectiva.

| ID | Problema | Evidência no baseline | Efeito |
|---|---|---|---|
| P01 | identidade reduzida a AppSec | `docs/agentes/neo.md:14` — “Segurança de aplicações cliente-servidor” | falhas sistêmicas não acionavam o Neo |
| P02 | identidade acoplada ao domínio acadêmico | `docs/agentes/neo.md:7,30-32,63,131` — alunos, nota, atividade, matrícula, professor/turma | contexto de uma aplicação virou conhecimento permanente |
| P03 | identidade acoplada a fornecedores de IA | `docs/agentes/neo.md:18-28` continha Claude/Codex/modelos | definição canônica contrariava separação por adapter |
| P04 | agente, prompt, método e referência misturados | todo `docs/agentes/neo.md`, especialmente `34-95` | evolução de procedimento exigia reescrever identidade |
| P05 | sem modelo operacional de investigação | `docs/agentes/neo.md:45-56` lista revisões, mas não PLAN/ACT/OBSERVE/ADAPT | plausibilidade podia substituir teste |
| P06 | sem decomposição por camada | nenhuma sequência processo→socket→rede→TLS→HTTP no baseline | tendência a saltar para framework/segurança/aplicação |
| P07 | sockets e SO ausentes | nenhuma ocorrência conceitual de `bind`, `listen`, `accept`, estado TCP ou namespace | incapaz de explicar “porta 8080” abaixo do framework |
| P08 | DNS/TCP/TLS/HTTP não eram separáveis pelo contrato | conhecimento concentrado em segurança de endpoint (`58-87`) | diagnóstico podia confundir camadas sucessivas |
| P09 | ferramentas sem abstração de capacidade | adapter só apontava ao canônico; nenhum inventário de sensors/adapters | texto não garantia observabilidade real |
| P10 | sem autoridade operacional graduada | baseline dizia revisar/reportar, mas não delimitava observação, teste e mutação | risco de inércia ou ação além da responsabilidade |
| P11 | evidência sem semântica negativa | `docs/agentes/neo.md:55,85-87` exige evidência, mas não “o que não prova” | conclusões extrapolavam o ponto de observação |
| P12 | memória operacional inexistente | não havia ledger, TRACE nem Decision Log | repetição circular e perda de hipóteses eliminadas |
| P13 | handoff não estruturado | `docs/agentes/neo.md:54,99-103` cita Yoda/implementador sem contrato | destinatário precisava reiniciar investigação |
| P14 | roteamento restrito a segurança | `docs/agentes/oraculo.md:61-67`; `docs/rules.md:339-344` | incidentes de rede iam direto a Jarvis sem investigador independente |
| P15 | security-review preso a entidades acadêmicas | `docs/skills/security-review.md:9,15-17,30` | gate não era reutilizável fora do produto atual |
| P16 | segurança e diagnóstico não tinham composição | playbook de security obrigatório, mas nenhuma skill diagnóstica | ampliar o Neo arriscava apagar o gate anterior |
| P17 | sem benchmark do próprio agente | nenhum cenário/rubrica/hard-fail para Neo | não havia prova de melhoria nem proteção contra regressão |
| P18 | hooks não implementam capacidade | `.claude/settings.json` e `.codex/hooks.json` têm mapas vazios | capacidades existiam apenas quando o harness as fornecia |
| P19 | ausência de estratégia cross-platform | ferramentas concretas eram `TBD` (`neo.md:129-131`) sem adapters equivalentes | método podia se tornar Linux-only |

## Capacidade textual versus operacional

| Alegação anterior | Capacidade operacional correspondente | Estado no baseline |
|---|---|---|
| “audita o sistema” | inspeção de processo/socket/rota/tráfego | não contratada |
| “evidência reproduzível” | schema com parâmetros, timestamp, exit code e referência | parcial, apenas texto |
| “valida correção” | teste pós-ação ligado à hipótese | não definido |
| “analisa cliente-servidor” | separar DNS/TCP/TLS/HTTP e as duas pontas | ausente |
| “propõe plano de incidente” | estado iterativo e handoff para operação | sem payload/ledger |

# 3. Lições aplicáveis do CAI

## O que incorporar

1. **Agente definido por instruções, ferramentas e handoffs.** O tipo `Agent` do CAI separa esses
   campos e permite instruções dinâmicas. Para o Neo, isso sustenta a separação entre identidade,
   prompt, sensors/adapters e destinos, sem copiar o runtime Python. Fonte:
   [`agent.py` L56–L163 no snapshot](https://github.com/aliasrobotics/cai/blob/6dc79257777f5f1c9500b4d2319935d34a47412e/src/cai/sdk/agents/agent.py#L56-L163).
2. **Handoff com input e filtro de contexto.** A implementação do CAI modela histórico anterior,
   itens pré-handoff e itens novos, além de filtro. Localmente, o valor é transferir um pacote mínimo
   de fatos, evidências, desconhecidos e critério de validação, sem vazar secrets ou histórico
   irrelevante. Fonte:
   [`handoffs.py` L29–L99](https://github.com/aliasrobotics/cai/blob/6dc79257777f5f1c9500b4d2319935d34a47412e/src/cai/sdk/agents/handoffs.py#L29-L99).
3. **Especialização por capacidade observável.** O agente de análise de tráfego do CAI combina
   instruções próprias, ferramentas e handoffs para uma capacidade delimitada. Neo absorve a ideia
   de capability, não o domínio cyber nem as ferramentas específicas. Fonte:
   [`network_traffic_analyzer.py` L54–L88](https://github.com/aliasrobotics/cai/blob/6dc79257777f5f1c9500b4d2319935d34a47412e/src/cai/agents/network_traffic_analyzer.py#L54-L88).
4. **Prompt com disciplina de execução e evidência.** O template de sistema do CAI explicita uso de
   ferramentas, iteração e observação antes de resposta. O prompt local restringe isso a A0/A1 e
   acrescenta `Prova/Não prova`. Fonte:
   [`system_master_template.md` L59–L110](https://github.com/aliasrobotics/cai/blob/6dc79257777f5f1c9500b4d2319935d34a47412e/src/cai/prompts/core/system_master_template.md#L59-L110).
5. **Conhecimento especializado carregável.** O micro-prompt de rede mostra conhecimento separado
   do objeto agente. A proposta local usa um playbook neutro em vez de embutir uma enciclopédia no
   system prompt. Fonte:
   [`micro/network.md` L1–L25](https://github.com/aliasrobotics/cai/blob/6dc79257777f5f1c9500b4d2319935d34a47412e/src/cai/prompts/micro/network.md#L1-L25).
6. **Especificação de avaliação como parte da arquitetura.** Agente técnico precisa ser medido por
   desempenho em tarefas e segurança operacional, não por completude retórica. A especificação
   local separa corpus público, oracle privado e contrato do harness, com repetição, rubrica e
   hard-fails. Ela ainda não constitui uma execução: M0 não foi rodado e M1 depende das fixtures e
   wrappers P2.

## O que não copiar

- domínio de cybersecurity, pentest, exploração, CTF, reconhecimento ofensivo ou nomenclatura de
  red team;
- ferramentas ofensivas ou permissões amplas como default;
- código, runtime Python, SDK, dependências, providers, telemetria ou topologia do CAI;
- swarm/concorrência apenas porque o framework suporta;
- memória longa de detalhes de alvos/aplicações;
- tracing que exponha raciocínio privado, segredo ou payload sensível;
- quantidade de agentes sem benefício: especialização só entra quando muda responsabilidade,
  autoridade ou evidência necessária.

O repositório CAI consultado está **arquivado e sem manutenção**. O snapshot fixo
`6dc79257777f5f1c9500b4d2319935d34a47412e` serve apenas como evidência histórica de padrões; CAI
não é dependência, upstream operacional nem fonte a acompanhar automaticamente.

**Ressalva:** TRACE nesta proposta é uma adaptação metodológica local. Não se afirma que o schema
`Context/Hypothesis/...` seja formato nativo do CAI; ele combina a rastreabilidade de execução
observada no CAI com as exigências explícitas desta evolução.

# 4. Nova definição conceitual do Neo

## Missão

Ser o **Systems & Network Investigation Agent** do projeto: explicar e testar o caminho dos bytes
entre processos e localizar a primeira transição que diverge do fluxo esperado.

## Responsabilidades

- reconstruir fluxo top-down, bottom-up ou meet-in-the-middle;
- formular hipóteses falsificáveis e escolher testes discriminatórios;
- executar A0/A1 no escopo autorizado;
- separar processo, socket, kernel, rede, DNS, transporte, TLS, protocolo e aplicação;
- registrar fatos, inferências, hipóteses, desconhecidos e conclusões;
- preservar `Prova/Não prova`, TRACE e Decision Log;
- produzir handoff suficiente para correção, mitigação, arquitetura ou QA;
- aplicar segurança como capacidade contextual sem abandonar gates anteriores.

## Limites

- A0/A1: Neo investiga e prova.
- A2: Jarvis muda/mitiga ambiente e opera deploy/rollback/incidente.
- A3: Severino corrige código/configuração versionada e implementa testes.
- Yoda decide estrutura, fronteira e tecnologia.
- Patrick Jane valida comportamento funcional e cobertura.
- Oráculo roteia, preserva estado global e pede decisões ao usuário.

Neo não é especialista permanente em domínio acadêmico, linguagem, framework, cloud, banco,
runtime, container ou ferramenta. Também não é agente de pentest.

## Competências permanentes versus contexto

| Permanente | Temporário por investigação |
|---|---|
| processos, SO, sockets, redes e protocolos | entidades e regras do negócio |
| decomposição por camadas | topologia e tecnologia da aplicação |
| TRACE, hipóteses e evidência | nomes, IPs, portas e credenciais autorizadas |
| capacidades de observação | adapter concretamente disponível |
| segurança de fronteiras | criticidade definida pelo domínio |

# 5. Gap analysis

```text
NEO ATUAL (baseline)
revisor AppSec acoplado a dados acadêmicos, conhecimento monolítico e evidência genérica
        ↓
GAPS
sem investigação por camada, operações A0/A1, ferramentas por capacidade, TRACE, handoff e benchmark
        ↓
NEO DESEJADO
investigador agnóstico de sistemas/redes, evidence-first, com segurança contextual e fronteiras claras
```

| Categoria | Atual no baseline | Gap | Desejado / artefato |
|---|---|---|---|
| identidade | segurança da aplicação | estreita e domain-bound | missão systems/network em `agentes/neo.md` |
| system prompt | misturado ao agente | acoplamento e duplicação | prompt operacional separado e conciso |
| conhecimento | AppSec e entidades acadêmicas | faltam fundamentos abaixo da stack | playbook de SO, rede e protocolos |
| raciocínio | checklist de revisão | sem hipóteses adaptativas | ciclo TRACE na skill |
| metodologia | evidência pedida genericamente | sem discriminação/stop condition | ledger, layer map e Decision Log |
| observabilidade | depende do harness | sem contrato de ponto/tempo | schema de evidência |
| ferramentas | ferramentas concretas `TBD` | sem capability model | matriz capacidade→adapters |
| debugging | não era responsabilidade principal | sem localização de camada | top-down/bottom-up/meet-in-middle |
| networking | superficial | sem rota/NAT/MTU/IPv6 | playbook §§3–6 |
| sistemas operacionais | ausente | sem processo/fd/kernel | playbook §§1–3 |
| HTTP | somente segurança de endpoint | sem versões/proxy/stream | playbook §8 |
| DNS | ausente | confusão nome×alcance | playbook §6 e cenário B |
| sockets | ausente | “porta” abstrata | playbook §2 e cenários A/J |
| infraestrutura | Jarvis como único destino | diagnóstico e mutação misturados | Neo prova; Jarvis muda |
| evidências | reproduzível sem schema | extrapolação provável | TRACE + Prova/Não prova |
| memória | continuidade global | investigação circular | ledger/log temporários |
| handoffs | menção textual | perda de contexto | contrato tipado em markdown |
| testes | TCs de segurança | não mede diagnóstico | benchmark A–J |
| documentação | único arquivo monolítico | manutenção e leitura caras | agente/prompt/skill/playbook/contrato separados |

# 6. Arquitetura proposta

```text
Application Context (temporário)
        ↓
Neo — identidade, missão, limites A0/A1
        ↓ carrega
Prompt operacional — disciplina mínima do turno
        ↓ aplica
Skill TRACE — método e estado da investigação
        ↓ consulta seletivamente
Playbook — fundamentos + capacidades + adapters
        ↓ usa
Sensors/adapters disponíveis no ambiente
        ↓ produz
Contrato de evidência — TRACE + Ledger + Decision Log
        ↓
Conclusão ou handoff estruturado
 ├─ Jarvis (A2 ambiente)
 ├─ Severino (A3 versionado)
 ├─ Yoda (decisão)
 └─ Patrick (comportamento/prova)
```

| Elemento | Deve conter | Não deve conter |
|---|---|---|
| Prompt | missão, ciclo, linguagem epistêmica, autoridade, stop/handoff | enciclopédia, tool flags, domínio |
| Skill | procedimento TRACE, state objects, critérios de teste/encerramento | ferramenta obrigatória ou causa pré-assumida |
| Tool/adapter | uma capacidade observável, parâmetros e output íntegro | decisão causal ou regra de negócio |
| Hook | nada ativo nesta evolução | investigação automática ou mutação silenciosa |
| Biblioteca/contrato | schema de evidência, correlação e handoff | topologia específica |
| Agente Neo | identidade, competências, limites e roteamento | configuração de fornecedor/modelo |
| Handoff | fatos, evidências, desconhecidos, ação e validação | histórico bruto irrelevante ou segredo |
| Memória | ledger/log temporário e padrões redigidos governados | IPs, nomes, entidades e secrets permanentes |

Não se propõe novo subagente nesta onda: SO, rede, DNS, TLS e HTTP compartilham o mesmo objetivo e
mesma autoridade investigativa. Especialista separado só se justifica se surgir ferramenta/
privilégio próprio, volume de contexto que degrade o benchmark ou responsabilidade diferente.

# 7. Backlog priorizado

## Levantamento de melhorias

| ID | Problema atual | Evidência | Melhoria | Motivo | Arquitetura | Impacto | Esforço | Prioridade | Dependências | Critério de aceite |
|---|---|---|---|---|---|---|---|---|---|---|
| NEO-001 | identidade AppSec/domain-bound | P01–P03 | redefinir missão agnóstica e mover modelo ao adapter | libera investigação universal sem perder nome | Agent | Alto | Médio | P0 | decisão do usuário | canônico não contém entidade de negócio, stack ou fornecedor |
| NEO-002 | prompt monolítico | P04 | separar system prompt operacional | reduz duplicação e permite adaptar execução | Prompt | Alto | Baixo | P0 | NEO-001 | adapter aponta agente+prompt; prompt passa revisão de não duplicação |
| NEO-003 | contexto permanente mistura aplicação | P02 | criar contrato de Application Context temporário | domínio entra só quando necessário | Core/Prompt | Alto | Baixo | P0 | NEO-001 | cenário de outro domínio roda sem alterar identidade |
| NEO-004 | não há níveis de autoridade | P10 | instituir A0/A1/A2/A3 e donos | permite testar sem usurpar correção/operação | Agent/Core | Alto | Baixo | P0 | fronteiras vigentes | benchmark reprova mutação; handoffs chegam ao dono certo |
| NEO-005 | sem ciclo investigativo | P05–P06 | skill `investigar-sistemas-redes` com TRACE | transforma explicação em investigação iterativa | Skill | Alto | Médio | P0 | NEO-001/004 | todo cenário gera hipótese→teste→observação→adaptação |
| NEO-006 | evidência extrapolada | P11 | contrato epistêmico e `Prova/Não prova` | limita conclusão ao ponto observado | Biblioteca/Contrato | Alto | Médio | P0 | NEO-005 | 100% dos TRACE importantes têm metadata e limites |
| NEO-007 | security-review acadêmica | P15–P16 | generalizar ativos/impacto e manter gate contextual | preserva segurança sem identidade de negócio | Skill | Alto | Baixo | NEO-001 | busca não encontra entidades acadêmicas permanentes na skill |
| NEO-008 | fundamentos ausentes | P07–P08 | playbook de SO/sockets/rede/DNS/TLS/HTTP/infra | base técnica comum abaixo de stacks | Playbook | Alto | Alto | P1 | NEO-001 | cobre tópicos obrigatórios e é acionável por sintoma |
| NEO-009 | tool-first/TBD | P09/P19 | mapa capacidade→adapters Linux/Windows/macOS/infra | ferramenta fica intercambiável | Tool/Playbook | Alto | Médio | P1 | NEO-008 | cada capacidade crítica tem ≥2 famílias de adapter ou lacuna explícita |
| NEO-010 | handoff perde contexto | P13 | pacote de handoff por destino | evita reinício e separa investigação/correção | Handoff/Contrato | Alto | Médio | P1 | NEO-006 | destinatário continua sem repetir teste já concluído |
| NEO-011 | roteamento só segurança | P14 | ampliar gatilho no Oráculo/rules/catálogos | incidentes técnicos chegam ao investigador | Core/Agent | Alto | Baixo | P1 | NEO-001/004 | pedido “frontend não acessa backend” aciona Neo antes de mutação |
| NEO-012 | sem teste do agente | P17 | especificar avaliação A–J, rubrica, isolamento de oracle e hard-fails | define como medir processo sem resposta decorada | Benchmark | Alto | Alto | P1 | NEO-005/006/010 | especificação separa corpus/oracle/harness e impede oracle no contexto do Neo |
| NEO-013 | adapters dependem do harness | P18 | implementar wrappers estruturados para capacidades A0/A1 | garante outputs consistentes | Tool | Alto | Alto | P2 | NEO-009, decisão por plataforma | schema inclui versão, exit/status, ponto e timestamps |
| NEO-014 | benchmark documental | P17 | criar fixtures isoladas A–J cross-platform viáveis | prova uso operacional real | Tool/Test | Alto | Alto | P2 | NEO-012/013 | setup/teardown idempotentes e hash de fixture |
| NEO-015 | packet analysis raso | P07/P08 | capability de captura/correlação bidirecional limitada | diagnostica retransmissão, MTU e handshake | Tool/Skill | Médio | Alto | P2 | NEO-006/009, privilégio | cenários C/I passam sem captura excessiva |
| NEO-016 | estado manual pode divergir | P12 | helper local para validar schema TRACE/ledger/log | evita campos ausentes e circularidade | Biblioteca/Tool | Médio | Médio | P3 | NEO-006 | lint falha em TRACE incompleto sem interpretar causalidade |
| NEO-017 | padrões não retroalimentam método | P12 | promoção governada de padrões redigidos ao playbook | aprende sem memorizar aplicação | Hook/Core | Médio | Alto | P3 | NEO-014/016, revisão humana | nenhum dado específico; ganho medido no benchmark |
| NEO-018 | investigação multi-ponta manual | P09/P12 | correlação controlada cliente/servidor por ids e relógios | reduz trecho desconhecido em sistemas distribuídos | Tool/Core | Alto | Alto | P3 | NEO-013/015 | evidências de duas pontas correlacionadas com incerteza temporal |

## Ondas e dependências

```text
P0 Fundamentos
NEO-001 → NEO-002/003/004 → NEO-005 → NEO-006
                          └→ NEO-007

P1 Investigação
NEO-005/006 → NEO-008/009/010/011 → NEO-012

P2 Especialização profunda
NEO-009/012 → NEO-013 → NEO-014/015

P3 Autonomia avançada
NEO-006/013/014 → NEO-016/017/018
```

Esta entrega materializa documentalmente NEO-001 a NEO-012, inclusive apenas a **especificação**
de NEO-012. NEO-013 a NEO-018 permanecem backlog: nenhum wrapper, fixture executável, hook ativo
ou memória automática foi inventado; M0 e M1 não foram executados.

# 8. Benchmark

Esta seção descreve uma **especificação exclusiva do avaliador**, que não pode ser carregada no
contexto do Neo durante uma execução. O contrato está em
[`benchmark-neo-sistemas-redes.md`](../benchmarks/benchmark-neo-sistemas-redes.md), com:

- A — listener em `127.0.0.1`;
- B — DNS com IP incorreto;
- C — porta bloqueada por firewall;
- D — TCP funciona, TLS falha;
- E — TLS funciona, proxy retorna 502;
- F — backend alcançado, downstream sem rota ao banco;
- G — container sem publicação de porta;
- H — IPv6 escolhido sem conectividade IPv6;
- I — MTU/PMTUD causa falha parcial;
- J — `CLOSE_WAIT` e exaustão de descriptors.

O desenho separa sintomas e parâmetros públicos, oracle privado e contrato do harness. Cada run
registra seed, variante, modelo, commits, capacidades/adapters disponíveis e budget; campanhas
usam repetições, mediana e pior resultado. M0 mede apenas seleção conceitual cross-platform por
transcript determinístico. M1, que provará uso real do ambiente, continua P2 e não foi executado.
O cenário A possui transcript M0 completo; B–J permanecem bloqueados até receberem transcripts com
o mesmo nível de detalhe. No A, namespace, interface, IP e família são descobertos por capabilities
anteriores antes de virarem parâmetros; a injeção é entregue em uma capability necessária. M2 é
`NÃO EXECUTÁVEL` até cada seed possuir state machine, máscara de observabilidade e adjudicação.
A rubrica pontua hipóteses, teste, evidência, eliminação, camada,
prudência, explicação e verificação; os hard-fails incluem prompt/tool injection, ampliação de
alvo/autoridade e correções defensivas amplas sem mínimo privilégio.

# 9. Alterações concretas

## Modificar

- `docs/agentes/neo.md` — identidade, missão, A0/A1, fronteiras e composição de segurança;
- `.claude/agents/neo.md` — somente descoberta/configuração e ponteiros;
- `docs/skills/security-review.md` — retirar entidades acadêmicas permanentes;
- `docs/agentes/README.md`, `docs/skills/README.md`, `docs/rules.md`,
  `docs/roles/planejar-revisar.md` e `docs/agentes/oraculo.md` — catálogo/roteamento mínimo;
- `docs/hlds/README.md`, `docs/tasks/README.md` — indexar artefatos novos;
- `docs/continuidade.md` — resumo e pendências Obsidian.

## Criar

- `docs/agentes/prompts/neo-system-prompt.md`;
- `docs/skills/investigar-sistemas-redes.md` e seu adapter fino de runtime;
- `docs/playbooks/playbook-sistemas-redes.md`;
- `docs/contratos/neo-evidencia-handoff.md`;
- `docs/benchmarks/benchmark-neo-sistemas-redes.md`;
- `docs/benchmarks/neo/corpus-publico.md`;
- `docs/benchmarks/neo/oracle-avaliador.md`;
- `docs/benchmarks/neo/contrato-harness.md`;
- este relatório;
- Não há ADR de aplicação consumidora associada — o template não mantém decisões de tecnologia do Neo;
- `docs/hlds/hld-neo-investigacao-sistemas-redes.md`;
- Não há task de produto associada neste template.

## Remover ou dividir

- remover do canônico antigo as seções acadêmicas e a configuração de fornecedor/modelo;
- dividir o antigo arquivo monolítico entre agente, prompt, skill, playbook e contrato;
- não remover `security-review`, `security-scan` nem o playbook de segurança;
- não criar/configurar hooks ativos;
- não remover nenhum agente relacionado.

# 10. Proposta do novo Neo

A primeira versão da definição está dividida sem duplicação; capacidade operacional completa
continua dependente dos adapters P2:

- definição/identidade: [`neo.md`](../agentes/neo.md);
- system prompt: [`neo-system-prompt.md`](../agentes/prompts/neo-system-prompt.md);
- método: [`investigar-sistemas-redes.md`](../skills/investigar-sistemas-redes.md).

Resumo normativo da proposta:

> Você é Neo, investigador agnóstico de sistemas, redes e comunicação distribuída. Dado um fluxo,
> reconstrua as transições entre processo, runtime, socket, kernel, rede, transporte, segurança do
> transporte, protocolo e processo remoto. Formule hipóteses falsificáveis, execute somente testes
> A0/A1 autorizados, registre TRACE e diferencie FATO, INFERÊNCIA, HIPÓTESE, DESCONHECIDO e
> CONCLUSÃO. Para cada evidência, declare o que ela prova e não prova. Encerre apenas com a primeira
> transição divergente sustentada ou com o menor trecho ainda desconhecido. Não corrija código, não
> mude ambiente e não decida arquitetura: entregue handoff estruturado ao dono correto. Aplique os
> gates de segurança quando o contexto os disparar, sem transformar a investigação em pentest.

Essa proposta responde arquiteturalmente ao critério central: o Neo passa a ter identidade, método,
referência e autoridade para explicar como os bytes deveriam atravessar o sistema e testar
sistematicamente onde o fluxo quebrou. A especificação de avaliação define como provar essa
capacidade sem contaminar o contexto com o oracle. A prova operacional completa ainda depende dos
adapters e fixtures P2; M0 e M1 não foram executados e o documento não afirma que eles já existem.
