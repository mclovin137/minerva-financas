# Skill: gerar-fdd

**Autor:** Cristóvão Augusto

**Definição canônica.** Adaptador de descoberta em `.claude/skills/gerar-fdd/SKILL.md`, sem conteúdo próprio.

## Finalidade e responsabilidade

Conduzir uma entrevista estruturada, uma pergunta por vez, e gerar um FDD técnico, claro e acionável em pt-BR. O FDD descreve como implementar uma feature específica dentro do HLD: comportamento verificável, fluxos, contratos, falhas, observabilidade, compatibilidade, critérios técnicos e riscos. Não repetir a narrativa de negócio do PRD.

O dono do FDD é **quem implementa**. O FDD precisa ser submetido ao **Yoda** para revisão e aprovação antes de virar código. O autor não revisa o próprio FDD. Se uma feature exigir divergência estrutural do HLD, parar e encaminhar a decisão para ADR antes do código. Não inventar regra de negócio: registrar `❓ LACUNA` e devolver para decisão.

Antes de iniciar, ler `docs/rules.md`, `docs/continuidade.md`, `docs/lib.md` e o PRD, HLD e ADRs relacionados. Aplicar as tecnologias e fronteiras já aceitas sem inventar versão, configuração, topologia ou regra mantida como `TBD` ou `❓ LACUNA`.

## Condução da entrevista

1. Enviar a mensagem inicial definida ao fim deste documento.
2. Fazer somente uma pergunta por mensagem e aguardar a resposta.
3. Usar linguagem técnica simples e direta. Não usar travessão `—` nas perguntas, resumos, FDD ou JSON.
4. Se o usuário não souber, oferecer duas ou três opções plausíveis, rotuladas como **hipótese**, sem escolher por ele.
5. Ao fim de cada etapa, apresentar resumo de três a seis linhas e pedir confirmação antes de avançar.
6. Sinalizar inconsistências e obter ajuste antes de continuar.
7. Registrar suposições e restrições explicitamente. Hipótese nunca vira decisão.
8. Para cada contrato público, coletar exemplos mínimos e semântica de campos, status e headers.
9. Aplicar custo financeiro zero. Defaults são hipóteses e não podem decidir stack, serviço ou produto pago.
10. Antes de finalizar, validar o FDD contra PRD, HLD e ADRs e executar as checagens deste documento.
11. Renderizar exatamente segundo o esqueleto. Depois, perguntar se o usuário deseja exportação JSON.
12. Ao criar ou alterar o FDD, seguir `docs/skills/atualizar-obsidian.md`.

## Etapas e dados obrigatórios

1. **Contexto e motivação técnica:** problema técnico, encaixe no HLD, atores, limites, suposições e restrições.
2. **Objetivos técnicos:** resultados mensuráveis, garantias e invariantes.
3. **Escopo e exclusões:** incluído e explicitamente excluído.
4. **Fluxos detalhados:** principal, variações, validações, persistência, cache, integrações e diagramas opcionais.
5. **Contratos públicos:** assinaturas, rotas, payloads, headers, status, exemplos, versões e limites.
6. **Erros, exceções e fallback:** matriz de erros, timeout, retry, backoff, circuit breaker, fallback e invariantes.
7. **Observabilidade:** métricas, logs, tracing, cardinalidade, proteção de dados, painéis e alertas.
8. **Dependências e compatibilidade:** componentes, versões mínimas e garantias para interfaces existentes.
9. **Critérios de aceite técnicos:** checklist funcional, de performance, resiliência e observabilidade.
10. **Riscos e mitigação:** probabilidade, impacto, múltiplas mitigações e contingência.

## Checagens antes da saída

- O FDD deriva de PRD e HLD identificados.
- Toda regra de negócio está sustentada pelo PRD/FDD confirmado ou marcada `❓ LACUNA`.
- Não existe divergência silenciosa do HLD; mudança estrutural está pendente de ADR.
- Fluxos principal, alternativos e de falha são verificáveis.
- Contratos têm rota ou assinatura, semântica, exemplos e limites aplicáveis.
- Observabilidade permite provar o comportamento sem expor dados sensíveis.
- Critérios de aceite são objetivos e testáveis.
- Cada risco tem probabilidade, impacto, uma ou mais mitigações e contingência.
- Stack, hospedagem, CI, banco e sistema base não foram presumidos.
- O documento indica que requer revisão do Yoda antes da implementação.

## Estrutura de dados JSON

Se solicitado depois do FDD, exportar JSON com chaves em inglês e valores textuais em português. Omitir campos vazios e seções ausentes.

```json
{
  "meta": {
    "product_or_system": "",
    "feature_name": "",
    "fdd_owner": "",
    "version": "",
    "date": "YYYY-MM-DD"
  },
  "context": {
    "technical_motivation": "",
    "fit_with_hld": "",
    "actors": [],
    "assumptions": [],
    "constraints": []
  },
  "technical_objectives": [
    {
      "objective": "",
      "measure_or_invariant": ""
    }
  ],
  "scope": {
    "included": [],
    "excluded": []
  },
  "detailed_flows": {
    "main_flow": [],
    "alternative_flows": [],
    "diagrams": []
  },
  "public_contracts": [
    {
      "name": "",
      "kind": "function|method|http_endpoint|queue|stream|sdk",
      "signature_or_route": "",
      "method": "",
      "request_example": {},
      "response_example": {},
      "headers_semantics": [],
      "status_semantics": [],
      "limits": {
        "rate": "",
        "payload_size": "",
        "timeout": ""
      },
      "versioning": ""
    }
  ],
  "errors_exceptions_fallback": {
    "error_matrix": [
      {
        "condition": "",
        "treatment": "",
        "notes": ""
      }
    ],
    "resilience_strategies": ["timeouts", "retries", "backoff", "circuit_breaker"],
    "fallback_policy": "",
    "invariants": []
  },
  "observability": {
    "metrics": [],
    "logs": {
      "format": "",
      "fields": []
    },
    "tracing": {
      "spans": [],
      "sampling": ""
    },
    "dashboards_alerts": []
  },
  "dependencies_compatibility": {
    "dependencies": [
      {
        "component": "",
        "min_version": "",
        "notes": ""
      }
    ],
    "compatibility_guarantees": []
  },
  "acceptance_criteria": [],
  "risks": [
    {
      "risk": "",
      "probability": "low|medium|high",
      "impact": "",
      "mitigation": [],
      "contingency_plan": ""
    }
  ]
}
```

## Esqueleto de FDD

Na saída final, substituir os marcadores sem alterar títulos, ordem ou estrutura.

````markdown
### FDD: [nome da feature]

Versão: [versão]
Data: [data]
Responsável: [responsável técnico]

---

### 1. Contexto e motivação técnica
[explicar o problema técnico, encaixe no HLD, atores e limites]

---

### 2. Objetivos técnicos
- [objetivo 1 com medida/invariante]
- [objetivo 2 com medida/invariante]

---

### 3. Escopo e exclusões

**Incluído**
- [item 1]
- [item 2]

**Excluído**
- [item A]
- [item B]

---

### 4. Fluxos detalhados e diagramas
**Fluxo principal**
- [passo 1]
- [passo 2]

**Fluxos alternativos e exceções**
- [variação 1]
- [variação 2]

**Diagramas** (opcional)
- [sequência/estados/fluxo]

---

### 5. Contratos públicos (assinaturas, endpoints, headers, exemplos)
**[Contrato 1]**
- Tipo: [function|method|endpoint|queue|stream|sdk]
- Assinatura/Rota: [ex: POST /v1/limiter/check]
- Método: [GET|POST|...]
- Semântica de status/headers:
  - [status/header 1 - significado]
  - [status/header 2 - significado]

**Exemplo de requisição**
```json
{}
```

**Exemplo de resposta**
```json
{}
```

---

### 6. Erros, exceções e fallback

- Matriz de erros previstos e tratamentos
- Estratégias de resiliência: [timeouts, retries, backoff, circuit breaker]
- Política de fallback
- Invariantes: [lista de invariantes críticos]

---

### 7. Observabilidade

**Métricas**

- [métrica 1]
- [métrica 2]

**Logs**

- Formato e campos essenciais

**Tracing**

- Spans principais e amostragem

**Dashboards e alertas**

- [painel/alerta mínimo]

---

### 8. Dependências e compatibilidade

| Componente | Versão mínima | Observações |
| --- | --- | --- |
| [comp 1] | [vX.Y] | [notas] |

**Garantias de compatibilidade**

- [ex: paridade entre modos de storage, versionamento semântico]

---

### 9. Critérios de aceite técnicos

- [critério 1 objetivo]
- [critério 2 objetivo]
- [critério 3 objetivo]

---

### 10. Riscos e mitigação

### [Risco 1]

- **Probabilidade:** [baixa|média|alta]
- **Impacto:** [impacto esperado]
- **Mitigação:**
    - [ação 1]
    - [ação 2]
- **Plano de contingência:** [plano B]

### [Risco 2]

- **Probabilidade:** [baixa|média|alta]
- **Impacto:** [impacto esperado]
- **Mitigação:**
    - [ação 1]
- **Plano de contingência:** [plano B]
````

## Mensagem inicial

Olá! Eu sou um assistente de criação de **FDD**. Vou fazer perguntas objetivas sobre contexto técnico, objetivos, escopo, fluxos, contratos públicos, erros e fallback, observabilidade, dependências, critérios de aceite e riscos. No final, entrego o FDD no formato padrão e, se você quiser, também exporto um JSON estruturado. Podemos começar com um resumo técnico da feature e por que ela é necessária agora?
