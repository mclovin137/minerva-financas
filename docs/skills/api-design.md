# Skill: api-design

**Autor:** Cristóvão Augusto

**Definição canônica e independente de ferramenta.** Adaptador de descoberta em `.claude/skills/api-design/SKILL.md`, sem conteúdo próprio.

## Para o futuro agente

Planejar ou revisar contratos de API consistentes e verificáveis sem escolher protocolo, estilo, framework ou formato antes de HLD, FDD e ADR vigentes. Usar para novas interfaces, evolução de contratos, paginação, filtros, erros, versionamento, limites e revisão de endpoints.

## Origem e licença

Adaptada da skill `api-design` do [mclovin137/TGM2](https://github.com/mclovin137/TGM2/tree/abc15b18bdc3e814c1dffa5e4e1eb99de7a7e292/.claude/skills/api-design), vendorizada por esse projeto a partir de [affaan-m/ECC](https://github.com/affaan-m/ECC/tree/main/skills/api-design), licença MIT. Snapshot de origem: `abc15b18bdc3e814c1dffa5e4e1eb99de7a7e292`.

## Pré-condições

- Ler PRD, HLD, FDD e ADRs aplicáveis.
- Confirmar o protocolo e o estilo já decididos. REST, GraphQL, gRPC, eventos ou outro estilo são alternativas, não defaults.
- Confirmar exposição, consumidores, identidade, autorização, compatibilidade e requisitos não funcionais.
- Marcar como `❓ LACUNA` qualquer regra de negócio ausente e como decisão pendente qualquer escolha estrutural.

## Procedimento

1. Nomear a interface pela linguagem do domínio e pelo contexto delimitado, não pela estrutura interna do framework.
2. Especificar operação, entrada, saída, tipos, formatos, tamanhos, limites e semântica de ausência.
3. Declarar autenticação e autorização no servidor, incluindo acesso horizontal, vertical e negação por padrão.
4. Definir sucesso, validação, conflito, indisponibilidade e falha sem vazar detalhes internos nem existência de recurso protegido.
5. Definir idempotência, concorrência, repetição, timeout, rate limiting e backpressure conforme o FDD.
6. Escolher paginação, filtros, ordenação e busca a partir do volume e da consistência exigida. Não fixar paginação por offset ou cursor sem justificativa aprovada.
7. Definir evolução compatível, depreciação e critério para breaking change conforme decisão vigente.
8. Especificar logs, métricas e tracing sem dados pessoais ou segredos.
9. Derivar casos de teste de todos os fluxos. Cada endpoint exige integração idempotente com dados próprios, limpeza e evidência definida pela ADR de testes.
10. Registrar mudança estrutural em ADR e mudança de comportamento no `.md` correspondente e no Obsidian.

## Checklist do contrato

- Nome e semântica usam a linguagem ubíqua do domínio.
- Entrada rejeita campos não declarados e contém tipo, faixa, tamanho e formato.
- Saída expõe somente campos autorizados para o ator.
- Erros têm código estável, mensagem segura e correlação observável.
- Idempotência e concorrência estão explícitas para operações mutáveis.
- Listagens têm ordenação determinística e estratégia de paginação justificada.
- Limites de carga, taxa e tempo são mensuráveis ou estão marcados `TBD`.
- Compatibilidade e versionamento têm política decidida, não presumida.
- Testes cobrem sucesso, validação, autorização, erro, borda e estado inválido.
- Nenhuma escolha implica serviço pago ou tecnologia ainda pendente.

## Saída mínima

```markdown
### Contrato: <nome>
- Origem: <PRD, HLD, FDD e ADRs>
- Tipo e protocolo: <decidido ou bloqueio>
- Exposição e consumidores: <interno ou externo, atores>
- Operação: <assinatura, rota, tópico ou equivalente>
- Entrada: <campos, tipos, limites e exemplo>
- Saída: <campos, tipos e exemplo>
- Erros: <condição, semântica e resposta segura>
- Autorização: <regra por ator e recurso>
- Idempotência e concorrência: <garantias>
- Limites: <taxa, tamanho, timeout e paginação>
- Compatibilidade: <política e breaking changes>
- Observabilidade: <logs, métricas e spans>
- Testes: <fluxos e evidências de integração>
- Lacunas e ADRs: <itens>
```
