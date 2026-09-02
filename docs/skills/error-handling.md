# Skill: error-handling

**Autor:** Cristóvão Augusto

**Definição canônica e independente de ferramenta.** Adaptador de descoberta em `.claude/skills/error-handling/SKILL.md`, sem conteúdo próprio.

## Para o futuro agente

Projetar, implementar ou revisar tratamento de falhas como parte explícita do domínio e dos contratos. Usar em módulos, endpoints, integrações, jobs e fluxos assíncronos, sem presumir linguagem, framework, protocolo ou biblioteca.

## Origem e licença

Adaptada da skill `error-handling` do [mclovin137/TGM2](https://github.com/mclovin137/TGM2/tree/abc15b18bdc3e814c1dffa5e4e1eb99de7a7e292/.claude/skills/error-handling), vendorizada por esse projeto a partir de [affaan-m/ECC](https://github.com/affaan-m/ECC/tree/main/skills/error-handling), licença MIT. Snapshot de origem: `abc15b18bdc3e814c1dffa5e4e1eb99de7a7e292`.

## Princípios

- Representar falhas por categorias estáveis, não por comparação de texto.
- Separar erro de domínio, aplicação, infraestrutura e apresentação.
- Preservar causa e contexto técnico internamente, mas retornar somente informação segura ao consumidor.
- Nunca engolir erro. Tratar, transformar com causa, compensar ou propagar.
- Falha esperada pertence ao contrato e ao FDD.
- Não usar exceção de infraestrutura para modelar regra de negócio.
- Não registrar segredo, credencial, dado pessoal, payload integral ou stack trace em resposta pública.

## Procedimento

1. Ler FDD, HLD, ADRs e invariantes do domínio.
2. Inventariar pontos de falha em validação, autorização, estado, persistência, concorrência, rede, fila e dependências externas.
3. Classificar cada falha como recuperável, não recuperável, transitória ou permanente.
4. Mapear categoria interna para contrato externo sem acoplar o domínio ao transporte.
5. Definir timeout em toda chamada externa. Aplicar retry somente a falhas transitórias e operações idempotentes, com limite e backoff decidido.
6. Aplicar circuit breaker, fila de falhas, compensação ou fallback apenas quando HLD/FDD justificarem. Estratégia estrutural nova exige ADR.
7. Manter correlation ou trace ID e campos estruturados permitidos nos logs.
8. Definir métrica por categoria, taxa, latência, retry, exaustão e fallback.
9. Testar erro, timeout, repetição, recuperação, concorrência e não vazamento. Todo endpoint afetado exige teste de integração idempotente e evidência.
10. Atualizar o FDD e o `.md` de comportamento no mesmo commit.

## Invariantes de segurança e dados

- Falha parcial não pode violar invariantes dos dados críticos definidos pela aplicação consumidora em ADR.
- Operação não idempotente não recebe retry automático sem chave ou mecanismo equivalente decidido.
- Mensagem não confirma a existência de recurso que o ator não pode acessar.
- Fallback nunca transforma falha em sucesso falso nem reduz autorização.
- Erro inesperado não expõe query, caminho, versão, stack ou identificador de outro usuário.

## Matriz de saída

```markdown
| Condição | Camada de origem | Categoria | Contrato externo | Retry | Fallback ou compensação | Log e métrica | Teste |
| --- | --- | --- | --- | --- | --- | --- | --- |
| <falha> | <domínio/aplicação/infra/apresentação> | <tipo> | <semântica segura> | <não ou política> | <ação> | <campos permitidos> | <caso> |
```

## Checagens

- Todas as falhas previstas no FDD têm tratamento determinístico.
- Causa original é preservada internamente sem vazar ao consumidor.
- Retries têm elegibilidade, limite, backoff e observabilidade.
- Timeouts e cancelamento propagam até as dependências aplicáveis.
- Compensações e fallbacks preservam invariantes.
- Testes provam comportamento e ausência de vazamento.
- Não foi adicionada dependência, serviço ou padrão sem decisão vigente e custo zero.
