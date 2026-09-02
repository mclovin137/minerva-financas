### FDD: APIs financeiras do nível 1

Versão: 1.0
Data: 2026-09-02
Responsável: Severino
Revisor: Yoda

---

### 1. Contexto e motivação técnica
Implementa o primeiro recorte do [PRD-001](../prds/prd-001-financas-pessoais.md) dentro do [HLD-001](../hlds/hld-001-arquitetura.md): conta, ativos, movimentos e posição. O domínio não conhece REST nem persistência. Usuário e administrador são atores do produto; autenticação detalhada permanece ❓ LACUNA.

### 2. Objetivos técnicos
- Persistir crédito, débito, compra e venda atomicamente sem saldo ou quantidade negativa.
- Calcular posição por ativo com dinheiro em centavos, floor e fórmulas do PRD.

### 3. Escopo e exclusões
**Incluído**
- APIs de saldo, lançamentos, CRUD de ativos, compra, venda e posição.
- Validação de campos obrigatórios, escalas, saldo, quantidade e tipos `RV`, `RF`, `FUNDO`.

**Excluído**
- Datas, mercado histórico, autenticação multiusuário, posição assíncrona e Docker do nível 3.
- Telas e estados listados como ❓ LACUNA no design.

### 4. Fluxos detalhados e diagramas
**Fluxo principal**
- Receber comando, validar valor/descrição/ativo/quantidade, executar caso de uso e persistir fatos em uma transação.
- Consultar fatos, agregar por ativo e devolver quantidade, mercado, rendimento e lucro.

**Fluxos alternativos e exceções**
- Rejeitar entrada inválida, débito sem saldo, venda sem quantidade e ativo inexistente sem efeito parcial.
- Em atualização concorrente, reavaliar a invariante antes de confirmar.

**Diagramas** (opcional)
- ❓ LACUNA: diagrama visual ainda não produzido.

### 5. Contratos públicos (assinaturas, endpoints, headers, exemplos)
**Conta, ativos, movimentação e posição**
- Tipo: endpoint
- Assinatura/Rota: rotas REST de conta corrente, ativos, movimentação e posição definidas pelo contrato do nível 1; rotas exatas não foram fornecidas no enunciado.
- Método: GET, POST, PUT/PATCH e DELETE conforme operação
- Semântica de status/headers:
  - 2xx confirma operação; POST pode não ter corpo.
  - 400 para entrada inválida, 404 para recurso ausente, 409 para conflito de saldo/posição.

**Exemplo de requisição**
```json
{"valor":12.42,"descricao":"salário"}
```

**Exemplo de resposta**
```json
{"saldo":12.42}
```

### 6. Erros, exceções e fallback
- Matriz: escala/sinal/tipo inválido -> 400; ativo ausente -> 404; saldo ou quantidade insuficiente -> 409; falha de persistência -> 500 sem detalhe sensível.
- Estratégias de resiliência: timeout de banco e retry somente para leitura segura. O contrato MAPS não prevê chave de idempotência nos POSTs; proteção contra repetição é risco operacional e TBD, sem alterar payload ou endpoint.
- Política de fallback: falha transacional não altera nenhum fato e é reportada ao cliente.
- Invariantes: dinheiro em centavos; floor; quantidade não negativa; saldo não negativo; compra/venda e lançamento correlato atômicos.

### 7. Observabilidade
**Métricas**
- Latência e status por operação; rejeições por motivo; conflitos de saldo/quantidade.

**Logs**
- Estruturados com correlation id, caso de uso, resultado e duração, sem payload financeiro completo.

**Tracing**
- Entrada REST, caso de uso e transação SQL; exportador ❓ LACUNA.

**Dashboards e alertas**
- Erros 5xx, conflitos e falhas de transação; limiares ❓ LACUNA.

### 8. Dependências e compatibilidade
| Componente | Versão mínima | Observações |
|---|---|---|
| Java | 8 | stack registrada usa 25 |
| REST/JSON | contrato do nível 1 | rotas ainda ❓ LACUNA |

**Garantias de compatibilidade**
- Valores de entrada e saída respeitam sem fração de centavo; endpoints do nível 3 serão cobertos no FDD-003.

### 9. Critérios de aceite técnicos
- Crédito aumenta e débito reduz saldo; débito inválido não persiste.
- Preço aceita até oito casas, quantidade até duas, dinheiro até duas, e cálculos com floor.
- Compra gera saída e venda gera entrada; venda acima da posição é rejeitada.
- Posição retorna um registro por ativo e as quatro fórmulas do PRD.
- Cada endpoint definido durante a implementação possui teste REST com fixture isolada, limpeza, rollback ou reset entre casos, reexecutável e com evidência de pipeline. Isso não exige idempotência semântica do POST.
- FDD requer revisão e aprovação de Yoda antes de código.

### 10. Riscos e mitigação
### Rotas de nível 1 não foram especificadas
- **Probabilidade:** média
- **Impacto:** cliente e teste podem divergir.
- **Mitigação:** definir contrato antes da task e registrar alteração no FDD.
- **Plano de contingência:** bloquear endpoint até decisão.

### Arredondamento inconsistente
- **Probabilidade:** alta
- **Impacto:** divergência financeira.
- **Mitigação:** valor de domínio em centavos e testes de fronteira.
- **Plano de contingência:** rejeitar cálculo sem referência determinística.
