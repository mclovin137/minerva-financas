### FDD: Nível 3, contratos e opções operacionais

Versão: 1.0
Data: 2026-09-02
Responsável: Severino
Revisor: Yoda

---

### 1. Contexto e motivação técnica
Detalha os contratos N3 do [PRD-001](../prds/prd-001-financas-pessoais.md) e os limites de concorrência do [HLD-001](../hlds/hld-001-arquitetura.md). A entrega cloud foi adiada por decisão do usuário, mas Dockerfile e docker-compose são incluídos; a escolha final entre opções A e B é ❓ LACUNA.

### 2. Objetivos técnicos
- Expor payloads e rotas exatos sem conteúdo nos POSTs e saldo JSON.
- Demonstrar thread-safety e, na opção B, id, 425, processamento paralelo e memória constante.

### 3. Escopo e exclusões
**Incluído**
- `POST /contacorrente/credito`, `POST /contacorrente/debito`, `POST /movimentacao/compra`, `POST /movimentacao/venda` e `GET /contacorrente/saldo?data=...`.
- Seed `ATIVO0` até `ATIVO127` com preço de mercado de 2020-01-02.
- Opção A: Basic, usuários, root e isolamento. Opção B: consulta assíncrona e escala.
- Dockerfile e docker-compose.

**Excluído**
- Deploy cloud efetivo, pois adiado pelo usuário.
- Inventar rota N1/N2 não fornecida.

### 4. Fluxos detalhados e diagramas
**Fluxo principal**
- Receber Basic quando A ativa, validar JSON, aplicar caso de uso e retornar status.
- Para compra/venda, persistir movimento e conta atomicamente.
- Para B, criar id, retornar, processar incrementalmente em paralelo e consultar estado.

**Fluxos alternativos e exceções**
- B pendente responde 425 imediatamente, tratado visualmente como progresso.
- A filtra usuário; root só administra ativos e não acessa finanças.
- Falha não deixa fatos parciais; id inexistente responde 404.

**Diagramas** (opcional)
- ❓ LACUNA: diagrama de sequência ainda não produzido.

### 5. Contratos públicos (assinaturas, endpoints, headers, exemplos)
**Crédito**
- Tipo: http_endpoint
- Assinatura/Rota: `POST /contacorrente/credito`
- Método: POST
- Semântica de status/headers: JSON `{"valor":12.42,"descricao":"alguma coisa","data":"2020-02-28"}`; 2xx sem corpo; 400 inválido; 401/403 na opção A; 409 regra de domínio.

**Débito**
- Tipo: http_endpoint
- Assinatura/Rota: `POST /contacorrente/debito`
- Método: POST
- Semântica de status/headers: mesmo corpo e semântica do crédito; valor positivo no request reduz saldo.

**Compra**
- Tipo: http_endpoint
- Assinatura/Rota: `POST /movimentacao/compra`
- Método: POST
- Semântica de status/headers: JSON `{"ativo":"ATIVO1","data":"2020-02-28","quantidade":2.5,"valor":105.53}`; 2xx sem corpo; 400, 401/403 e 409 conforme regra.

**Venda**
- Tipo: http_endpoint
- Assinatura/Rota: `POST /movimentacao/venda`
- Método: POST
- Semântica de status/headers: mesmo corpo da compra; quantidade insuficiente retorna 409; 2xx sem corpo em sucesso.

**Saldo**
- Tipo: http_endpoint
- Assinatura/Rota: `GET /contacorrente/saldo?data=2020-02-28`
- Método: GET
- Semântica de status/headers: 200 com `{"saldo":1234.56}`; 400 para data inválida; 401/403 na opção A.

**Posição assíncrona**
- Tipo: http_endpoint
- Assinatura/Rota: `GET /posicao?data=2020-02-28` e `GET /posicao/42`
- Método: GET
- Semântica de status/headers: criação retorna `{"id":42}`; consulta retorna 425 enquanto pendente e 200 com conteúdo ao terminar; resultado é descartado após execução.

### 6. Erros, exceções e fallback
- Matriz: sem Basic -> 401; papel sem capacidade -> 403; JSON inválido -> 400; saldo/posição -> 409; execução ausente -> 404; banco indisponível -> 503/500 conforme contrato de operação.
- Estratégias de resiliência: timeout, backpressure e retry somente em GET seguro; POST exige proteção contra repetição.
- Política de fallback: não degradar para dados de outro usuário nem trocar 425 por erro.
- Invariantes: rotas/payloads exatos; seed completo; isolamento; thread-safety; memória B constante; resultado descartável.

### 7. Observabilidade
**Métricas**
- Status/latência por rota, 401/403/409, contenção SQLite, tamanho da fila, 425, tempo de execução, heap e grau de paralelismo.

**Logs**
- JSON com correlation id, execution id, rota, papel, resultado e duração; nunca senha ou Basic bruto.

**Tracing**
- Request, autorização, transação, consulta incremental e tarefas paralelas; amostragem TBD.

**Dashboards e alertas**
- Erros por rota, crescimento da fila, memória não constante, contenção e vazamento de isolamento.

### 8. Dependências e compatibilidade
| Componente | Versão mínima | Observações |
|---|---|---|
| Java/Maven | Java 8; Maven conforme projeto | stack aprovada usa Java 25 |
| Docker | runtime compatível | cloud adiado; standalone obrigatório |

**Garantias de compatibilidade**
- Rotas e exemplos acima são literais. Alteração de caminho, campo, status ou corpo exige revisão do PRD/FDD e auditoria.

### 9. Critérios de aceite técnicos
- As cinco rotas síncronas respondem aos caminhos e payloads exatos; cada POST não exige corpo de resposta.
- Seed contém exatamente `ATIVO0` a `ATIVO127` com mercado em 2020-01-02.
- A exige Basic em toda requisição, cria os dez usuários e root, aplica isolamento e impede root de transacionar/consultar privado.
- B retorna id, 425 enquanto pendente, 200 terminado, descarta conteúdo, processa centenas de milhares com memória constante e paralelismo demonstrado.
- Concorrência não viola saldo, posição ou isolamento.
- Dockerfile e docker-compose permitem execução standalone; deploy cloud permanece adiado.
- Cada endpoint tem integração com fixture isolada e reexecutável por limpeza, rollback ou reset entre casos, com evidência de request/response publicada. Isso não torna o POST semanticamente idempotente.
- FDD requer revisão e aprovação de Yoda antes de código.

### 10. Riscos e mitigação
### Basic expõe isolamento incorreto
- **Probabilidade:** média
- **Impacto:** vazamento de dados privados.
- **Mitigação:** autorização central, testes cruzados e logs sem credenciais.
- **Plano de contingência:** desabilitar opção A até correção auditada.

### Memória B cresce com o banco
- **Probabilidade:** média
- **Impacto:** indisponibilidade.
- **Mitigação:** JDBC incremental, heap profiling e carga com centenas de milhares.
- **Plano de contingência:** interromper execuções novas e preservar somente ids.
