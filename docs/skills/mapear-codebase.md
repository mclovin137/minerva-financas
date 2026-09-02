# Skill: mapear-codebase

**Autor:** Cristóvão Augusto

**Definição canônica e independente de ferramenta.** Adaptador de descoberta em `.claude/skills/mapear-codebase/SKILL.md`, sem conteúdo próprio.

## Para o futuro agente

Reconstruir, somente por leitura, o comportamento e a arquitetura observável de um repositório existente. Usar antes de refatoração ou migração, ou quando for necessário inventariar funcionalidades, entry points, componentes, integrações, stack, testes, carga de manutenção e lacunas. A atividade pertence à responsabilidade de **planejar/revisar**.

Não usar para revisar diff ou PR, criar HLD, decidir arquitetura ou alterar qualquer arquivo. A skill `repo-scan`, quando disponível, pode complementar a classificação de ativos, código vendorizado e peso morto; ela não substitui este mapeamento funcional e arquitetural.

## Restrições

- Operar estritamente em modo read-only: não criar, editar, remover, formatar nem salvar arquivos; não executar build, testes, migrations, instalação, upgrade ou comandos que alterem caches e artefatos.
- Produzir todo conteúdo autoral em pt-BR.
- Sustentar afirmações factuais com caminho relativo e linha, por exemplo `src/modulo.ts:42`. Quando uma conclusão combinar evidências, citar todas as relevantes e identificá-la como inferência.
- Não tratar documentação como prova suficiente quando o código ou a configuração a contradisser.
- Excluir binários, arquivos gerados, dependências vendorizadas, caches e artefatos de build da leitura detalhada; registrar as exclusões.
- Não exigir MCP, acesso de rede nem varredura literal de todos os arquivos. Declarar cobertura, amostragem e limitações.
- Persistir o relatório somente em uma ação posterior, com autorização explícita do usuário.

## Profundidade e cobertura

Escolher a menor profundidade que responda à demanda e declará-la:

- `RÁPIDA`: estrutura, manifestos, documentação, entry points e módulos principais.
- `PADRÃO`: rápida mais rotas, casos de uso, integrações, persistência e testes representativos.
- `PROFUNDA`: padrão mais relações entre componentes, fluxos ponta a ponta e indicadores de manutenção nas áreas em escopo.

Se o usuário delimitar pastas, respeitar esse escopo. Caso contrário, usar a raiz acessível. No início do relatório, informar raiz, profundidade, tipos de artefato inspecionados, exclusões e áreas inacessíveis. Cobertura é declarada, nunca presumida como completa.

## Procedimento

1. Ler `docs/rules.md`, `docs/continuidade.md` e `docs/lib.md` quando estiver no projeto Minerva e identificar a responsabilidade exercida.
2. Delimitar raiz, profundidade, exclusões e limites de acesso.
3. Inventariar linguagens, manifestos, lockfiles, build, infraestrutura, CI/CD, observabilidade e diretórios relevantes.
4. Localizar entry points em rotas, controllers, handlers, comandos CLI, jobs, schemas, serviços RPC, eventos e composição da aplicação.
5. Derivar funcionalidades primárias e secundárias a partir de fluxos executáveis e seus pré-requisitos.
6. Mapear componentes, fronteiras, dependências internas, persistência e integrações externas por imports, wiring, configuração e contratos.
7. Inspecionar testes e quality gates sem executá-los; distinguir unitário, integração, contrato e E2E.
8. Registrar indicadores verificáveis de manutenção, como arquivos extensos, alto acoplamento, ciclos evidenciados, mocks excessivos, forks ou concentração de responsabilidades. Não atribuir gravidade sem evidência.
9. Separar fatos observados, inferências, premissas e desconhecidos.
10. Emitir o relatório determinístico abaixo. Usar diagramas textuais somente quando tornarem um fluxo ou limite materialmente mais claro.

## Estado sem implementação

Se não houver código ou configuração executável, não retornar erro artificial. Relatar somente a governança e a documentação observáveis, declarar que funcionalidades, arquitetura de runtime, stack e testabilidade ainda não podem ser mapeadas e listar o que falta. No estado inicial do projeto Minerva, não inferir stack a partir de `.idea/` nem transformar decisões ainda não registradas pela aplicação consumidora em fatos.

## Formato de saída

```markdown
# Relatório de Inteligência do Sistema do Projeto

Data da análise: AAAA-MM-DD
Raiz analisada: <caminho>
Profundidade: RÁPIDA | PADRÃO | PROFUNDA
Cobertura declarada: <artefatos e áreas inspecionados>
Exclusões e limites: <itens>

## 1. Resumo
<propósito observado, módulos e achados principais>

## 2. Funcionalidades primárias
| Funcionalidade | Descrição | Entry points | Quando usar | Pré-condições e dependências | Evidências |
| --- | --- | --- | --- | --- | --- |

## 3. Funcionalidades secundárias
| Funcionalidade | Descrição | Suporta | Observações | Evidências |
| --- | --- | --- | --- | --- |

## 4. Arquitetura observada
<camadas, limites, dados e fluxos; marcar inferências>

## 5. Componentes principais e relações
| Componente | Caminhos principais | Depende de | Usado por | Observações | Evidências |
| --- | --- | --- | --- | --- | --- |

## 6. Inventário da stack
| Camada | Tecnologia | Versão ou fonte de configuração | Finalidade | Evidências |
| --- | --- | --- | --- | --- |

## 7. Testabilidade e quality gates
| Camada de teste | Ferramenta | Escopo observado | Sinais de cobertura | Lacunas ou riscos | Evidências |
| --- | --- | --- | --- | --- | --- |

## 8. Dependências e serviços externos
| Dependência ou serviço | Uso observado | Finalidade | Observações | Evidências |
| --- | --- | --- | --- | --- |

## 9. Indicadores de manutenção
| Indicador | Evidência | Efeito esperado | Direção de investigação |
| --- | --- | --- | --- |

## 10. Notas de integração
<adapters, SDKs, clientes gerados e limites de configuração>

## 11. Premissas e desconhecidos
- <premissa ou desconhecido, efeito e evidência disponível>

## 12. Observações adicionais
<itens objetivos não cobertos acima>
```

Omitir linhas de tabela sem evidência, mas manter todas as seções. Para seção sem achados, escrever `Nenhum item verificável no escopo analisado.`
