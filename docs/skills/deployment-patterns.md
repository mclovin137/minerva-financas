# Skill: deployment-patterns

**Autor:** Cristóvão Augusto

**Definição canônica e independente de ferramenta.** Adaptador de descoberta em `.claude/skills/deployment-patterns/SKILL.md`, sem conteúdo próprio.

## Para o futuro agente

Planejar, implementar ou revisar mecanismos de build, publicação, deploy, healthcheck, rollback e prontidão operacional já decididos, sem escolher configuração, container, CI, estratégia de rollout ou ferramenta mantida como `TBD`. Severino mantém pipelines; Jarvis define gates operacionais e é dono do que ocorre após a liberação.

## Origem e licença

Adaptada da skill `deployment-patterns` do [mclovin137/TGM2](https://github.com/mclovin137/TGM2/tree/abc15b18bdc3e814c1dffa5e4e1eb99de7a7e292/.claude/skills/deployment-patterns), vendorizada por esse projeto a partir de [affaan-m/ECC](https://github.com/affaan-m/ECC/tree/main/skills/deployment-patterns), licença MIT. Snapshot de origem: `abc15b18bdc3e814c1dffa5e4e1eb99de7a7e292`.

## Bloqueios

- Exigir ADRs aceitas para hospedagem, CI/CD, estratégia de branches, empacotamento e banco quando necessários.
- Exigir HLD/FDD para topologia, dependências, healthchecks e comportamento operacional.
- Exigir solução de custo financeiro zero.
- Tratar Docker como fundação de execução reprodutível, sem assumir orquestrador, plataforma, cloud, workflow, registry ou escala horizontal.
- Não criar pipeline única: o projeto exige pipeline de review e pipeline de execução de casos de teste separados.

## Procedimento

1. Ler regras, ADRs, HLD, FDD e task.
2. Desenhar o caminho do commit até produção usando somente ferramentas decididas.
3. Garantir que o primeiro commit publicável já inclua aplicação acessível, infra e automação.
4. Configurar o pipeline de review para aderência documental, qualidade e segurança conforme decisões vigentes.
5. Configurar separadamente o pipeline de casos para executar a suíte, inclusive testes de integração por endpoint, e publicar as evidências definidas pela ADR de testes.
6. Preservar custo financeiro zero e não alterar recursos externos sem autorização explícita.
7. Produzir artefato reproduzível, imutável e identificável pelo commit, no formato decidido.
8. Validar configuração no startup e impedir segredo em código, imagem, log ou artefato.
9. Implementar liveness e readiness com semântica definida no HLD, sem expor detalhes internos.
10. Definir rollout e rollback compatíveis com o free tier e com versões coexistentes. Estratégia nova exige ADR.
11. Alinhar migrations reversíveis e compatíveis com rollback da aplicação.
12. Executar smoke tests e critérios objetivos de abortar ou reverter.
13. Registrar métricas, logs, alertas, capacidade, cota e evidência de rollback.

## Gates de produção

- Dois pipelines obrigatórios verdes e independentes.
- Evidências de testes publicadas e referenciáveis.
- Nenhuma publicação/deploy habilitado sem evidência de custo zero e autorização explícita do usuário.
- Artefato e dependências reproduzíveis e sem versões flutuantes, conforme tecnologia decidida.
- Aplicação executa com privilégio mínimo e configuração validada.
- Healthchecks distinguem processo vivo de instância pronta.
- Rollback da aplicação e das migrations foi testado.
- Backup e restore foram executados quando dados participam do risco.
- Logs estruturados não expõem PII ou segredos.
- Cotas do free tier têm medição e alerta antes do esgotamento.
- Dados críticos, cuja perda ou corrupção seja irreversível para o usuário final e que sejam definidos pela aplicação consumidora em ADR, não ficam sujeitos a perda para manter custo zero.

## Plano de entrega

```markdown
# Plano de deploy: <sistema ou versão>
- Origem: <task, HLD, FDD e ADRs>
- Artefato: <formato decidido e identificação>
- Ambiente: <topologia decidida>
- Pipeline de review: <gates>
- Pipeline de casos de teste: <suíte e evidências>
- Gatilho de deploy: merge em main
- Healthchecks: <liveness e readiness>
- Rollout: <estratégia decidida>
- Critérios de abortar: <sinais e limiares>
- Rollback: <aplicação, configuração e dados>
- Observabilidade: <logs, métricas e alertas>
- Capacidade e custo: <cotas e margem>
- Evidências: <execuções reais>
- Lacunas ou ADRs: <itens>
```

Não declarar prontidão sem teste de rollback e restore aplicável. Divergência operacional que muda arquitetura volta para Yoda como ADR.
