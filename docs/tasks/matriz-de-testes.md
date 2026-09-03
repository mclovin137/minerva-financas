# Matriz de testes e rastreabilidade

Versão: 1.0 — 2026-09-02  
Dono: Patrick Jane — execução: Severino

Esta matriz fecha requisito → critério → caso → evidência. Cada caso é independente, usa dados exclusivos com prefixo do próprio TC, executa em qualquer ordem e repetidamente, limpa seus dados por rollback ou reset de fixture e publica request, response, status e relatório no pipeline `testes-integracao`, em `artifacts/testes/<commit>/<tc-id>/`; o PR referencia o caminho. Os casos marcados com concorrência devem registrar as threads e o resultado agregado.

| ID | Requisito de origem | Critério verificável | Fixture própria / limpeza | Evidência publicada |
|---|---|---|---|---|
| TC-001 | FR-001 / `/credito` | crédito válido retorna 201 sem corpo | conta TC-001 / reset | request-response / `artifacts/testes/<commit>/TC-001/` |
| TC-002 | FR-001 / `/debito` | débito válido reduz saldo e retorna 201 | conta TC-002 / reset | idem TC-002 |
| TC-003 | FR-001 | saldo atualizado retorna 200 e JSON | conta TC-003 / reset | idem TC-003 |
| TC-004 | FR-001 | débito que zera saldo é aceito | conta TC-004 / reset | idem TC-004 |
| TC-005 | FR-001 | saldo insuficiente retorna 409 sem efeito | conta TC-005 / reset | idem TC-005 |
| TC-006 | FR-001 | valor ausente, zero, negativo ou não numérico retorna 400 | conta TC-006 / reset | idem TC-006 |
| TC-007 | FR-001 | descrição ausente retorna 400 | conta TC-007 / reset | idem TC-007 |
| TC-008 | FR-001 / concorrência | débitos concorrentes não deixam saldo negativo | conta TC-008 / reset | threads + request-response |
| TC-009 | FR-002 / `POST /ativos` | ativo válido retorna 201 sem corpo | ativo TC-009 / reset | idem TC-009 |
| TC-010 | FR-002 / `GET /ativos` | lista contém ativo criado | ativo TC-010 / reset | idem TC-010 |
| TC-011 | FR-002 / `GET /ativos/{ativo}` | ativo existente retorna 200 | ativo TC-011 / reset | idem TC-011 |
| TC-012 | FR-002 / `PUT /ativos/{ativo}` | alteração válida retorna 200 atualizado | ativo TC-012 / reset | idem TC-012 |
| TC-013 | FR-002 / `DELETE /ativos/{ativo}` | remoção válida retorna 204 | ativo TC-013 / reset | idem TC-013 |
| TC-014 | FR-002 | ativo inexistente retorna 404 | ativo TC-014 / reset | idem TC-014 |
| TC-015 | FR-002 | tipo inválido, campo ausente ou preço > 8 casas retorna 400 | ativo TC-015 / reset | idem TC-015 |
| TC-016 | FR-002 | duplicidade ou remoção com movimento retorna 409; concorrência preserva integridade | ativo TC-016 / reset | idem TC-016 |
| TC-017 | FR-003 / `/compra` | compra válida retorna 201 sem corpo | conta/ativo TC-017 / reset | idem TC-017 |
| TC-018 | FR-003 / `/venda` | venda válida retorna 201 sem corpo | conta/ativo TC-018 / reset | idem TC-018 |
| TC-019 | FR-003 | compra debita e venda credita atomicamente | conta/ativo TC-019 / reset | request-response + saldo |
| TC-020 | FR-003 | compra sem saldo retorna 409 sem lançamento | conta/ativo TC-020 / reset | idem TC-020 |
| TC-021 | FR-003 | venda acima da quantidade retorna 409 sem efeito parcial | conta/ativo TC-021 / reset | idem TC-021 |
| TC-022 | FR-003 | ativo inexistente retorna 404 | ativo TC-022 / reset | idem TC-022 |
| TC-023 | FR-003 | quantidade zero, negativa ou >2 casas retorna 400 | ativo TC-023 / reset | idem TC-023 |
| TC-024 | FR-003 / concorrência | vendas concorrentes não vendem quantidade duas vezes | ativo TC-024 / reset | threads + request-response |
| TC-025 | FR-004 / `/posicao` | posição válida retorna 200 por ativo | posição TC-025 / reset | idem TC-025 |
| TC-026 | FR-004 | quantidade é compras menos vendas | posição TC-026 / reset | idem TC-026 |
| TC-027 | FR-004 | valor de mercado é quantidade × preço | posição TC-027 / reset | idem TC-027 |
| TC-028 | FR-004 | preço médio usa média ponderada pela quantidade | compras 1×10 e 3×20 TC-028 / reset | cálculo + response |
| TC-029 | FR-004 | soma intermediária exata precede conversão | compras com escalas TC-029 / reset | cálculo + response |
| TC-030 | FR-004 / FR-007 | floor ocorre somente no quociente reduzido | divisão fracionária TC-030 / reset | cálculo + response |
| TC-031 | FR-004 | `rendimento = preco_mercado ÷ preco_medio` e lucro = vendas − compras | compra/venda TC-031 / reset | cálculo + response |
| TC-032 | FR-004 | ativo sem compra não divide por zero e segue contrato | ativo TC-032 / reset | response |
| TC-033 | FR-005 / preço | preço de mercado por data válido retorna 201/200 conforme FDD-002 | preço TC-033 / reset | request-response |
| TC-034 | FR-005 | preço histórico mais recente <= corte é escolhido | preços TC-034 / reset | response + cálculo |
| TC-035 | FR-005 | excluir preço histórico válido retorna 204 | preço TC-035 / reset | request-response |
| TC-036 | FR-005 | emissão anterior ao vencimento é aceita | ativo TC-036 / reset | idem TC-036 |
| TC-037 | FR-005 | emissão igual/posterior ao vencimento retorna 400 | ativo TC-037 / reset | idem TC-037 |
| TC-038 | FR-005 | data/preço inválido retorna 400 | preço TC-038 / reset | idem TC-038 |
| TC-039 | FR-005 | ativo/preço inexistente retorna 404 | preço TC-039 / reset | idem TC-039 |
| TC-040 | FR-005 / concorrência | preços concorrentes não criam estado inconsistente | preço TC-040 / reset | threads + response |
| TC-041 | FR-006 / lançamentos | filtro inclusive retorna lançamentos nos dois limites | fatos TC-041 / reset | request-response |
| TC-042 | FR-006 / movimentações | filtro inclusive retorna movimentos nos dois limites | fatos TC-042 / reset | idem TC-042 |
| TC-043 | FR-006 | filtro ausente retorna 400 | consulta TC-043 / reset | idem TC-043 |
| TC-044 | FR-006 | intervalo invertido retorna 400 | consulta TC-044 / reset | idem TC-044 |
| TC-045 | FR-006 | data não parseável retorna 400 | consulta TC-045 / reset | idem TC-045 |
| TC-046 | FR-006 | emissão é inclusiva e vencimento exclusivo | ativo/movimentos TC-046 / reset | response |
| TC-047 | FR-006 | fim de semana fora da regra retorna 409/400 conforme FDD-002 | movimento TC-047 / reset | response |
| TC-048 | FR-006 | inserção fora de ordem preserva não negatividade histórica | fatos TC-048 / reset | série temporal + response |
| TC-049 | FR-007 | dinheiro com até 2 casas é aceito | valores TC-049 / reset | response |
| TC-050 | FR-007 | dinheiro com >2 casas retorna 400 | valores TC-050 / reset | response |
| TC-051 | FR-007 | preço com até 8 casas é aceito | preço TC-051 / reset | response |
| TC-052 | FR-007 | preço com >8 casas retorna 400 | preço TC-052 / reset | response |
| TC-053 | FR-007 | quantidade com até 2 casas é aceita | movimento TC-053 / reset | response |
| TC-054 | FR-007 | quantidade com >2 casas retorna 400 | movimento TC-054 / reset | response |
| TC-055 | FR-007 | sinais inválidos, overflow e não finito retornam 400 | parser TC-055 / reset | response |
| TC-056 | FR-007 | cálculos fracionários aplicam floor determinístico | cálculo TC-056 / reset | cálculo + response |
| TC-057 | FR-008 | cinco rotas canônicas aceitam payload literal | contrato TC-057 / reset | contrato request-response |
| TC-058 | FR-008 | POSTs respondem status adequado e corpo vazio | contrato TC-058 / reset | headers/body |
| TC-059 | FR-008 | saldo responde `{"saldo":1234.56}` | saldo TC-059 / reset | request-response |
| TC-060 | FR-008 | JSON malformado/caminho inválido retorna 400 | contrato TC-060 / reset | response |
| TC-061 | FR-008 | conflito financeiro retorna 409 | contrato TC-061 / reset | response |
| TC-062 | FR-008 | falha interna não revela dado sensível | servidor TC-062 / reset | response + log sanitizado |
| TC-063 | FR-008 / concorrência | 20 débitos simultâneos preservam invariantes | fixture de 20 débitos TC-063 / reset | threads + relatório |
| TC-064 | FR-008 | seed contém ATIVO0–ATIVO127 com preço inicial | seed TC-064 / reset | contagem + response |
| TC-065 | FR-009 | Basic válido permite usuário comum | usuário TC-065 / reset | request-response |
| TC-066 | FR-009 | Basic ausente/inválido retorna 401 | auth TC-066 / reset | response |
| TC-067 | FR-009 | root administra ativo e preço | root TC-067 / reset | response |
| TC-068 | FR-009 | usuário comum sem capacidade administrativa retorna 403 | auth TC-068 / reset | response |
| TC-069 | FR-009 | root não transaciona nem consulta privado | root TC-069 / reset | response |
| TC-070 | FR-009 | usuário A não vê dados de usuário B | dois usuários TC-070 / reset | response cruzado |
| TC-071 | FR-009 | todas as rotas de API exigem Basic | matriz de rotas TC-071 / reset | respostas 401 |
| TC-072 | FR-009 / concorrência | operações concorrentes mantêm isolamento e saldo | dois usuários TC-072 / reset | threads + relatório |
| TC-073 | FR-010 | criação async retorna 202 e id | execução TC-073 / reset | request-response |
| TC-074 | FR-010 | consulta pendente retorna 425 imediatamente | execução TC-074 / reset | timestamps + response |
| TC-075 | FR-010 | execução concluída retorna 200 com posição | execução TC-075 / reset | response |
| TC-076 | FR-010 | id inexistente ou já descartado retorna 404 | execução TC-076 / reset | response |
| TC-077 | FR-010 | falha da execução não é mascarada como 425 | execução falha TC-077 / reset | status + log |
| TC-078 | FR-010 / D3 | 200.000 movimentações são processadas | fixture determinística TC-078 / reset | relatório de carga |
| TC-079 | FR-010 / D3 | `Runtime` antes/depois varia menos de 64 MB | mesma fixture TC-079 / reset | heap antes/depois |
| TC-080 | FR-010 / D3 | mínimo de 2 threads é observável | mesma fixture TC-080 / reset | threads + relatório |

## Regras de execução

- Os TCs de cada endpoint são testes de integração REST + persistência; endpoints são sempre aplicáveis à regra 7.
- O pipeline `testes-integracao` deve executar a matriz em ordem aleatória e repetição limpa; o pipeline `review` valida links, escopo e ausência de código nesta entrega.
- Para TC-079, coletar `Runtime.getRuntime().totalMemory() - freeMemory()` imediatamente antes e depois da consulta, registrar ambos os valores e comparar a diferença absoluta com 64 MB.
- Para TC-080, registrar nomes/IDs de threads ativos durante o processamento e exigir pelo menos dois distintos.
