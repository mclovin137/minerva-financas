---
type: minerva-adr
project: Minerva Finanças
date: 2026-09-02
status: aceita
tags:
  - minerva
  - adr
ai-first: true
---

# ADR 003: Estrutura de pacotes e camadas do backend

## Para o futuro agente

Esta ADR fixa **como o backend é organizado**: camadas, pastas, nomenclatura e o uso de fábrica para
eliminar duplicação. Ela importa em toda criação ou movimentação de classe no backend, e **substitui**
a tabela de pacotes hexagonais do [HLD-001](../hlds/hld-001-arquitetura.md) e do
[FDD-001](../fdds/fdd-001-nivel-1.md), item 7.

## Contexto

A primeira implementação dos níveis 1 a 3 organizou o backend por *camada técnica global*
(`dominio/`, `aplicacao/`, `infra/`, `api/`), no estilo de portas e adaptadores. O
[playbook de backend](../playbooks/playbook-backend.md) já descrevia, na seção A, uma cadeia
diferente — Controller, Actor, Service, DAO — e, na seção B, a obrigação de DTO em todo request e
response. A implementação não seguiu o playbook, e a divergência só apareceu na revisão do usuário.

Duas causas concorreram, e as duas são endereçadas aqui:

1. O playbook estava escrito em tom consultivo, com ressalvas de anti-ritual em quase todo item. Um
   documento que se apresenta como sugestão é lido como sugestão.
2. Nenhum documento definia organização de pastas, convenção de nomes ou uso de fábrica. O que não
   está escrito é decidido por quem implementa, e cada implementação decide diferente.

## Escopo

- Afetado: todo o código de produção do backend, sua organização em pastas, os nomes de classe e as
  fronteiras entre camadas.
- Fora de escopo: contrato público da API — as rotas e os payloads não mudam; frontend; banco de
  dados; e a estratégia de testes, que continua no acervo de QA.

## Forças e critérios

- O playbook do projeto já prescrevia a cadeia REST → Actor → Service → DAO.
- O enunciado MAPS fixa literalmente quatro rotas de POST e uma de GET, que não podem mudar.
- Relatórios e operações irmãs (crédito/débito, compra/venda, posição síncrona/assíncrona) duplicam
  estrutura quando cada uma ganha o próprio caminho de ponta a ponta.
- Navegar por funcionalidade é mais frequente do que navegar por camada: quem mexe em ativo mexe no
  REST, no actor, no service e no DAO de ativo, quase nunca em todos os services do sistema.

## Opções consideradas

### Opção 1: pastas por entidade, com subpastas por função

- Status na análise: **escolhida**
- Benefícios: tudo o que uma funcionalidade usa fica junto; a fronteira entre entidades vira fronteira
  de diretório, visível em qualquer listagem; alinha com o playbook e com a decisão do usuário.
- Custos e limitações: tipos compartilhados precisam de um módulo `comum` explícito, sob risco de
  virar depósito; a estrutura tem mais diretórios.
- Trade-off: ganha coesão por funcionalidade e navegação previsível; aceita mais diretórios e a
  disciplina de manter `comum` pequeno.

### Opção 2: manter camadas técnicas globais (hexagonal)

- Status na análise: descartada
- Benefícios: direção de dependência verificável por três `grep`; já estava implementada.
- Custos e limitações: contraria o playbook vigente e a decisão do usuário; espalha uma
  funcionalidade por quatro árvores distantes.
- Trade-off: ganharia estabilidade de curto prazo ao custo de manter a documentação contradizendo a
  prática — que é o defeito que esta ADR existe para corrigir.

## Decisão

### Organização

Pasta por **entidade**, subpasta por **função**:

```
br.com.minerva.financas.<entidade>.<funcao>
```

Entidades: `contacorrente`, `ativo`, `movimentacao`, `posicao`, `usuario`. Funções: `rest`, `actor`,
`service`, `builder`, `dao`, `helper`, `dto`, `dominio`. O módulo `comum` guarda somente o que é
genuinamente compartilhado por mais de uma entidade.

### Fluxo

```
REST → Actor → { Helper, Service, Builder, DAO }
```

- **REST** recebe e devolve **DTO**, e nada além disso. Não contém regra, não conhece DAO.
- **Actor** orquestra um caso de uso. É onde a sequência de passos vive.
- **Service** concentra a regra de negócio, testável sem HTTP e sem banco.
- **DAO** é o único que fala SQL. Toda consulta é parametrizada.
- **Builder** monta objetos de domínio e DTOs de resposta, isolando a montagem de quem a usa.
- **Helper** guarda apoio puro e sem estado.
- **Domínio** guarda entidades, objetos de valor e invariantes.

### Nomenclatura

- O nome diz **o que a classe faz**: `CriarAtivoActor`, `ConsultarSaldoActor`, `LancamentoBuilder`,
  `ContaCorrenteDAO`, `AutenticacaoService`.
- **Interface tem prefixo `I`**: `IContaCorrenteDAO`, `IPosicaoService`. A implementação usa o mesmo
  nome sem o prefixo.
- Sem abreviação em nome de classe, método, campo ou parâmetro.

### Fábrica contra duplicação

Operações irmãs compartilham um único caminho no REST e uma **fábrica** escolhe o actor concreto:
`LancamentoActorFactory` devolve `CreditoActor` ou `DebitoActor`; `MovimentacaoActorFactory` devolve
`CompraActor` ou `VendaActor`; `PosicaoActorFactory` devolve o actor síncrono ou o assíncrono. Em vez
de um endpoint por relatório, um endpoint e uma fábrica.

**As rotas públicas não mudam.** O enunciado fixa `/contacorrente/credito`, `/contacorrente/debito`,
`/movimentacao/compra`, `/movimentacao/venda` e `/contacorrente/saldo`, e o TC-057 as testa
literalmente. A fábrica elimina a duplicação **atrás** do controller, não reescreve o contrato.

### Object Calisthenics

Aplicado a domínio, actors, services, builders e helpers. **DTOs são isentos das regras 8 e 9**
(máximo de duas variáveis de instância e ausência de acessores): um DTO é portador de dados por
definição, e submetê-lo a essas duas regras exigiria mapeamento manual de serialização sem ganho de
encapsulamento. As demais sete regras valem em todo o backend.

## Consequências

### Positivas

- Documentação e prática deixam de divergir.
- Uma funcionalidade é lida e alterada em um diretório só.
- Operações irmãs deixam de duplicar caminho de ponta a ponta.
- O prefixo `I` torna a fronteira de contrato visível sem abrir o arquivo.

### Negativas

- O gate de fronteira por `grep` sobre camadas globais deixa de valer e precisa ser reescrito para a
  estrutura por entidade.
- A reorganização toca praticamente todos os arquivos do backend de uma vez.

### Riscos residuais

- `comum` pode virar depósito de tudo o que não coube em uma entidade; tratar com revisão explícita a
  cada inclusão.
- A fábrica pode ser aplicada onde não há duplicação real, virando indireção sem ganho; tratar
  exigindo pelo menos duas implementações concretas antes de criar uma.

## Impacto nos artefatos

- `backend/src/main/java/**` inteiro.
- [HLD-001](../hlds/hld-001-arquitetura.md) e [FDD-001](../fdds/fdd-001-nivel-1.md), item 7: a tabela
  de pacotes hexagonais é substituída por esta decisão.
- [Playbook de backend](../playbooks/playbook-backend.md), seções A, B e C: passam a ser prescritivas.
- `.github/workflows/review.yml`: o gate de fronteira é reescrito.

## Relações

- Origem: revisão do usuário sobre a implementação dos níveis 1 a 3.
- ADRs relacionadas: [ADR-001](adr-001-stack-da-aplicacao.md), que fixa a stack e permanece vigente.
- Substitui: a tabela de pacotes do HLD-001 e do FDD-001, item 7. Nenhuma ADR é substituída.

## Critério para revisitar

Revisitar se a estrutura por entidade produzir dependência cíclica entre entidades que só se resolva
por um módulo compartilhado grande, ou se o número de entidades crescer a ponto de a duplicação entre
elas superar o ganho de coesão.

## Aceite

- Responsável arquitetural: Yoda
- Decisor: usuário
- Evidência do aceite: decisão explícita do usuário em 2026-09-02, revisando a primeira implementação.

## Histórico

- 2026-09-02: ADR criada com status aceita.
