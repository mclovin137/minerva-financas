# Roadmap — Minerva Finanças

Aplicação de finanças pessoais para o desafio MAPS, construída em quatro lotes.

## Lote 1 — Governança e fundação

- Governança copiada do template autorizado e estado reescrito para esta aplicação.
- Stack registrada na [ADR-001](adrs/adr-001-stack-da-aplicacao.md).
- Esqueleto Java 25 + Spring Boot + Maven, SQLite embarcado e schema inicial.

## Lote 2 — Nível 1

Implementar APIs de conta corrente, CRUD de ativos, compras, vendas e consulta de posição, com validações de valores, quantidades, saldo e posição.

## Lote 3 — Nível 2

Adicionar datas de emissão/vencimento e movimento, valores de mercado por data, consultas temporais, validação de não negatividade em nenhuma data e consulta assíncrona de posição com memória constante (opção B), além da alternativa multiusuário (opção A).

## Lote 4 — Nível 3 e entrega

Endurecer thread-safety e desempenho concorrente, completar testes de integração, pré-cadastros, Dockerfile, docker-compose, README final e documentação da segurança e execução.
