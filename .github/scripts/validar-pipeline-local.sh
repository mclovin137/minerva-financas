#!/usr/bin/env bash
# Gates reproduzíveis para executar antes de um push, sem disparar workflows remotos.
set -euo pipefail

mapfile -t workflows < <(find .github/workflows -maxdepth 1 -type f \( -name '*.yml' -o -name '*.yaml' \) -print | LC_ALL=C sort)
if [ "${#workflows[@]}" -eq 0 ]; then
  printf 'Nenhum workflow versionado foi encontrado em .github/workflows.\n' >&2
  exit 1
fi
printf 'Workflows detectados para validação local:\n'
printf '%s\n' "${workflows[@]}"

jq empty .claude/settings.json .codex/hooks.json
jq -e '.hooks.SessionStart == null and (.hooks.PreToolUse | type) == "array" and any(.hooks.PreToolUse[]; .matcher == "Write|Edit|NotebookEdit|Bash" and any(.hooks[]; .type == "command" and (.command | endswith("guarda-orquestrador.sh")))) and .hooks.PostToolUse == null' .claude/settings.json >/dev/null
jq -e '.hooks.SessionStart == null and .hooks.PreToolUse == null and .hooks.PostToolUse == null' .codex/hooks.json >/dev/null

while IFS= read -r -d '' arquivo; do
  bash -n "$arquivo"
done < <(find .claude .codex .github/scripts .githooks -type f \( -name '*.sh' -o -name '*.bash' -o -name 'pre-push' \) -print0)

bash .github/scripts/guard-lib-md.sh
git diff --check

if command -v actionlint >/dev/null 2>&1; then
  actionlint "${workflows[@]}"
elif command -v docker >/dev/null 2>&1; then
  docker run --rm -v "$PWD":/repo -w /repo rhysd/actionlint:1.7.7
else
  printf 'actionlint ou Docker é necessário para validar os workflows antes do push.\n' >&2
  exit 1
fi
