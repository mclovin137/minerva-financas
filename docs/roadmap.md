# Roadmap: direção de produto Minerva Finanças

Aplicação de finanças pessoais para o desafio MAPS. A direção é guiada pelo [PRD-001](prds/prd-001-financas-pessoais.md), organizada pelo [HLD-001](hlds/hld-001-arquitetura.md), detalhada nos [FDDs](fdds/README.md) e executada nas [tasks T-001 a T-003](tasks/README.md). A direção visual vigente está na [ADR-002](adrs/adr-002-direcao-visual.md).

## Fundação documental concluída nesta cadeia

- Governança copiada do template autorizado e estado reescrito para esta aplicação.
- Stack registrada na [ADR-001](adrs/adr-001-stack-da-aplicacao.md).
- Design system auditável, PRD, HLD, FDDs e tasks criados; revisão independente ainda pendente.

## Direção de produto 1: núcleo financeiro

Executar T-001 após aprovação do FDD-001. Resultado: conta, ativos, operações e posição com integridade financeira.

## Direção de produto 2: eixo temporal

Executar T-002 após T-001. Resultado: consultas por data, janela útil e mercado histórico sem negatividade em qualquer data.

## Direção de produto 3: contrato e escala

Executar T-003 após decisão A/B. Resultado: contrato N3, seed, thread-safety, Docker e segurança ou consulta assíncrona com memória constante. Deploy cloud permanece adiado.
