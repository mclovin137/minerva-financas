---
type: minerva-hld
project: Minerva
date: 2026-08-25
status: proposta
tags:
  - minerva
  - hld
  - agentes
  - neo
ai-first: true
---

# HLD — Neo: investigação de sistemas e redes

## Para o futuro agente

Este HLD organiza a definição, o contexto temporário, o ciclo investigativo, as capacidades de
ferramenta, a evidência e os handoffs do Neo. A arquitetura do Neo **não tem ADR vigente** — a ADR
008 que a decidia foi removida em 2026-08-28 por decisão do usuário (colisão de número com
`adr-008-fronteira-de-rede-controlada-por-deteccao.md`). A fonte que resta é `docs/rules.md`
(catálogo de agentes) e este próprio HLD; este documento mostra como as partes se relacionam sem
prescrever runtime ou ferramenta.

## Objetivo e escopo

Permitir que Neo receba um sintoma entre componentes, reconstrua o fluxo esperado, investigue A0/A1
por camadas e entregue causa sustentada ou o menor trecho desconhecido.

Fora de escopo: adapters executáveis, fixtures de laboratório, hook ativo, armazenamento externo de
memória, mutação de ambiente e correção da aplicação.

## Arquitetura de alto nível

```text
┌───────────────────────────────────────────────────────────┐
│ Application Context — temporário, com origem e validade   │
└──────────────────────────┬────────────────────────────────┘
                           ▼
┌───────────────────────────────────────────────────────────┐
│ Neo — identidade, missão, limites e autoridade A0/A1      │
└───────────┬──────────────────┬────────────────────────────┘
            │                  │
            ▼                  ▼
  ┌──────────────────┐  ┌──────────────────────────────────┐
  │ Prompt operacional│  │ Skill TRACE                     │
  │ disciplina do turno│ │ plano, ledger, teste e adaptação│
  └──────────┬────────┘  └──────────────┬───────────────────┘
             └──────────────┬───────────┘
                            ▼
  ┌─────────────────────────────────────────────────────────┐
  │ Playbook: fundamentos e capability→adapter discovery    │
  └─────────────────────────┬───────────────────────────────┘
                            ▼
  ┌─────────────────────────────────────────────────────────┐
  │ Sensors/adapters A0/A1 oferecidos pelo ambiente         │
  └─────────────────────────┬───────────────────────────────┘
                            ▼
  ┌─────────────────────────────────────────────────────────┐
  │ TRACE + Evidence + Hypothesis Ledger + Decision Log     │
  └──────────┬──────────────┬─────────────┬─────────────────┘
             ▼              ▼             ▼
         Conclusão       Handoff A2    Handoff A3/decisão/QA
                         Jarvis        Severino/Yoda/Patrick
```

## Componentes e responsabilidades

| Componente | Responsabilidade | Dependência |
|---|---|---|
| Agente Neo | identidade, competência, autoridade, fronteiras e gatilhos | `docs/rules.md` (sem ADR vigente — ver *Relações*) |
| Prompt | disciplina mínima de execução e resposta | agente |
| Skill TRACE | método, estados, critérios de ação/interpretação/encerramento | agente, contrato |
| Playbook | conhecimento consultável e adapters possíveis | skill |
| Adapter de agente | descoberta/configuração específica do harness | agente + prompt |
| Sensor/adapter | observar ou testar uma capacidade no ambiente | autorização, plataforma |
| Contrato de evidência | schema TRACE, prova/não-prova, correlação e retenção | skill |
| Contrato de handoff | payload por destino e validação pós-ação | evidência, agentes relacionados |
| Benchmark do avaliador | especificar avaliação de processo e segurança A–J; nunca compõe o contexto do Neo | todos os anteriores |

## Fluxo principal

1. Oráculo classifica a demanda e entrega Context Card inicial ao Neo.
2. Neo valida escopo/autorização e modela origem→destino.
3. Skill cria Layer Map e Hypothesis Ledger.
4. Neo escolhe capability e descobre adapter disponível.
5. Teste A0/A1 gera TRACE com timestamp, ambiente, adapter/versão, parâmetros, exit/status,
   observação, referência/hash e `Prova/Não prova`.
6. Neo atualiza ledger e Decision Log; o ciclo repete enquanto reduzir o desconhecido.
7. Neo conclui ou produz handoff estruturado ao dono do próximo passo.
8. Destinatário executa A2/A3/decisão/QA e devolve evidência pós-ação.
9. Neo valida tecnicamente a camada; Patrick valida comportamento quando aplicável.

## Fluxos de handoff

| Origem | Destino | Gatilho | Retorno esperado |
|---|---|---|---|
| Neo | Jarvis | mudança/mitigação de ambiente A2 | evidência de ação, rollback e teste pós-ação |
| Neo | Severino | código/config/teste versionado A3 | diff e teste de regressão |
| Neo | Yoda | estrutura/fronteira/tecnologia | decisão/ADR ou lacuna explícita |
| Neo | Patrick | comportamento/cobertura | matriz e validação independente |
| Neo | Oráculo/usuário | falta de autoridade/contexto | decisão ou dado necessário |

## Autoridade e proteção

| Nível | Dono | Exemplos | Controle |
|---|---|---|---|
| A0 | Neo | ler socket, rota, logs, config, captura existente | read-only, escopo registrado |
| A1 | Neo | DNS query, connect/request limitado, captura limitada | hipótese, timeout/volume/alvo, dados redigidos |
| A2 | Jarvis | restart, firewall, rota, DNS, deploy/rollback | mudança operacional, rollback, observabilidade |
| A3 | Severino | código, manifesto, pipeline, config/teste versionados | task, diff, testes, revisão |

Yoda não executa A2/A3; decide quando a evidência exige arquitetura. Patrick não corrige; valida
comportamento. Segurança contextual pode bloquear qualquer nível conforme `security-review`.

## Modelo de estado

O estado da investigação vive durante a tarefa:

- Context Card;
- Layer Map;
- Hypothesis Ledger;
- unidades TRACE;
- Decision Log;
- pacote de handoff.

Não há banco nem memória longa nesta onda. Ao encerrar, contexto específico não migra ao prompt.
Promoção de padrão ao playbook é trabalho governado futuro, com redação e avaliação
isolada.

## Observabilidade e evidência

O ponto de observação é parte do dado. Outputs de plano de controle são indiretos até teste de plano
de dados. Relógios não sincronizados reduzem a força de correlação. Evidência grande fica em local
autorizado; o repositório guarda referência redigida, não packet capture sensível.

## Portabilidade

O playbook define capacidades antes de adapters. Linux, Windows e macOS podem produzir o mesmo fato
por CLI, API ou GUI. Containers/cloud adicionam namespace e planos de controle/dados, sem mudar a
metodologia. IPv4 e IPv6 são caminhos separados em todos os testes relevantes.

## Falhas e degradação

| Condição | Comportamento |
|---|---|
| adapter preferido ausente | descobrir equivalente; senão registrar desconhecido |
| privilégio negado | não contornar; pedir evidência ao dono |
| output truncado/perdido | marcar qualidade parcial; não concluir além dele |
| clocks divergentes | registrar incerteza e usar ids/portas adicionais |
| sintoma não reproduzido | registrar condições e janela; não declarar saudável |
| teste exige A2/A3 | parar e fazer handoff |

## Segurança

- mínimo privilégio, alvo, volume, duração e retenção;
- sem exploração, evasão ou varredura aberta;
- secrets/payloads/dados pessoais redigidos;
- `security-review` composto quando houver gatilho;
- exceção de risco estrutural continua exigindo ADR;
- a guarda `PreToolUse` do orquestrador é a única ativa; ela pertence ao contrato de governança e não a este HLD.

## Critérios arquiteturais de aceite

- nenhum conhecimento de negócio, stack ou fornecedor define Neo;
- prompt, skill, playbook, adapter, contrato e especificação do benchmark têm responsabilidade
  única; corpus/oracle do avaliador nunca entram no contexto runtime do Neo;
- A0–A3 e destinos são consistentes em todos os artefatos;
- toda conclusão usa fatos/inferências/hipóteses/desconhecidos e `Prova/Não prova`;
- a especificação do benchmark cobre A–J, seeds/variantes, rubrica, repetição, isolamento do oracle
  e hard-fails, sem alegar execução;
- roteamento alcança Neo para diagnóstico e segurança sem retirar função de Jarvis;
- links internos e paridade de adapters passam nos gates locais.

## Riscos e próximos passos

- Implementar adapters estruturados e fixtures M1 é P2, após auditoria desta arquitetura.
- Completar os transcripts B–J e então validar o prompt em M0 antes de ampliar texto; M0 ainda não
  foi executado.
- M2 permanece não executável até adjudicação, state machine e máscara de observabilidade por seed.
- Só separar novos especialistas se benchmark/profiling demonstrar necessidade.
- Não ativar hooks como substituto de disciplina do agente.

## Relações

- ADR: ❓ LACUNA — a ADR 008 que decidia esta arquitetura foi removida em 2026-08-28 (colisão de número, decisão do usuário); não há ADR vigente para o Neo. Decidir se a arquitetura ganha uma ADR renumerada ou permanece sem ADR.
- Análise: [`evolucao-neo-sistemas-redes.md`](../analises/evolucao-neo-sistemas-redes.md)
- Task: nenhuma task de produto registrada; este HLD permanece referência agnóstica de governança.

## Histórico

- 2026-08-25: proposta inicial criada; revisão independente pendente.
