### FDD: Temporalidade e mercado do nível 2

Versão: 1.1  
Data: 2026-09-02  
Responsável: Severino  
Revisor: Yoda  
**Revisão arquitetural: Yoda — 2026-09-02 — status: aprovado.**

---

### 1. Contexto e motivação técnica
Detalha FR-005 e FR-006 do [PRD-001](../prds/prd-001-financas-pessoais.md) nas fronteiras do [HLD-001](../hlds/hld-001-arquitetura.md). A data do movimento passa a ser parte do fato; o cursor visual segue a [ADR-002](../adrs/adr-002-direcao-visual.md). O contrato de erro, as escalas, o modo de transação e as fronteiras de pacote estão fechados no [FDD-001](fdd-001-nivel-1.md), itens 3, 4, 6 e 7, e valem integralmente aqui. **Este FDD foi revisado e aprovado por Yoda em 2026-09-02; T-002 está liberada para implementação.**

### 2. Objetivos técnicos
- Produzir saldo e posição corretos para qualquer data, usando fatos até ela inclusive.
- Impedir negatividade em toda a série, janela de emissão/vencimento e fim de semana.

### 3. Escopo e exclusões
**Incluído**
- Emissão anterior a vencimento; preço de mercado por ativo/data; inclusão e exclusão de preço.
- Filtros obrigatórios início/fim para listas e data para saldo/posição.

**Excluído**
- Multiusuário e execução assíncrona do FDD-003; UI de data não desenhada além do contrato existente.
- **Escopo cortado nesta revisão:** não existe `GET /ativos/{ativo}/precos`. Nenhum FR e nenhum caso da matriz exige listar o histórico de preços; o preço vigente já aparece no ativo e na posição. Criar a rota seria superfície pública sem demanda.

### 4. Fluxos detalhados e diagramas
**Fluxo principal**
- Validar ISO date, emissão < vencimento, dia útil e janela [emissão, vencimento).
- Consultar fatos <= data; selecionar preço de mercado máximo com data <= consulta.
- Ao inserir fato, executar janela temporal para garantir saldo e posição não negativos em nenhuma data afetada.

**Fluxos alternativos e exceções**
- Movimento na emissão é válido; no vencimento é inválido; sábado e domingo são inválidos.
- Filtro ausente ou invertido é rejeitado; fato futuro não aparece em consulta anterior.

**Dia útil** (decisão D-B4): dia útil é qualquer dia de segunda a sexta-feira. **Não existe calendário de feriados**; nem o enunciado nem o PRD definem um, e inventá-lo mudaria silenciosamente o conjunto de datas aceitas. O cálculo usa o dia da semana da data no fuso `America/Sao_Paulo` (FDD-001, D-A8).

**Diagramas** (opcional)
- ❓ LACUNA: diagrama visual ainda não produzido. Não bloqueia a implementação.

### 5. Contratos públicos (assinaturas, endpoints, headers, exemplos)

#### 5.1 Preço de mercado por data (rotas fechadas nesta revisão — D-B1)

| Operação | Método e caminho | Payload | Sucesso |
|---|---|---|---|
| Definir preço na data | `PUT /ativos/{ativo}/precos/{data}` | `{"precoMercado":105.53}` | `201 Created` quando cria, `200 OK` quando substitui, corpo `{"ativo":"ATIVO1","data":"2020-02-28","precoMercado":105.53}` |
| Excluir preço na data | `DELETE /ativos/{ativo}/precos/{data}` | nenhum | `204 No Content` |

`{data}` usa `YYYY-MM-DD` e é a chave do preço, casando com o índice único `uq_valor_mercado_ativo_data`; `PUT` é, por isso, naturalmente idempotente. Erros: `400 CAMPO_INVALIDO` para data não parseável ou preço com mais de oito casas, negativo ou não numérico; `404 ATIVO_NAO_ENCONTRADO` para ativo inexistente; `404 RECURSO_NAO_ENCONTRADO` no `DELETE` de preço inexistente. Alternativa descartada: `POST /ativos/{ativo}/precos` com a data no corpo — exigiria decidir à parte o comportamento de recriação sobre a chave única, que o `PUT` resolve por definição.

#### 5.2 Ativo com janela de negociação

A partir do N2 o payload de `POST /ativos` e `PUT /ativos/{ativo}` é:
```json
{"ativo":"ATIVO1","nome":"Ação 1","tipo":"RV","dataEmissao":"2020-01-02","dataVencimento":"2021-01-02"}
```
`dataEmissao` e `dataVencimento` são **obrigatórias** e `dataEmissao < dataVencimento`, sob pena de `400 CAMPO_INVALIDO`. O campo `precoMercado` **sai do contrato** e, se enviado, responde `400 CAMPO_INVALIDO` (D-B2). A representação do ativo passa a ser:
```json
{"ativo":"ATIVO1","nome":"Ação 1","tipo":"RV","dataEmissao":"2020-01-02","dataVencimento":"2021-01-02"}
```

#### 5.3 Consultas temporais

Herdadas do FDD-001, item 2, com `data`, `dataInicio` e `dataFim` **obrigatórias** a partir do N2 (FDD-001, D-A8): `GET /contacorrente/saldo?data=`, `GET /posicao?data=`, `GET /contacorrente/lancamentos?dataInicio=&dataFim=`, `GET /movimentacao?dataInicio=&dataFim=`. Ausência responde `400 FILTRO_INVALIDO`.

**Exemplo de requisição**
```json
{"ativo":"ATIVO1","data":"2020-02-28","quantidade":2.5,"valor":105.53}
```

**Exemplo de resposta**
```json
{"saldo":1234.56}
```

### 6. Erros, exceções e fallback
- Matriz: fim de semana → `400 DATA_NAO_UTIL`; fora da janela [emissão, vencimento) → `400 FORA_DA_JANELA`; filtro ausente/invertido → `400 FILTRO_INVALIDO`; qualquer negatividade futura → `409 SALDO_INSUFICIENTE` ou `409 QUANTIDADE_INSUFICIENTE`; preço elegível ausente → ver D-B3, **não é erro**.
- **Decisão de status (D-B5):** fim de semana e violação de janela respondem **400**, não 409. Ambas são propriedades da data enviada, avaliadas antes de qualquer leitura do estado financeiro; `409` fica reservado a conflito com saldo, quantidade ou existência persistidos. Isto resolve a ambiguidade "409/400 conforme FDD-002" do TC-047 da matriz: é **400**.
- Códigos acrescidos ao catálogo do FDD-001, item 3: `DATA_NAO_UTIL` (400) e `FORA_DA_JANELA` (400).
- Estratégias de resiliência: transação curta, timeout de leitura e retry apenas em consulta segura.
- Invariantes: datas inclusive/exclusive corretas; fatos <= consulta; mercado máximo <= consulta; saldo/posição nunca negativos.

### 7. Observabilidade
**Métricas**
- Rejeições por janela, dia, escala e negatividade; latência de consulta temporal; quantidade de linhas examinadas; contagem de posições com preço indisponível.

**Logs**
- Data consultada, intervalo, regra rejeitada e correlation id; sem dados privados desnecessários.

**Tracing**
- Validação, consulta de janela, seleção de preço e commit.

**Dashboards e alertas**
- Taxa de rejeições temporais, consultas lentas, falhas de integridade e preço indisponível.

### 8. Dependências e compatibilidade
| Componente | Versão mínima | Observações |
|---|---|---|
| FDD-001 | 1.2 | fatos, fórmulas, escalas, erro, transação e pacotes |
| HLD-001 | 1.1 | SQL temporal e fronteiras |

**Quebras de compatibilidade introduzidas pelo T-002, deliberadas e registradas**
1. `precoMercado` sai de `POST`/`PUT /ativos` e passa a responder 400 (D-B2).
2. `dataEmissao` e `dataVencimento` passam a ser obrigatórias em `/ativos`.
3. `data`, `dataInicio` e `dataFim` passam a ser obrigatórias, sem valor padrão (FDD-001, D-A8).

Os casos TC-009, TC-012 e TC-015 da [matriz](../tasks/matriz-de-testes.md) usam o payload do N1 e devem ser atualizados para o payload do N2 dentro do T-002; a atualização é do dono da matriz e não é opcional, sob pena de falso verde. Nenhuma outra rota muda: as cinco rotas canônicas do N3 permanecem intactas.

### 9. Critérios de aceite técnicos
- Emissão é aceita e vencimento é rejeitado para movimento; fins de semana são rejeitados com 400.
- Consulta por data inclui fatos na data e exclui fatos posteriores.
- Lista exige início e fim inclusive/inclusive.
- Preço selecionado é o último com data menor ou igual à consulta, nunca um preço futuro.
- Ausência de preço elegível segue D-B3, sem erro e sem valor inventado.
- Inserção fora de ordem é rejeitada se gerar negatividade em qualquer data posterior.
- A migration do T-002 remove as âncoras `0001-01-01` e torna `data_emissao`/`data_vencimento` `NOT NULL`.
- Cada endpoint temporal tem teste REST com persistência/fixture própria, limpeza, rollback ou reset entre casos, reexecutável e com evidência. Não há exigência de idempotência semântica no POST.

### 10. Decisões arquiteturais fechadas na revisão (normativas)

#### D-B1 — Rotas de preço histórico
Fechadas no item 5.1. Existiam apenas como "rotas específicas não foram fornecidas no enunciado", o que obrigaria a implementação a inventar caminho, método e status. O enunciado não fornece a rota, mas escolher a rota é decisão de contrato, não de domínio: nada de regra de negócio é criado aqui.

#### D-B2 — Como o `precoMercado` do N1 desaparece no N2
O ativo nunca teve coluna de preço (FDD-001, D-A1). No N1 o campo era persistido como o preço na data-âncora `0001-01-01`. O T-002:

1. deixa de aceitar `precoMercado` em `/ativos`, respondendo `400 CAMPO_INVALIDO` se enviado — tolerar e ignorar esconderia bug de cliente e contrariaria o FR-005, que manda **remover** o preço direto do ativo;
2. remove as linhas de preço com `data = '0001-01-01'` na migration, porque uma âncora sobrevivente tornaria "sempre existe preço elegível" e mascararia D-B3;
3. mantém a regra temporal da consulta de posição — ela seleciona o preço mais recente com data menor ou igual à consulta desde o N1; no N3, o transporte passa a ser assíncrono conforme o FDD-003.

Efeito colateral aceito: ativos criados no N1 ficam sem preço até receberem um `PUT /ativos/{ativo}/precos/{data}`, e sua posição passa a exibir os campos de mercado como `null` conforme D-B3. Como o banco é embarcado e a carga é de desafio, o T-002 executa o reseed determinístico do ambiente após a migration; ativos preexistentes sem `dataEmissao`/`dataVencimento` são recriados pelo seed, não têm datas adivinhadas.

#### D-B3 — Ausência de preço de mercado elegível (fecha o `❓ LACUNA` bloqueante)
Quando não existe preço com data menor ou igual à data consultada, a posição do ativo é devolvida com:

```json
{"ativo":"ATIVO1","nome":"Ação 1","tipo":"RV","quantidade":2.50,
 "precoMercado":null,"valorMercadoTotal":null,"precoMedio":100.00000000,
 "rendimento":null,"lucro":-250.00}
```

Regras: `precoMercado`, `valorMercadoTotal` e `rendimento` vêm `null`; `quantidade`, `precoMedio` e `lucro` continuam calculados, porque não dependem do preço de mercado. A consulta responde `200 OK` e a linha é contabilizada na métrica de preço indisponível. **Nunca** se usa preço posterior à data, nem preço zero, nem se omite o ativo.

Alternativas descartadas, com o motivo:
- **Preço zero**: inventaria um valor financeiro falso e produziria `valorMercadoTotal` errado sem sinalizar nada — é exatamente a regra de negócio inventada que a arquitetura recusa.
- **Erro 409 na consulta inteira**: um ativo sem preço derrubaria a posição completa do usuário, inclusive dos ativos precificados; consulta de leitura não deve falhar por dado ausente de um item.
- **Omitir o ativo da lista**: esconderia uma posição que existe, o que é pior que exibi-la incompleta.
- **Usar o preço posterior mais próximo**: contradiz frontalmente o FR-005 e o critério de aceite do PRD.

Esta é decisão de contrato de leitura, tomada dentro do mandato que o PRD FR-005 deixou explícito ("conforme decisão futura"); ela não cria, altera nem remove regra financeira. Se o usuário preferir tratamento como erro de domínio, a mudança é local ao mapeamento de saída.

#### D-B4 — Dia útil sem calendário de feriados
Registrado no item 4.

#### D-B5 — 400 para fim de semana e janela
Registrado no item 6.

### 11. Riscos e mitigação
#### Janela SQL não cobre fatos fora de ordem
- **Probabilidade:** média
- **Impacto:** posição inválida.
- **Mitigação:** recomputar janela afetada e testar datas intercaladas.
- **Plano de contingência:** rejeitar a operação até correção.

#### Preço posterior vaza para consulta histórica
- **Probabilidade:** média
- **Impacto:** valor de mercado incorreto.
- **Mitigação:** predicado de data e teste de regressão.
- **Plano de contingência:** retirar consulta e preservar preço histórico.

#### Âncora do N1 sobrevive à migration
- **Probabilidade:** média
- **Impacto:** D-B3 nunca é exercitada e a ausência de preço fica silenciosamente mascarada.
- **Mitigação:** a migration remove `data = '0001-01-01'` e um caso de teste prova a ausência de âncoras após o T-002.
- **Plano de contingência:** reexecutar a limpeza e reprocessar as consultas afetadas.

### Histórico

- 2026-09-02: versão inicial.
- 2026-09-02: revisão arquitetural de Yoda — **aprovado**. Fechadas as decisões D-B1 a D-B5. Achados corrigidos: rotas de preço histórico ausentes; `❓ LACUNA` bloqueante da ausência de preço elegível; contradição do `precoMercado` com o FDD-001; contrato do ativo no N2 sem `dataEmissao`/`dataVencimento`; ambiguidade 400/409 do TC-047; feriados indefinidos; quebras de compatibilidade não declaradas; risco de âncora sobrevivente à migration; `GET` de histórico de preços cortado por falta de demanda.
