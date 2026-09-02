# Skill: security-review

**Autor:** Cristóvão Augusto

**Definição canônica e independente de ferramenta.** Adaptador de descoberta em `.claude/skills/security-review/SKILL.md`, sem conteúdo próprio.

## Para o futuro agente

Revisar segurança de FDD, código, dependências, contratos e configurações, especialmente ao tratar
autenticação, autorização, input, output, segredos, uploads, dados sensíveis ou fronteiras de
confiança. A revisão pertence a **planejar/revisar**; Neo reporta e valida, mas não implementa a
correção.

## Origem e licença

Adaptada da skill `security-review` do [mclovin137/TGM2](https://github.com/mclovin137/TGM2/tree/abc15b18bdc3e814c1dffa5e4e1eb99de7a7e292/.claude/skills/security-review), snapshot `abc15b18bdc3e814c1dffa5e4e1eb99de7a7e292`. O arquivo de origem não declarou upstream nem licença; confirmar ambos antes de redistribuição fora do projeto Minerva.

## Prioridade

Começar pelos ativos e operações de maior impacto identificados no contexto temporário da aplicação.
Avaliar confidencialidade, integridade, disponibilidade, leitura/alteração/exclusão indevidas,
operações irreversíveis e ausência de trilha. Tratar toda entrada externa como não confiável sem
incorporar entidade ou prioridade do domínio à definição permanente da skill.

## Procedimento

1. Ler PRD, HLD, FDD, ADRs, modelo de papéis e contratos.
2. Mapear ativos, atores, fronteiras de confiança, entradas, saídas e operações irreversíveis.
3. Conferir autenticação e autorização no servidor em cada requisição, com negação por padrão.
4. Testar acesso horizontal, vertical, credencial ausente, inválida e expirada.
5. Validar input por lista de permitidos: tipo, faixa, tamanho, formato e rejeição de campo extra.
6. Revisar o ponto de uso contra SQL/ORM, comando, XSS, template, desserialização, log, cabeçalho, caminho e upload.
7. Revisar output e erros contra exposição de PII, segredo, stack, query, versão, caminho ou existência protegida.
8. Revisar segredos no código, histórico, configuração, artefatos, logs e pipelines.
9. Para dependência nova ou alterada, acionar `docs/skills/auditar-dependencias.md` e consumir seu relatório read-only. A revisão de segurança avalia o efeito da dependência nos controles e dados, sem repetir o inventário especializado e sem aprovar custo financeiro.
10. Criar casos de segurança na suíte compartilhada, idempotentes. Todo endpoint recebe no mínimo
    testes sem credencial, identidade/papel incompatível, referência a recurso fora do escopo
    autorizado e entrada maliciosa, usando a ferramenta aprovada e evidência.
11. Emitir achados com severidade, evidência, impacto, recomendação e critério de validação.

## Checklist mínimo

- Autenticação e autorização ocorrem no servidor.
- Cada endpoint declara permissão e nega por padrão.
- Campos desconhecidos são rejeitados.
- Queries e comandos usam mecanismos seguros no ponto de uso.
- Conteúdo renderizado ou exportado recebe encoding ou sanitização contextual.
- Upload valida conteúdo, tamanho, nome, armazenamento e acesso.
- Logs e traces não contêm PII, token, sessão ou payload sensível.
- Alterações críticas têm auditoria imutável suficiente para atribuição e recuperação.
- Dependências novas têm análise atual e licença compatível.
- Casos de segurança são idempotentes e cobrem todos os endpoints.
- Pipeline de review executa os gates decididos e pipeline de casos publica evidências.

## Formato do achado

```markdown
### SEC-NNN: <título>
- Severidade: crítica | alta | média | baixa | informativa
- Ativo e fronteira: <item>
- Evidência reproduzível: <passos sem segredo>
- Impacto: <confidencialidade, integridade ou disponibilidade>
- Regra ou contrato violado: <referência>
- Recomendação: <resultado esperado, sem implementar>
- Critério de validação: <teste objetivo>
- Estado: aberto | mitigado | aceito por ADR | falso positivo
```

Restrição de segurança que muda decisão técnica vira ADR com Yoda. Risco não aceito bloqueia o go-live; o revisor não corrige a feature nem aprova o próprio trabalho.
