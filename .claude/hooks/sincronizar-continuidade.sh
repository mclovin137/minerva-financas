#!/usr/bin/env bash
# Mantém exclusivamente a região gerada de docs/continuidade.md. Não interpreta
# payload, não sincroniza Obsidian e não altera significado, checklist ou plano.
set -euo pipefail

modo="${1:-sync}"
case "$modo" in init|sync) ;; *) printf 'Modo inválido: %s\n' "$modo" >&2; exit 2 ;; esac
if [ ! -t 0 ]; then cat >/dev/null || true; fi

if [ -n "${MINERVA_CONTINUITY_ROOT:-}" ]; then
  raiz="$(cd "$MINERVA_CONTINUITY_ROOT" && pwd -P)"
elif [ -n "${CLAUDE_PROJECT_DIR:-}" ]; then
  raiz="$(cd "$CLAUDE_PROJECT_DIR" && pwd -P)"
else
  raiz="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd -P)"
fi

continuidade="$raiz/docs/continuidade.md"
[ -f "$continuidade" ] || exit 0
id_raiz="$(printf '%s' "$raiz" | sha256sum | cut -c1-16)"
runtime="${MINERVA_CONTINUITY_RUNTIME_DIR:-${TMPDIR:-/tmp}/minerva-continuity-$(id -u)-$id_raiz}"
mkdir -p "$runtime"
chmod 700 "$runtime"
exec 9>"$runtime/lock"
flock -w 5 9 || { printf 'Não foi possível adquirir o lock de continuidade.\n' >&2; exit 1; }

snapshot="$runtime/snapshot.tsv"
novo="$(mktemp "$runtime/snapshot-novo.XXXXXX")"
trap 'rm -f "$novo" "${corpo:-}" "${temporario:-}"' EXIT

gerar_snapshot() {
  local destino="$1" inventario relativo base
  inventario="$(mktemp "$runtime/inventario.XXXXXX")"
  while IFS= read -r -d '' arquivo; do
    relativo="${arquivo#"$raiz"/}"; base="${relativo##*/}"
    case "$relativo" in
      docs/continuidade.md|docs/.continuidade.md.tmp.*|.git/*|.idea/*|vendor/*|node_modules/*|build/*|dist/*|coverage/*|storage/framework/*|bootstrap/cache/*|.claude/.continuity/*) continue ;;
    esac
    case "$relativo" in *$'\n'*|*$'\t'*) continue ;; esac
    case "$base" in
      Dockerfile|Containerfile|Makefile|composer.lock|package-lock.json|pnpm-lock.yaml|yarn.lock|*.md|*.txt|*.php|*.json|*.jsonc|*.yaml|*.yml|*.xml|*.toml|*.ini|*.conf|*.env.example|*.example|*.sh|*.bash|*.sql|*.js|*.jsx|*.ts|*.tsx|*.css|*.scss|*.html|*.blade.php|*.graphql|*.gql) ;;
      *) continue ;;
    esac
    printf '%s\t%s\n' "$relativo" "$(sha256sum "$arquivo" | cut -d' ' -f1)" >>"$inventario"
  done < <(find "$raiz" -type f -print0)
  LC_ALL=C sort -t $'\t' -k1,1 "$inventario" >"$destino"
  rm -f "$inventario"
}

gerar_snapshot "$novo"
if [ "$modo" = init ] || [ ! -f "$snapshot" ]; then
  mv -f "$novo" "$snapshot"
  trap - EXIT
  exit 0
fi

declare -A antes depois
while IFS=$'\t' read -r caminho hash; do [ -n "$caminho" ] && antes["$caminho"]="$hash"; done <"$snapshot"
while IFS=$'\t' read -r caminho hash; do [ -n "$caminho" ] && depois["$caminho"]="$hash"; done <"$novo"
criados=() alterados=() removidos=()
for caminho in "${!depois[@]}"; do
  if [ -z "${antes[$caminho]+presente}" ]; then criados+=("$caminho")
  elif [ "${antes[$caminho]}" != "${depois[$caminho]}" ]; then alterados+=("$caminho"); fi
done
for caminho in "${!antes[@]}"; do [ -z "${depois[$caminho]+presente}" ] && removidos+=("$caminho"); done
if [ "${#criados[@]}" -eq 0 ] && [ "${#alterados[@]}" -eq 0 ] && [ "${#removidos[@]}" -eq 0 ]; then exit 0; fi

ordenar() { [ "$#" -eq 0 ] || printf '%s\n' "$@" | LC_ALL=C sort; }
corpo="$(mktemp "$runtime/corpo.XXXXXX")"
{
  printf 'Última sincronização: `%s`\n\n' "$(date -u '+%Y-%m-%dT%H:%M:%SZ')"
  for rotulo in Criados Alterados Removidos; do
    printf '### %s\n' "$rotulo"
    case "$rotulo" in Criados) itens=("${criados[@]}");; Alterados) itens=("${alterados[@]}");; *) itens=("${removidos[@]}");; esac
    if [ "${#itens[@]}" -eq 0 ]; then printf -- '- Nenhum.\n\n'; else ordenar "${itens[@]}" | while IFS= read -r item; do printf -- '- `%s`\n' "$item"; done; printf '\n'; fi
  done
  printf 'Este registro não altera nem conclui a checklist da task.\n'
} >"$corpo"

temporario="$(mktemp "$(dirname "$continuidade")/.continuidade.md.tmp.XXXXXX")"
awk -v inicio='<!-- minerva-continuity:generated:start -->' -v fim='<!-- minerva-continuity:generated:end -->' -v corpo="$corpo" '
  $0 == inicio { print; while ((getline linha < corpo) > 0) print linha; close(corpo); ignorar=1; next }
  $0 == fim { ignorar=0; print; next }
  !ignorar { print }
' "$continuidade" >"$temporario"
grep -Fqx '<!-- minerva-continuity:generated:start -->' "$continuidade" && grep -Fqx '<!-- minerva-continuity:generated:end -->' "$continuidade" || { printf 'Região gerada ausente.\n' >&2; exit 1; }
chmod --reference="$continuidade" "$temporario"
mv -f "$temporario" "$continuidade"
mv -f "$novo" "$snapshot"
