### FDD: Temporalidade e mercado do nível 2

Versão: 1.0
Data: 2026-09-02
Responsável: Severino

---

### 1. Contexto e motivação técnica
Detalha FR-005 e FR-006 do [PRD-001](../prds/prd-001-financas-pessoais.md) nas fronteiras do [HLD-001](../hlds/hld-001-arquitetura.md). A data do movimento passa a ser parte do fato; o cursor visual segue a [ADR-002](../adrs/adr-002-direcao-visual.md).

### 2. Objetivos técnicos
- Produzir saldo e posição corretos para qualquer data, usando fatos até ela inclusive.
- Impedir negatividade em toda a série, janela de emissão/vencimento e fim de semana.

### 3. Escopo e exclusões
**Incluído**
- Emissão anterior a vencimento; preço de mercado por ativo/data; inclusão e exclusão de preço.
- Filtros obrigatórios início/fim para listas e data para saldo/posição.

**Excluído**
- Multiusuário e execução assíncrona do FDD-003; UI de data não desenhada além do contrato existente.

### 4. Fluxos detalhados e diagramas
**Fluxo principal**
- Validar ISO date, emissão < vencimento, dia útil e janela [emissão, vencimento).
- Consultar fatos <= data; selecionar preço de mercado máximo com data <= consulta.
- Ao inserir fato, executar janela temporal para garantir saldo e posição não negativos em nenhuma data afetada.

**Fluxos alternativos e exceções**
- Movimento na emissão é válido; no vencimento é inválido; sábado e domingo são inválidos.
- Filtro ausente ou invertido é rejeitado; fato futuro não aparece em consulta anterior.

**Diagramas** (opcional)
- ❓ LACUNA: diagrama visual ainda não produzido.

### 5. Contratos públicos (assinaturas, endpoints, headers, exemplos)
**Consultas temporais e preço histórico**
- Tipo: endpoint
- Assinatura/Rota: APIs de conta, movimentação, saldo, posição e mercado do nível 2; rotas específicas não foram fornecidas no enunciado, além das rotas exatas do nível 3.
- Método: GET, POST e DELETE conforme operação
- Semântica de status/headers:
  - 400 para data, escala ou intervalo inválido; 409 para invariante; 404 para recurso ausente.

**Exemplo de requisição**
```json
{"ativo":"ATIVO1","data":"2020-02-28","quantidade":2.5,"valor":105.53}
```

**Exemplo de resposta**
```json
{"saldo":1234.56}
```

### 6. Erros, exceções e fallback
- Matriz: fora da janela/fim de semana -> 400; filtro ausente -> 400; qualquer negatividade futura -> 409; preço elegível ausente -> ❓ LACUNA.
- Estratégias de resiliência: transação curta, timeout de leitura e retry apenas em consulta segura.
- Política de fallback: não escolher preço posterior; sem preço elegível, aplicar decisão de domínio quando registrada.
- Invariantes: datas inclusive/exclusive corretas; fatos <= consulta; mercado máximo <= consulta; saldo/posição nunca negativos.

### 7. Observabilidade
**Métricas**
- Rejeições por janela, dia, escala e negatividade; latência de consulta temporal; quantidade de linhas examinadas.

**Logs**
- Data consultada, intervalo, regra rejeitada e correlation id; sem dados privados desnecessários.

**Tracing**
- Validação, consulta de janela, seleção de preço e commit.

**Dashboards e alertas**
- Taxa de rejeições temporais, consultas lentas e falhas de integridade.

### 8. Dependências e compatibilidade
| Componente | Versão mínima | Observações |
|---|---|---|
| FDD-001 | 1.0 | fatos e fórmulas do nível 1 são base |
| HLD-001 | 1.0 | SQL temporal e fronteiras |

**Garantias de compatibilidade**
- Consultas existentes passam a exigir filtros definidos pelo nível 2; mudanças incompatíveis devem ser versionadas e documentadas.

### 9. Critérios de aceite técnicos
- Emissão é aceita e vencimento é rejeitado para movimento; fins de semana são rejeitados.
- Consulta por data inclui fatos na data e exclui fatos posteriores.
- Lista exige início e fim inclusive/inclusive.
- Preço selecionado é o último com data menor ou igual à consulta, nunca um preço futuro.
- Inserção fora de ordem é rejeitada se gerar negatividade em qualquer data posterior.
- Cada endpoint temporal tem teste REST idempotente, persistência própria e evidência.
- FDD requer revisão e aprovação de Yoda antes de código.

### 10. Riscos e mitigação
### Janela SQL não cobre fatos fora de ordem
- **Probabilidade:** média
- **Impacto:** posição inválida.
- **Mitigação:** recomputar janela afetada e testar datas intercaladas.
- **Plano de contingência:** rejeitar a operação até correção.

### Preço posterior vaza para consulta histórica
- **Probabilidade:** média
- **Impacto:** valor de mercado incorreto.
- **Mitigação:** predicado de data e teste de regressão.
- **Plano de contingência:** retirar consulta e preservar preço histórico.

