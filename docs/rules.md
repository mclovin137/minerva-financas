# Roles e regras do projeto Minerva

**Autor:** Cristóvão Augusto

**Documento canônico e independente de ferramenta.** É a fonte da verdade sobre como este projeto é operado por qualquer IA (Claude Code, Codex, outras) e por qualquer pessoa. Arquivos como `CLAUDE.md` e `AGENTS.md` são adaptadores finos que apontam para cá e não contêm regras próprias.

Regra nova, mudança ou remoção de regra: **acontece aqui**, nunca em um adaptador.

---

## Idioma

**Todo output gerado por qualquer LM neste projeto deve ser em pt-BR.** Inclui respostas ao usuário, mensagens de commit, descrições de PR, comentários de review, documentação, notas do Obsidian, ADRs, PRDs, nomes de tasks e mensagens de erro autorais. Identificadores de código, termos técnicos consagrados e saídas de ferramentas de terceiros permanecem como estão.

## Estado e continuidade

Três artefatos de continuidade vivem em `docs/`, ao lado deste documento, e são lidos antes de retomar qualquer task: o contrato canônico é [`docs/rules.md`](rules.md); o **estado** da task ativa vive em [`docs/continuidade.md`](continuidade.md); o inventário de dependências vive em [`docs/lib.md`](lib.md). **A raiz do repositório não hospeda nenhum deles** — caminho de raiz para esses arquivos é erro, não variante aceitável.

**Três tempos de vida, três arquivos.** Misturá-los foi o que fez o arquivo de estado crescer de 2.440 B para 88.950 B em nove dias:

| Arquivo | Tempo de vida | Cardinalidade | Como muda |
|---|---|---|---|
| [`docs/continuidade.md`](continuidade.md) | **estado** da task ativa | **1 por seção** | sobrescrito |
| [`docs/pendencias-obsidian.md`](pendencias-obsidian.md) | **obrigação aberta** da regra 3 | N | linha entra ao criar a pendência, sai ao conferir a nota |
| [`docs/historico/AAAA-MM.md`](historico/) | **registro** encerrado | N | append-only, **nunca reescrito** |

Resumo decisório de task encerrada, evidência já produzida e pendência já cumprida saem do arquivo de estado e vão para o histórico do mês. Obrigação documental ainda aberta **não** vai para o histórico: histórico é imutável, e pendência aberta ainda vai mudar.

**A rotação acontece na abertura da task, não no encerramento.** Ao começar uma task, o agente move para o histórico do mês o que a task anterior deixou e reduz `docs/continuidade.md` ao estado da task nova. Rotação no encerramento é o passo que se pula quando o trabalho já parece pronto; na abertura, ela é pré-requisito de começar.

Dois gates mecânicos sustentam isso, e a razão de existirem está registrada: sem eles, o arquivo acumulou sete blocos de *Resumo decisório mínimo*, três de *Decisões vigentes* e sete de *Pendências Obsidian*. O gate de **cardinalidade** recusa seção repetida ou fora do conjunto declarado; o gate de **tamanho** recusa `docs/continuidade.md` acima de 8 KB. Ambos rodam no pipeline de review, por `.github/scripts/verificar-continuidade.py`. Eles verificam **forma, não julgamento**: nenhum dos dois sabe se um parágrafo é estado ou registro — isso continua sendo trabalho do agente.

Agentes autorizados atualizam semanticamente `docs/continuidade.md`. O hook de uma ferramenta pode atualizar somente a região delimitada como gerada, sem inferir conclusão, alterar checklist ou substituir julgamento. Ferramentas sem adaptador equivalente registram as mutações manualmente antes de entregar. `docs/lib.md` nunca é atualizado pelo hook: toda dependência exige versão e finalidade verificadas por um agente.

---

## Regras de ferro

Não negociáveis. Qualquer proposta que viole uma delas deve ser recusada e substituída por alternativa conforme.

1. **Multi-agente e independente de ferramenta.** O projeto é operado por múltiplas IAs (Claude Code, Codex e outras). Skills, agentes e roles são definidos em markdown neutro, sem depender de recursos exclusivos de um fornecedor. Arquivos específicos de ferramenta (`CLAUDE.md`, `AGENTS.md`, `.codex/`, etc.) são **adaptadores finos** que apontam para a definição canônica — nunca a fonte da verdade.
2. **Output em pt-BR.** Ver seção *Idioma*.
3. **Obsidian é a documentação oficial.** Ver seção *Documentação no Obsidian*.
4. **Três responsabilidades de agente:** orquestrar, planejar/revisar, implementar. Ver seção *Arquitetura de agentes*.
5. **Custo financeiro zero.** Nenhuma dependência, serviço, hospedagem, runner ou ferramenta paga. Só free tier permanente ou open source self-hosted sem custo. Se a única solução viável para um problema for paga, o problema volta para decisão do usuário — não se contrata nada. Ver seção *Escopo da regra 5*.
6. **Aplicação consumidora acessível desde o primeiro entregável.** O primeiro entregável da aplicação consumidora já publica uma aplicação acessível, com infraestrutura, pipeline e deploy no escopo inicial. O template não hospeda aplicação.
7. **DDD + teste de integração em todo endpoint da aplicação consumidora.** A aplicação consumidora implementa e valida essa obrigação; ver seção *Arquitetura e testes*.
8. **Dois pipelines de CI/CD da aplicação consumidora:** um de review e um de execução dos casos de teste. A aplicação consumidora implementa esses dois papéis; ver seção *CI/CD*.
9. **Exceção enxuta para ajuste básico ou urgente.** Só pode dispensar task e documentação formal com autorização explícita do usuário para aquela mudança e nas condições da seção *Fluxo de trabalho → Exceção enxuta*. Branch nova, PR, revisão independente e segurança continuam obrigatórios.
10. **Conflito material entre regras exige decisão do usuário.** Quando regras de ferro — ou seus efeitos — colidirem materialmente, o agente para e apresenta trade-offs, alternativas, impacto e a regra excepcional ao usuário; nunca escolhe silenciosamente.
11. **Toda interface segue o design system da aplicação consumidora.** A aplicação consumidora define-o, registra a direção visual em ADR e o torna contrato auditável. Tokens, tipografia, layout e teclado são contrato, nenhum estado depende só de cor, e o que não foi desenhado é `❓ LACUNA`.
12. **Toda tela da aplicação consumidora é responsiva.** Nenhuma faixa de largura de uso fica sem comportamento definido e utilizável. Responsividade é obrigação de resultado, não a mesma tela encolhida; o que não estiver desenhado é `❓ LACUNA` e não pode ser improvisado.

---

## Escopo da regra 5

A regra 5 rege **o que o projeto contrata**: dependência, biblioteca, hospedagem, banco de dados, runner de CI, serviço externo, domínio, e qualquer coisa que o sistema precise para existir ou rodar. Tudo isso é free tier permanente ou open source self-hosted sem custo. Solução paga volta como decisão do usuário.

A regra 5 **não rege as ferramentas de IA que operam o projeto**. As assinaturas do Claude Code e do Codex/ChatGPT são **custo previsto e aceito pelo usuário**: são ferramenta de trabalho dele, já existiam antes do projeto e continuariam existindo sem ele. Consequência prática, para não haver dúvida em auditoria: escolher `opus`, `gpt-5.6-agua`, `gpt-5.6-luna` ou qualquer modelo dentro dessas assinaturas **não viola a regra 5**. Configuração concreta de fornecedor/modelo pode ficar somente no adaptador da ferramenta quando a definição canônica do agente exigir independência total desse runtime, como no Neo.

A fronteira é a pergunta: *quem paga a conta e por quê?* Se o custo nasce de uma escolha do projeto e entra na infraestrutura do sistema, a regra 5 vale. Se é a ferramenta com que o usuário trabalha, não vale — e cobrar custo zero aí só levaria a operar pior sem economizar nada.

---

## Arquitetura de agentes

Toda atividade pertence a exatamente uma das três responsabilidades. A separação existe para que nenhum agente aprove o próprio trabalho.

**Orquestrador — quando o fluxo da sessão o exigir, é a sessão principal (Oráculo)**
- Interpreta a demanda, localiza a posição dela no fluxo (ver *Fluxo de trabalho*) e delega ao agente certo.
- Mantém estado: o que está em andamento, o que está bloqueado, o que aguarda auditoria.
- **Não altera, não cria e não apaga nenhum arquivo** — inclusive por shell (`>`, `rm`, `mv`, `sed -i`, mutações de git). Toda escrita acontece dentro de um agente delegado; não existe mudança pequena demais para delegar.
- **Não escreve código de produção e não aprova PR.**

**Planejador / Revisor**
- Escreve roadmap, épicos, PRDs e a quebra em tasks.
- Faz a **auditoria** do PR: aderência ao PRD, às regras de ferro, cobertura de testes e evidências.
- **Não implementa a task que ele mesmo planejou** quando houver agente disponível para implementar.

**Implementador**
- Implementa a task, escreve os testes e produz as evidências.
- Abre o PR e responde ao review.
- **Não aprova nem faz merge do próprio PR.**

**Delegação permanentemente autorizada.** O usuário autoriza, de forma permanente e para toda sessão, que a sessão principal despache subagentes conforme os gatilhos e responsabilidades canônicos, sem confirmação caso a caso. A autorização cobre apenas o despacho: não dispensa regra de ferro, gate, revisão independente nem qualquer autorização específica exigida por outra regra.

Definições de agentes, roles e skills são markdown neutro, versionado no repositório, consumível por qualquer ferramenta (regra 1). Skills são usadas conforme seus próprios contratos e gatilhos; agentes são acionados pelos gatilhos e responsabilidades desta regra. Toda criação/edição/remoção de agente, role ou skill exige atualização do Obsidian (regra 3).

### Catálogo de agentes

| Agente | Responsabilidade | Escopo principal |
|---|---|---|
| [Oráculo](agentes/oraculo.md) | Orquestrar | Interpreta, localiza a etapa, delega e acompanha; não escreve arquivos |
| [Yoda](agentes/yoda.md) | Planejar/revisar | Arquitetura, HLD, ADR, trade-offs e conformidade arquitetural |
| [Severino](agentes/severino.md) | Implementar | Todo o código da aplicação, migrations, pipeline-as-code e documentação associada |
| [Patrick Jane](agentes/patrick-jane.md) | Planejar/revisar | Estratégia, casos e evidências de QA; não implementa a feature |
| [Neo](agentes/neo.md) | Planejar/revisar | Investiga e prova falhas em sistemas/redes por A0/A1; mantém gates de segurança contextuais e não corrige nem muda ambiente |
| [Jarvis](agentes/jarvis.md) | Implementar operação | Ambientes, pós-pipeline, deploy, rollback, observabilidade, backup e incidente |
| [c4-diagram-generator](agentes/c4-diagram-generator.md) | Planejar/revisar | Diagramas C4 em PlantUML, fundamentados em FDD aprovado |

---

## Documentação no Obsidian

A documentação oficial vive na base **`Minerva`** do vault Obsidian do usuário, **fora deste repositório**:

```
Windows : C:\Users\mclov\OneDrive\Documentos\Obsidian Vault\mclov\Documents\SecondBrain\Bases\Minerva
WSL     : /mnt/c/Users/mclov/OneDrive/Documentos/Obsidian Vault/mclov/Documents/SecondBrain/Bases/Minerva
```

**Todo gatilho vira pendência documental imediata.** Como a base está fora do repositório, o diff do PR não prova sua atualização. Registre em `docs/pendencias-obsidian.md` a origem, o destino, o responsável e o prazo máximo aplicável; sincronize antes se o usuário pedir. A nota só é declarada sincronizada depois de escrita e conferida. A base anterior não pode ser apresentada como atualizada enquanto houver pendência; divergência não é resolvida silenciosamente.

**Regime de reprazo, decidido nesta emenda.** O teto de **24 horas continua obrigatório para a criação de toda pendência documental**, contado da criação do gatilho. **Reprazar é ato posterior e distinto**, permitido tanto para uma pendência isolada quanto para um lote acumulado, mas somente por decisão expressa do usuário: cada pendência pode ser reprazada **uma única vez**, por **no máximo 7 dias corridos a partir da data da decisão**. Este teto de 7 dias é uma escolha desta emenda para oferecer uma janela operacional limitada sem permitir adiamento indefinido; não é exceção ao teto de criação. O registro do reprazo deve conter a data da decisão e o motivo. Reprazo não declara nota sincronizada, não apaga a dívida e não dispensa a sincronização; apenas altera, dentro desse teto, o prazo da pendência ainda aberta. A pendência continua bloqueando quando o prazo reprazado vencer.

Procedimento obrigatório, com a tabela de gatilhos e o formato das notas: `docs/skills/atualizar-obsidian.md`.

Gatilhos obrigatórios, em criação, edição **ou** remoção: regra de negócio; dependência (adição, remoção ou upgrade relevante); banco de dados (tabelas, índices, triggers e funções); ADR; PRD; HLD; FDD; roadmap; roles; skills; agentes; tasks. Decisões estruturais e escolhas de tecnologia entram como **ADR**, inclusive as pendências listadas no fim deste documento.

**ADR, PRD, HLD, FDD, LLD, RFC e as notas de responsabilidade têm cópia canônica na base e espelho no repositório** (`docs/adrs/`, `docs/prds/`, `docs/hlds/`, `docs/fdds/`, `docs/llds/`, `docs/rfcs/`, `docs/roles/`). O espelho e a pendência seguem o contrato em *Arquitetura e testes → Cópia canônica e espelho*. Pendência dentro do prazo é rastreável; pendência vencida bloqueia conclusão e trabalho dependente.

---
## Fluxo de trabalho

### Documentos: qual criar, e por quê

Documentos de produto, design e arquitetura formam uma cadeia de abstração: começam no problema de produto, aproximam-se da implementação e registram decisões. Cada artefato responde uma pergunta própria e não compete com os outros.

| Documento | Pergunta principal | Detalhe | Momento de uso |
|---|---|---|---|
| PRD | Qual problema de produto resolver e qual valor se espera? | baixo detalhe técnico | antes do desenho técnico |
| HLD | Como a solução se organiza em alto nível? | alto nível técnico | após clareza de produto, antes do detalhamento estrutural |
| FDD | Como uma feature ou módulo será implementado? | intermediário | quando o escopo da feature estiver definido |
| LLD | Como a implementação concreta será estruturada? | alto detalhe técnico | próximo da implementação, quando reduzir ambiguidade |
| RFC | Quais alternativas ainda estão em discussão? | orientado ao debate | antes de decisão técnica relevante |
| ADR | Qual decisão arquitetural foi tomada e por quê? | registro objetivo | depois da decisão |

Quatro regras governam a escolha:

1. **Proporcionalidade.** A escolha é proporcional ao porte da mudança, ao risco técnico e à necessidade de alinhamento. Não é ritual, e a taxonomia **não autoriza gerar documento por catálogo**: cria-se somente o artefato que responde uma pergunta real, tem dono e permanecerá útil depois da entrega.
2. **Declaração de origem.** Ao criar um documento, declare a categoria, a pergunta que ele responde, quais artefatos anteriores consultou, por que os níveis não usados não são necessários, e quem é dono e revisor.
3. **Reciprocidade e atualidade.** Ao revisar, confirme que o conteúdo continua atual, que os links são recíprocos quando houver decisão ou impacto estrutural, e que não duplica outro artefato.
4. **Obsoleto é defeito.** Documento errado, contraditório ou obsoleto é risco operacional para pessoas e IA: corrigir, substituir ou marcar explicitamente seu estado é obrigatório — nunca tratá-lo como contexto vigente por silêncio.

Os demais tipos que o projeto reconhece — FRD e TRD como nomenclatura legada, LLD em formato RUP, AI Design Doc, Prompt Spec, Security Design Doc, documento de avaliação de IA, runbook, e documentos de observabilidade, capacidade, infraestrutura e CI/CD — não são regra e sim referência de quando cada um cabe. Ficam em [`docs/playbooks/playbook-documentacao.md`](playbooks/playbook-documentacao.md), consultado na criação do documento. O que continua sendo obrigação deste contrato: **estratégia, casos e evidências de teste seguem as regras de QA**, o **pipeline-as-code atribuído é de Severino** e a **operação, garantias de liberação e pós-deploy são de Jarvis** — nada disso é dispensado por ser secundário à codificação.

### ADR — memória técnica, elegibilidade e governança

Um **Architecture Decision Record** registra uma decisão arquitetural e, principalmente, **por que** ela foi tomada: contexto, restrições, alternativas, trade-offs e consequências. O código mostra o que foi implementado, mas não preserva essas pressões. O ADR é memória técnica explícita para pessoas e agentes; reduz tradição oral e reabertura de decisão sem o contexto que a justificou.

**Uma ADR por decisão.** Não descreve o sistema inteiro nem mistura escolhas independentes. Usa **identificador estável e nomenclatura previsível** para permitir ordenar, citar e relacionar decisões — número duplicado no acervo é defeito, não variante. A escrita é objetiva: contexto suficiente para sustentar a decisão, **sem transformar o ADR em narrativa histórica extensa**.

**Exige ADR** a decisão com impacto arquitetural duradouro ou difícil de reverter, que afete múltiplos módulos, equipes ou contratos públicos, ou cujo motivo esquecido prejudique segurança, custo, desempenho, interoperabilidade, operação ou evolução: modularização, fronteira entre componentes, persistência, autenticação e autorização, observabilidade, deploy, resiliência, versionamento de contrato público, dependência crítica e lock-in.

**Não exige ADR** convenção local já institucionalizada, regra de domínio que evolui dentro de uma feature, organização de arquivos sem efeito arquitetural, padrão de implementação sem contrato público ou parâmetro de ajuste frequente — esses pertencem a README, HLD, FDD ou LLD, salvo se representarem ruptura de paradigma, restrição sistêmica ou reversão de impacto amplo. Na zona cinzenta: *se o porquê se perder, isso prejudicará a evolução do sistema?*

**Governança de processo não é ADR.** Regra sobre como o projeto é operado — papel de agente, fluxo, gate, critério de auditoria, divisão de artefatos — mora **nesta fonte canônica**. O acervo registra a arquitetura do produto; este documento registra o processo que o constrói.

**Ciclo de vida.** ADR passa pelo mesmo fluxo governado de código: branch, PR, revisão independente, evidências e histórico. Decisão antiga não é reescrita para parecer atual: decisão nova cria ADR novo, aponta a relação e marca o anterior como `superseded` ou inativo. Documento contraditório, incompleto ou obsoleto gera falsa confiança e deve ser corrigido ou explicitamente marcado; nunca é contexto vigente por silêncio.

**Encadeamento.** Todo ADR aponta para os PRDs, RFCs, HLDs, FDDs, LLDs e ADRs que fundamentam a decisão; os documentos impactados apontam de volta, usando `dependsOn`, `relatesTo` e `supersedes` quando aplicável, para que o acervo seja grafo navegável e não pasta de arquivos. **Linkagem assistida, revisão humana obrigatória:** agentes propõem relações a partir de evidência documentada, mas não inventam vínculo nem mudam estado de ADR por inferência; o revisor confirma cada ligação antes de registrá-la.

### Classificação antes da delegação

Antes de qualquer delegação, o Oráculo classifica a mudança como **via rápida** ou **fluxo completo**. Se identificar possível exceção enxuta, ele não a inicia nem a classifica autonomamente: explica escopo, motivo, controles mantidos e documentação dispensada, e pede autorização explícita do usuário para aquela mudança. Fora da exceção autorizada da regra 9, a classificação, as validações e os agentes acionados ficam registrados na task e no resumo decisório mínimo.

### Exceção enxuta: ajuste básico ou urgente

```
branch nova → PR → validações proporcionais → revisão independente → merge
```

Pela regra 9, ajuste básico e/ou urgente pode dispensar **task e documentação formal** somente após autorização explícita do usuário para aquela mudança e quando não introduz, altera ou remove comportamento de produto, regra de negócio, endpoint, contrato público, persistência, integração, dependência, segredo, permissão, pipeline, decisão arquitetural ou mudança estrutural.

- Não dispensa branch nova, PR, revisão independente, validação proporcional, segurança nem obrigação legal, regulatória ou contratual aplicável.
- A LLM ou o orquestrador apenas identifica a possibilidade e pede autorização; não escolhe, classifica nem inicia a exceção por conta própria.
- Não se aplica a nenhum gatilho da regra 3. Havendo gatilho documental, a pendência imediata e a sincronização no prazo continuam obrigatórias; na dúvida, não usar a exceção.
- O PR registra objetivamente o motivo de urgência ou simplicidade, escopo, validações executadas e a justificativa de não haver gatilho documental. Esse registro não substitui documento obrigatório.
- Neo é acionado para qualquer superfície de segurança; havendo incerteza, acesso a arquivo, hook, permissão, segredo, dependência ou configuração sensível, a exceção para e retorna à via rápida ou ao fluxo completo.
- Conflito material com outra regra de ferro segue a regra 10.

### Via rápida: manutenção sem comportamento de produto

```
task curta → implementação delimitada → validações proporcionais → revisão independente → merge
```

Usar somente para mudança documental, governança, adaptador de ferramenta, automação mecânica ou manutenção que **não** introduza, altere ou remova comportamento de produto, regra de negócio, endpoint, contrato público, persistência, integração, dependência ou decisão estrutural.

- A task curta declara objetivo, arquivos ou superfície permitida, exclusões, validações e revisor independente.
- PRD, HLD e FDD não são exigidos quando não houver comportamento de produto. Decisão estrutural continua exigindo ADR; impacto estrutural continua exigindo HLD.
- A validação é proporcional e determinística sempre que possível: sintaxe, links, JSON, shell, diff, comportamento do hook ou automação tocada. A revisão independente continua obrigatória.
- Segurança não é opcional: Neo é acionado quando a mudança toca permissões, hooks, segredos, dependências, pipeline, configuração, acesso a arquivos ou superfície de ataque. Jarvis é acionado para ambiente, deploy, rollback, observabilidade, backup ou custo. Patrick Jane é acionado se surgir comportamento observável ou contrato testável. Yoda é acionado para decisão estrutural, tecnologia, fronteira ou regra de governança.
- Se durante a execução surgir comportamento de produto, risco não coberto ou decisão estrutural, a via rápida para e a mudança retorna ao fluxo completo.

### Fluxo completo: feature ou mudança estrutural

```
roadmap → épico → PRD → RFC (quando houver deliberação) → HLD (quando estrutural) → FDD → LLD (quando reduzir ambiguidade) → task → PR → auditoria → merge → deploy
```

- **Roadmap:** direção do produto; origem de todo épico. **Épico:** recorte grande derivado do roadmap.
- **PRD:** o quê e o porquê, com critérios de aceite verificáveis.
- **HLD:** partes, fronteiras e contratos entre elas. Obrigatório quando a mudança for estrutural. **Dono: Yoda.**
- **FDD:** como cada feature funciona por dentro. Obrigatório quando houver comportamento, regra de negócio, integração, contrato público ou risco relevante. **Dono: quem implementa; revisor: Yoda.** O autor não revisa o próprio FDD (regra 4).
- **LLD:** contratos e detalhes executáveis. Opcional; usar quando o FDD não reduzir ambiguidade suficiente para implementar e testar com segurança. **Dono: quem implementa; revisor: Yoda quando tocar arquitetura, fronteira ou contrato.**
- **RFC:** proposta e alternativas antes de decisão relevante ainda em aberto. Opcional; tomada a decisão, seu registro segue a regra de ADR aplicável.
- **Task:** unidade executável derivada do PRD e do FDD, com escopo fechado. **Numeração:** a primeira task formal do ciclo atual é `T-001`, e as seguintes avançam sequencialmente a partir dela; não inferir numeração por notas, arquivos ou registros históricos.
- **PR:** entrega da task, com testes e evidências anexadas. **Auditoria:** revisão contra PRD e regras de ferro, por agente diferente de quem implementou. **Merge:** só após auditoria aprovada e pipelines verdes. **Publicação e deploy:** custo financeiro zero e gates continuam obrigatórios.

**ADR é transversal:** não ocupa posição rígida na cadeia, porque uma decisão estrutural pode nascer no PRD, RFC, HLD, FDD, LLD ou diante de um problema encontrado no código.

Ao validar uma implementação, lê-se a cadeia **antes** do código: PRD, RFC aplicável, HLD, FDD, LLD aplicável e só então o diff. Ler o código primeiro faz avaliar se ele é coerente consigo mesmo, em vez de coerente com o que foi decidido.

Não iniciar feature sem PRD e task correspondentes. Mudança estrutural também exige HLD; comportamento, regra de negócio, integração, contrato público ou risco relevante também exigem FDD aprovado. A exceção é a via rápida, limitada pelos critérios acima.

### Resumo decisório mínimo

Toda task e toda atualização semântica de `docs/continuidade.md` registra, em formato curto e factual:

- **Objetivo:** resultado e limite da mudança.
- **Decisão:** classificação da mudança, caminho escolhido e decisões aplicadas ou pendentes.
- **Evidências:** validações executadas, resultados e artefatos consultados.
- **Riscos e lacunas:** risco remanescente, `❓ LACUNA`, bloqueio ou "nenhum identificado" com base observada.
- **Próximo passo:** ação concreta, dono e condição de continuidade ou conclusão.

O resumo não substitui PRD, HLD, FDD ou ADR. Ele evita depender de histórico de chat e não pode declarar validação que não foi executada. **Enquanto a task está aberta, o resumo vive em `docs/continuidade.md` e é sobrescrito; ao encerrar, ele é movido para `docs/historico/AAAA-MM.md` na abertura da task seguinte** (ver *Estado e continuidade*).

### Conflito entre regras de ferro

Conflito material não é resolvido por interpretação silenciosa. O agente interrompe a execução, descreve as regras e efeitos em colisão, alternativas viáveis, impacto de cada uma e qual regra seria excepcionalmente limitada, e pede ao usuário que escolha o trade-off. Até a decisão explícita, não implementa, aprova, faz merge nem declara conformidade.

### Acionamento proporcional de agentes

Todo PR recebe revisão independente de agente diferente de quem implementou. O orquestrador aciona somente as especialidades exigidas pelo risco, sem transformar agentes em etapa decorativa:

| Gatilho | Acionamento obrigatório |
|---|---|
| Decisão de tecnologia, estrutura, fronteira, contrato entre módulos ou regra de governança | Yoda; ADR quando a decisão for estrutural ou tecnológica, HLD quando houver impacto estrutural |
| Endpoint, comportamento observável, critério de aceite, fluxo, invariante ou contrato público | Patrick Jane; PRD e FDD conforme esta seção |
| Falha de comunicação, processo/socket/porta, DNS, rota, TCP/UDP/QUIC, TLS, HTTP, proxy ou dependência distribuída que exija localização por evidência | Neo investiga por A0/A1; Jarvis só executa mutação/mitigação A2 e Severino correção versionada A3 |
| Autenticação, autorização, input/output, segredo, dependência, hook, pipeline, credencial, acesso a arquivo ou outra superfície de ataque | Neo |
| Ambiente, container, deploy, rollback, observabilidade, backup/restore, capacidade ou custo de operação | Jarvis |

Yoda, Patrick Jane, Neo e Jarvis mantêm pareceres independentes dentro do próprio escopo. Nenhum é chamado apenas para confirmar o trabalho de outro; ausência de gatilho deve ser justificada na task. A auditoria geral integra os pareceres aplicáveis, sem aprovar trabalho próprio.

**Triagem por severidade.** Achado de auditoria é classificado antes de virar trabalho, e **só o bloqueante reabre o ciclo**. É bloqueante: vazamento alcançável de dado, credencial ou segredo; **falso verde** — teste ou gate que afirma sucesso onde há falha; comportamento de produto errado ou ausente; perda ou corrupção de dado; regressão em controle que já funcionava. Não é bloqueante: imperfeição de controle interno que não deixa passar defeito de produto; **falso-positivo**; cobertura incompleta de caso de borda em ferramenta de CI; imprecisão documental; ergonomia. **Falso verde bloqueia, falso-positivo não** — um gate que deixa passar defeito é perigoso, um que barra PR legítimo é chato. Auditar controle interno não é auditar produto: o pior caso de um linter é barrar um PR, e por ser ilimitado por natureza ele exige critério de parada que auditar produto não exige.

**Dívida aceita é anotada no código.** Achado não bloqueante não vai para backlog paralelo: recebe `⚠️ DÍVIDA` no próprio código, **adjacente ao ponto exato** e nunca no topo do arquivo, dizendo **o que** ficou aberto e **por que** foi aceito; sem as duas informações a anotação é ruído. É irmão do `❓ LACUNA` e difere no destino: `❓ LACUNA` para e volta ao usuário, `⚠️ DÍVIDA` não para nada — é risco já aceito, registrado para quem vier depois.

**Teto de duas rodadas.** Cada PR admite no máximo **duas** rodadas de correção pós-auditoria; a terceira aciona a regra 10. A classificação como via rápida ou fluxo completo fica **visível na task ou no corpo do PR**, e sem ela a auditoria não começa.

**Revalidação proporcional.** Correção de achado já auditado **roda o gate que reprovou, no menor escopo que contenha a correção — o pacote, o arquivo ou o caso de teste que o achado citou —, e entrega ao CI**: não reabre auditoria nem exige bateria nova. O resto fica com os dois pipelines da regra 8, que rodam em ambiente limpo e são reprodutíveis, o que a bateria local não é; reexecutar localmente um job já verde repete prova feita. Continua proibido declarar validação não executada.

A correção **volta a exigir auditoria** quando deixa de ser correção e vira mudança: quando toca contrato público, endpoint, formato de erro ou código de status; migration, esquema, índice ou constraint; superfície de segurança — autenticação, autorização, input/output, segredo, permissão, dependência, hook ou pipeline; quando altera comportamento observável além do que o achado descrevia; ou quando o diff é **materialmente maior que o achado**, alcançando arquivo, pacote ou fluxo que ele não citava, ou embutindo refatoração que ninguém pediu. O teste é um só: *o revisor teria escrito o mesmo achado olhando este diff?* Se não, é mudança nova e é reclassificada antes de seguir. O teto acima conta **reaberturas de auditoria, não correções**: a correção que fecha o achado encerra a rodada em curso, e rodada nova só é consumida quando aparece achado bloqueante novo — inclusive quando o gate reprova de novo pelo mesmo motivo, porque aí a correção não corrigiu.

**Custo aceito, declarado:** sem a bateria local, o que o gate que falhou não cobre passa a depender inteiramente do CI, e defeito de interação — ordem de teste, estado compartilhado, isolamento de banco, concorrência, ordenação de migration — pode atravessar um CI verde; as hipóteses de retorno à auditoria acima cobrem onde essa classe de defeito é mais provável e mais cara, não a eliminam.

---
## Arquitetura e testes

### Decisões vigentes

Este template não carrega decisão de tecnologia nem de arquitetura da aplicação. A aplicação consumidora registra suas decisões em ADRs próprias, e o acervo dessa aplicação é a fonte de verdade. A única orientação estrutural deste contrato é DDD e SOLID (regra 7); o restante é decisão da aplicação consumidora.

Decisões deste contrato que não nasceram de ADR:

- **DDD, SOLID** e os princípios de qualidade definidos por ADR orientam a arquitetura.

### Cópia canônica e espelho

A **base Obsidian é a cópia canônica** de ADR, PRD, HLD, FDD, LLD, RFC e das notas de responsabilidade (regra de ferro 3). O repositório guarda um **espelho de leitura** em [`docs/adrs/`](adrs/), [`docs/prds/`](prds/), [`docs/hlds/`](hlds/), [`docs/fdds/`](fdds/), [`docs/llds/`](llds/), [`docs/rfcs/`](rfcs/) e [`docs/roles/`](roles/), para que um agente trabalhando no código leia a decisão sem depender do vault. Três regras:

1. **Pendência rastreada primeiro.** Criação, edição ou remoção é registrada imediatamente em `docs/pendencias-obsidian.md`, com origem, destino, responsável e prazo máximo de 24 horas; se o usuário decidir reprazar, aplica-se o regime explícito de reprazo da seção *Documentação no Obsidian*; pedido do usuário antecipa a sincronização.
2. **A base vence após conferência.** Em divergência confirmada, vale a base; o espelho é regenerado a partir dela, não reconciliado à mão. Antes da conferência, registro operacional temporário não pode alegar que a base foi atualizada.
3. **A única diferença permitida é mecânica.** O espelho preserva integralmente corpo e frontmatter e converte apenas os wikilinks: `[[nota]]` vira link Markdown para o caminho relativo, e `[[nota|Rótulo]]` preserva o rótulo — wikilink não resolve fora do Obsidian. O frontmatter é mantido porque carrega informação de decisão (o `status` de uma ADR, sobretudo) e porque manter as cópias byte a byte iguais fora dos links torna a divergência detectável por `diff`.

A relação é inversa à das skills: skill tem canônico no repositório e registro na base; documento de decisão tem canônico na base e espelho no repositório. Pendência dentro do prazo é aceitável na auditoria quando completa e rastreável; **pendência vencida bloqueia a conclusão da mudança e o início de trabalho dependente**. Não existe scheduler nem automação externa que substitua essa responsabilidade.

### DDD e testes

**DDD.** O domínio é o núcleo: regras de negócio ficam isoladas de transporte, persistência e detalhes de framework. Infraestrutura depende do domínio, nunca o contrário. Contextos se comunicam por contratos explícitos, ids ou eventos, nunca importando silenciosamente entidades internas de outro contexto.

**Testes de integração — obrigatórios para todo endpoint:**

- **Idempotentes:** rodam repetidamente, em qualquer ordem, sem depender de estado deixado por execução anterior. Cada teste cria e limpa o próprio dado.
- **Evidência obrigatória:** cada execução publica evidência como artefato do pipeline e a referencia no PR.

Endpoint sem teste de integração com evidência não passa na auditoria.

---

## Design system

As regras 11 e 12 exigem que a aplicação consumidora defina seu próprio design system, registre a direção visual em ADR e a torne contrato auditável. Tokens, tipografia, layout, teclado e acessibilidade devem ser verificáveis; nenhum estado pode depender somente de cor.

A aplicação consumidora também deve definir o comportamento responsivo de cada faixa de uso. O que não tiver sido desenhado é `❓ LACUNA` e não pode ser improvisado. O template não fornece design system, telas ou piso específico de acessibilidade; a aplicação consumidora fixa esse piso em ADR própria.

---

## CI/CD

Dois papéis de pipeline separados (regra 8), implementados pela aplicação consumidora:

| Papel | Contrato |
|---|---|
| **Review** | aderência às regras de ferro, aos artefatos aprovados e à qualidade da entrega |
| **Execução de casos de teste** | execução da suíte aplicável e publicação das evidências como artefato |

Os dois papéis não precisam ter nomes ou arquivos específicos. Publicação e deploy, quando existirem na aplicação consumidora, continuam sujeitos à regra de custo zero.

**Nenhum check bloqueia merge neste repositório.** Isso é uma propriedade do plano free do GitHub, não uma decisão deste template; os gates são detecção, e merge com check vermelho continua sendo violação.

---

## Adaptadores por ferramenta

| Arquivo | Ferramenta | Aponta para | Conteúdo permitido |
|---|---|---|---|
| `CLAUDE.md` | Claude Code | `docs/rules.md` | Ponteiro canônico + regra de idioma + autorização permanente de delegação |
| `AGENTS.md` | Codex e demais agentes | `docs/rules.md` | Ponteiro canônico + regra de idioma + autorização permanente de delegação |
| `.claude/skills/<skill>/SKILL.md` | Claude Code | `docs/skills/<skill>.md` | Frontmatter de descoberta + ponteiro |
| `.claude/agents/<agente>.md` | Claude Code | `docs/agentes/<agente>.md` | Frontmatter de despacho (`model`, `effort`, `tools`) + ponteiro + enquadramento imperativo de despacho, quando a encarnação exigir |
| Hooks de sessão, guarda e continuidade | Claude Code e Codex | suas definições canônicas | Por autorização explícita do usuário, somente `PreToolUse` do Claude Code está ativo para `Write|Edit|NotebookEdit|Bash`; `SessionStart`, `PostToolUse` e hooks do Codex permanecem versionados e inativos |

Definição canônica de skill: `docs/skills/`. Fica no repositório, e não na base Obsidian, para que um agente trabalhando no código a leia sem depender da base; a base documenta que a skill existe (gatilho da regra 3).

O **enquadramento imperativo de despacho** existe porque o corpo do adaptador é o que vira o system prompt do subagente: instrução de acionamento escondida no `description` é lida por quem despacha, não por quem executa. Ele diz **como acionar** o que o canônico já decidiu — nunca cria, altera ou remove regra, responsabilidade, recusa ou escolha de encarnação, que continuam sendo exclusividade do canônico (regra de ferro 1). Parâmetros voláteis, como a linha de comando da encarnação primária, ficam no canônico e são referenciados, não copiados.

Ao adicionar suporte a uma ferramenta nova, cria-se **mais um adaptador ponteiro** — nunca uma cópia do conteúdo. A regra de idioma (regra 2) e a autorização permanente de delegação são repetidas inline nos adaptadores de propósito. Configurar automação de ciclo de vida exige autorização explícita do usuário; nenhum hook é registrado ou reconfigurado por conversa.

---

## Catálogos e arquivos permanentes

O índice canônico das **skills** é [`docs/skills/README.md`](skills/README.md), e o dos **agentes** é [`docs/agentes/README.md`](agentes/README.md), com as definições em `docs/skills/` e `docs/agentes/`. Este contrato não mantém uma segunda cópia dessas listas: catálogo duplicado diverge, e a divergência não é detectável por `diff`.

| Arquivo | Papel |
|---|---|
| `docs/rules.md` | Contrato canônico de governança, roles e decisões vigentes |
| `docs/continuidade.md` | **Estado** da task ativa: cardinalidade 1 por seção, sobrescrito, com a única região gerada |
| `docs/pendencias-obsidian.md` | **Obrigações abertas** da regra 3: origem, destino, responsável e prazo; a linha sai quando a nota é conferida |
| `docs/historico/AAAA-MM.md` | **Registro** append-only e imutável: resumo decisório de task encerrada, evidências e pendências cumpridas |
| `docs/lib.md` | Inventário de todas as dependências, com nome, versões, finalidade e status |
| `docs/roadmap.md` | Declaração sobre roadmap da aplicação consumidora |
| `docs/agentes/` | Definições canônicas dos agentes, com índice em `README.md` |
| `docs/skills/` | Definições canônicas das skills, com índice em `README.md` |
| `docs/playbooks/` | Referências diagnósticas e decisórias por gatilho |
| `docs/contratos/` | Contratos estruturados compartilhados, inclusive evidência e handoff do Neo |
| `docs/benchmarks/` | Benchmarks versionados de capacidades de agentes |
| `docs/analises/` | Análises estruturais e gap analyses que fundamentam decisões e backlog |
| `docs/tasks/` | Índice de tasks do template |
| `docs/refinamentos/` | Índice de refinamentos do template |
| `docs/adrs/`, `docs/rfcs/`, `docs/hlds/`, `docs/fdds/`, `docs/llds/`, `docs/prds/`, `docs/roles/` | Espelhos; cópia canônica na base Obsidian |
| `.github/workflows/` | Automação versionada no GitHub |

### Playbooks

Consulta seletiva. **Não substituem ADR, HLD, FDD ou skill prescritiva, e não contêm regra** — regra vive neste documento.

| Arquivo | Gatilho |
|---|---|
| `playbook-backend.md` | Idempotência, fila, fronteiras de contexto, resiliência, cache, lote, erros e anti-overengineering |
| `playbook-database.md` | Queries, índices, locks, pool, concorrência, isolamento, paginação e diagnóstico |
| `playbook-documentacao.md` | Qual tipo de documento cabe: tipos legados, contextuais e emergentes além da cadeia PRD → HLD → FDD → LLD |
| `playbook-security.md` | Injeção, autenticação, autorização, segredos, supply chain e LGPD |
| `playbook-sistemas-redes.md` | Processos, sockets, redes, DNS, transportes, TLS, HTTP, infraestrutura e superfícies defensivas |

---
