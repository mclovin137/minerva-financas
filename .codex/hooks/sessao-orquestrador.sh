#!/usr/bin/env bash
# SessionStart — declara a sessão principal como Oráculo, o Orquestrador
# do projeto Minerva.
#
# Adaptador (regra de ferro 1): não contém regra própria. Injeta no contexto da
# sessão o conteúdo canônico de docs/agentes/oraculo.md. Este arquivo espelha
# .claude/hooks/sessao-orquestrador.sh para o runtime do Codex.
set -euo pipefail
export PATH="$HOME/.local/bin:$PATH"

repo="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
doc="$repo/docs/agentes/oraculo.md"

# Sem o documento canônico não há o que injetar. Sai em silêncio em vez de
# inventar a regra aqui dentro.
[ -f "$doc" ] || exit 0

jq -n --rawfile conteudo "$doc" '{
  hookSpecificOutput: {
    hookEventName: "SessionStart",
    additionalContext: ("[PAPEL DA SESSÃO PRINCIPAL — projeto Minerva]\n\n" + $conteudo)
  }
}'
