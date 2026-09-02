# Skill: auditar-dependencias

**Autor:** Cristóvão Augusto

**Definição canônica e independente de ferramenta.** Adaptador de descoberta em `.claude/skills/auditar-dependencias/SKILL.md`, sem conteúdo próprio.

## Para o futuro agente

Auditar dependências de forma estritamente read-only antes de inclusão ou upgrade, no pré-release ou go-live, periodicamente, ou diante de alerta de vulnerabilidade, licença ou manutenção. A atividade pertence à responsabilidade de **planejar/revisar**, normalmente exercida pelo agente de Segurança.

Esta skill avalia o inventário de pacotes. `security-review` avalia código e controles; `search-first` pesquisa alternativas antes da adoção; `auditoria` revisa uma entrega e seu diff; `repo-scan` identifica inclusive bibliotecas incorporadas ao código. Uma não substitui a outra.

## Restrições

- Não alterar arquivos, instalar pacotes, executar upgrade, gerar lockfile, aplicar patch ou prescrever migration. Recomendar remediações é permitido; aplicá-las não é.
- Não executar builds, testes, migrations ou comandos de pacote que modifiquem caches, ambiente ou resolução.
- Produzir todo conteúdo autoral em pt-BR e citar evidências locais por caminho relativo e linha.
- Não escrever no Obsidian durante a auditoria. A auditoria é read-only; uma mudança posterior autorizada aciona separadamente `atualizar-obsidian`.
- Não exigir MCP específico. Usar pesquisa web ou outra consulta read-only disponível.
- Considerar a regra de custo financeiro zero do projeto Minerva. Dependência paga, licença incompatível ou solução cuja única remediação imponha custo deve voltar ao usuário como decisão, nunca ser adotada.

## Evidência externa obrigatória

Versão estável atual, CVE ou advisory, manutenção e licença são informações temporais. Pesquisar na data da auditoria e citar fontes primárias ou autoritativas:

- registry oficial do ecossistema para versão e metadados;
- repositório oficial e release notes para manutenção, depreciação e breaking changes;
- OSV, NVD ou GitHub Security Advisories para vulnerabilidades;
- arquivo `LICENSE` oficial e identificadores SPDX para licença.

Registrar URL e data de consulta no relatório. Não fabricar CVE, CVSS, versão, estado de manutenção, licença ou breaking change. Se a fonte não estiver acessível ou não confirmar o fato, mover o item para `Dependências não verificadas`, explicando a limitação. Mais de um ano sem release ou atividade relevante é somente indicador contextual, não prova isolada de abandono.

## Inventário e semântica de versão

- Identificar todos os ecossistemas e arquivos de dependência no escopo.
- Separar dependências diretas das transitivas. Auditar transitivas somente em seção própria e quando lockfile, SBOM, scanner ou advisory fornecer evidência.
- Distinguir:
  - `declarada`: constraint escrita no manifesto;
  - `resolvida`: versão fixada em lockfile ou outra evidência local reproduzível;
  - `estável atual`: versão confirmada em fonte oficial na data da consulta.
- Ausência de lockfile impede afirmar a versão resolvida. Constraint aberta não equivale à versão instalada.
- Avaliar compatibilidade de licença considerando a licença e a forma de distribuição do projeto; se qualquer uma estiver indefinida, registrar a análise como inconclusiva.
- Mencionar breaking changes somente com release notes oficiais e apenas quando relevantes à recomendação.

## Procedimento

1. Ler `docs/rules.md`, `docs/continuidade.md` e `docs/lib.md` quando estiver no projeto Minerva e delimitar raiz, subpastas e ecossistemas.
2. Localizar manifestos, lockfiles, catálogos, SBOMs e arquivos de licença sem executar resolução.
3. Construir o inventário de dependências diretas e, separadamente, transitivas evidenciadas.
4. Localizar usos no código e configurações, citando caminho e linha.
5. Para cada dependência, pesquisar versão estável, manutenção, depreciação, advisories e licença nas fontes definidas.
6. Correlacionar advisories com a versão **resolvida**. Sem versão resolvida, registrar exposição potencial ou verificação inconclusiva, nunca vulnerabilidade confirmada.
7. Classificar risco como `CRÍTICO`, `ALTO`, `MÉDIO` ou `BAIXO`, explicando impacto e evidência. Manter `NÃO VERIFICADO` fora da escala de risco.
8. Selecionar até 10 arquivos mais críticos que realmente usem dependências arriscadas; se houver menos, listar somente os existentes.
9. Recomendar ações priorizadas sem modificar o projeto, sem estimativas de prazo e sem assumir compatibilidade.
10. Emitir o relatório determinístico abaixo.

## Estado sem manifestos

Se nenhum manifesto ou inventário existir, informar que não há dependências auditáveis no escopo, listar os caminhos e padrões procurados e solicitar o manifesto ou aguardar a decisão de stack. Isso não é falha de segurança. No estado inicial do projeto Minerva, não inferir dependências a partir de `.idea/` nem pesquisar pacotes hipotéticos.

## Formato de saída

```markdown
# Relatório de Auditoria de Dependências

Data da auditoria: AAAA-MM-DD
Raiz analisada: <caminho>
Ecossistemas: <lista ou nenhum identificado>
Manifestos e lockfiles: <caminhos>
Cobertura e limitações: <escopo, fontes inacessíveis e lacunas>

## 1. Resumo
<inventário e achados principais>

## 2. Questões críticas
| Severidade | Dependência | Questão | Versão afetada | Evidência local | Fonte externa |
| --- | --- | --- | --- | --- | --- |

## 3. Dependências diretas
| Dependência | Declarada | Resolvida | Estável atual | Manutenção | Licença | Status | Evidências e fontes |
| --- | --- | --- | --- | --- | --- | --- | --- |

## 4. Dependências transitivas evidenciadas
| Dependência | Resolvida | Origem da evidência | Questão | Fonte externa |
| --- | --- | --- | --- | --- |

## 5. Análise de risco
| Severidade | Dependência | Vulnerabilidade, licença ou manutenção | Impacto | Evidências e fontes |
| --- | --- | --- | --- | --- |

## 6. Dependências não verificadas
| Dependência | Versão conhecida | Item não verificado | Motivo |
| --- | --- | --- | --- |

## 7. Arquivos críticos
| Arquivo | Dependência arriscada | Por que é crítico | Evidências |
| --- | --- | --- | --- |

## 8. Notas de integração
<como dependências relevantes são usadas, acoplamento e pontos únicos de falha>

## 9. Compatibilidade de licença
<licença do projeto, forma de distribuição, compatibilidades e incertezas>

## 10. Plano de ação recomendado
1. <ação, prioridade, justificativa e validação necessária>

## 11. Fontes consultadas
- <fonte, URL, item sustentado e data de consulta>
```

Manter todas as seções. Quando não houver item verificável, escrever `Nenhum item verificável no escopo analisado.` Não converter ausência de pesquisa em conclusão de segurança.
