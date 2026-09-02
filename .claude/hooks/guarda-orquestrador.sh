#!/usr/bin/env bash
# PreToolUse — impede que a sessão principal (Oráculo, o Orquestrador)
# altere, crie ou apague arquivos. Subagentes passam livremente.
#
# Como distingue: o input do hook traz "agent_id" APENAS quando a chamada vem de
# dentro de um subagente. Sem agent_id = sessão principal = negado.
#
# Adaptador (regra de ferro 1): a regra canônica é docs/agentes/oraculo.md.
set -euo pipefail
export PATH="$HOME/.local/bin:$PATH"

entrada="$(cat)"

negar_sem_jq() {
  printf '%s\n' '{"hookSpecificOutput":{"hookEventName":"PreToolUse","permissionDecision":"deny","permissionDecisionReason":"BLOQUEADO — a guarda não conseguiu interpretar a entrada porque jq não está disponível."}}'
  exit 0
}

command -v jq >/dev/null 2>&1 || negar_sem_jq

negar_entrada_invalida() {
  jq -n '{
    hookSpecificOutput: {
      hookEventName: "PreToolUse",
      permissionDecision: "deny",
      permissionDecisionReason: "BLOQUEADO — a guarda não conseguiu interpretar a entrada JSON do PreToolUse."
    }
  }'
  exit 0
}

printf '%s' "$entrada" | jq -e 'type == "object"' >/dev/null 2>&1 || negar_entrada_invalida

agente="$(printf '%s' "$entrada" | jq -r '.agent_id // empty')"

# Subagente: libera sem olhar mais nada.
[ -n "$agente" ] && exit 0

ferramenta="$(printf '%s' "$entrada" | jq -r '.tool_name // empty')"

negar() {
  jq -n --arg motivo "$1" '{
    hookSpecificOutput: {
      hookEventName: "PreToolUse",
      permissionDecision: "deny",
      permissionDecisionReason: $motivo
    }
  }'
  exit 0
}

motivo_base=$(cat <<'TXT'
BLOQUEADO — a sessão principal é o Oráculo, o Orquestrador do projeto Minerva, e não altera, cria ou apaga arquivo (regra de ferro 4).

Delegue a mudança ao agente responsável, via subagente:
  Yoda (arquitetura/ADR) · Severino (código da aplicação e pipeline-as-code) · Patrick Jane (QA) · Neo (segurança) · Jarvis (SRE e pós-pipeline)

Contrato: docs/agentes/oraculo.md. Leitura, busca e inspeção continuam liberadas.
TXT
)

case "$ferramenta" in
  Write|Edit|NotebookEdit)
    negar "$motivo_base"
    ;;
  Bash)
    comando="$(printf '%s' "$entrada" | jq -r '.tool_input.command // empty')"
    if printf '%s' "$comando" | grep -Eq \
      -e '(^|[;&|(]|[[:space:]])(rm|mv|cp|mkdir|rmdir|touch|ln|chmod|chown|truncate|dd|tee|shred|unlink)([[:space:]]|$)' \
      -e '(sed|perl)[[:space:]]+-i' \
      -e 'git[[:space:]]+(add|commit|rm|mv|checkout|switch|reset|restore|apply|clean|push|merge|rebase|stash|init|tag)([[:space:]]|$)' \
      -e '(^|[^0-9&>])>{1,2}[[:space:]]*[^&[:space:]]'
    then
      negar "$motivo_base

Comando barrado por parecer mutação de arquivo:
  $comando"
    fi
    ;;
esac

exit 0
