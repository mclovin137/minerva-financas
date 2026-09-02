#!/usr/bin/env bash
# Compara dependências diretas declaradas em manifestos com o inventário docs/lib.md.
set -euo pipefail
raiz="${1:-.}"
cd "$raiz"
if [ ! -f docs/lib.md ]; then
  printf 'Inventário ausente: docs/lib.md\n' >&2
  exit 1
fi
temporario="$(mktemp -d)"
trap 'rm -rf "$temporario"' EXIT
manifestos="$temporario/manifestos"
dependencias="$temporario/dependencias"
registradas="$temporario/registradas"
find . -type d \( -name .git -o -name node_modules -o -name worktrees \) -prune -o -type f \( \
  -name pom.xml -o -name build.gradle -o -name build.gradle.kts -o \
  -name requirements.txt -o -name pyproject.toml -o -name Cargo.toml -o \
  -name composer.json -o -name Gemfile -o -name go.mod -o -name package.json -o \
  -name '*.csproj' \
\) -print | LC_ALL=C sort > "$manifestos"
if [ ! -s "$manifestos" ]; then
  printf 'Nenhum manifesto de dependência encontrado; guard não aplicável.\n'
  exit 0
fi
: > "$dependencias"
falha_parser() {
  printf 'Manifesto detectado mas não pôde ser lido: %s (%s).\n' "$1" "$2" >&2
  exit 2
}
while IFS= read -r manifesto; do
  case "$manifesto" in
    */pom.xml|./pom.xml|*.csproj)
      if ! command -v python3 >/dev/null 2>&1; then
        printf 'Manifesto detectado mas não pôde ser lido: %s (python3 é necessário para XML).\n' "$manifesto" >&2
        exit 2
      fi
      tipo_xml=pom
      case "$manifesto" in *.csproj) tipo_xml=csproj;; esac
      if ! python3 - "$manifesto" "$tipo_xml" >> "$dependencias" <<'PY'
import sys
import xml.etree.ElementTree as ET

caminho, tipo = sys.argv[1:]
raiz = ET.parse(caminho).getroot()
nome_raiz = raiz.tag.rsplit("}", 1)[-1]
if tipo == "pom":
    if nome_raiz != "project":
        raise ValueError("raiz Maven incompatível; esperado project")
    filhos = {e.tag.rsplit("}", 1)[-1] for e in raiz}
    if "modelVersion" not in filhos:
        raise ValueError("estrutura Maven incompatível; modelVersion ausente")
    # ⚠️ DÍVIDA — dependências declaradas somente em <profiles> passam em silêncio;
    # foi aceito porque o template não declara dependências e o caso é latente.
    secao = next((e for e in raiz if e.tag.rsplit("}", 1)[-1] == "dependencies"), None)
    if secao is not None:
        for dependencia in secao:
            campos = {e.tag.rsplit("}", 1)[-1]: (e.text or "").strip() for e in dependencia}
            if dependencia.tag.rsplit("}", 1)[-1] != "dependency" or not campos.get("groupId") or not campos.get("artifactId"):
                raise ValueError("dependência Maven direta sem groupId/artifactId")
            print(f'{campos["groupId"]}:{campos["artifactId"]}')
else:
    if nome_raiz != "Project":
        raise ValueError("raiz MSBuild incompatível; esperado Project")
    elementos_validos = {"PropertyGroup", "ItemGroup", "Import", "ImportGroup", "Target", "Choose", "ProjectExtensions", "Sdk", "ItemDefinitionGroup"}
    if any(e.tag.rsplit("}", 1)[-1] not in elementos_validos for e in raiz):
        raise ValueError("estrutura MSBuild incompatível")
    for elemento in raiz.iter():
        if elemento.tag.rsplit("}", 1)[-1] == "PackageReference":
            nome = elemento.attrib.get("Include")
            if not nome:
                raise ValueError("PackageReference sem Include")
            print(nome)
PY
      then
        printf 'Manifesto detectado mas não pôde ser lido: %s (XML inválido ou estrutura não suportada).\n' "$manifesto" >&2
        exit 2
      fi
      ;;
    */composer.json|./composer.json)
      if ! command -v jq >/dev/null 2>&1 || ! jq -r '(.require // {} | keys[]) , (."require-dev" // {} | keys[])' "$manifesto" >> "$dependencias"; then
        printf 'Manifesto detectado mas não pôde ser lido: %s (jq ausente ou JSON inválido).\n' "$manifesto" >&2
        exit 2
      fi
      ;;
    */requirements.txt|./requirements.txt)
      python3 - "$manifesto" >> "$dependencias" <<'PY' || falha_parser "$manifesto" 'linha não suportada ou requirements inválido'
import re,sys
for n,line in enumerate(open(sys.argv[1],encoding="utf-8"),1):
    s=line.split("#",1)[0].strip()
    if not s: continue
    if s.startswith("-"): raise ValueError(f"linha {n}: -r/-e e diretivas não suportadas")
    m=re.match(r"^([A-Za-z0-9][A-Za-z0-9._-]*)",s)
    if not m or (m.end()<len(s) and s[m.end()] not in "<>=!~[ "): raise ValueError(f"linha {n}: sintaxe não suportada")
    print(m.group(1).lower().replace("_","-").replace(".","-"))
PY
      ;;
    */Gemfile|./Gemfile)
      python3 - "$manifesto" >> "$dependencias" <<'PY' || falha_parser "$manifesto" 'declaração Gemfile não suportada'
import re,sys
for n,line in enumerate(open(sys.argv[1],encoding="utf-8"),1):
    texto=line.split("#",1)[0].strip()
    if not texto: continue
    if re.search(r"\b(?:gemspec|eval_gemfile|instance_eval|load)\b",texto):
        raise ValueError(f"linha {n}: inclusão dinâmica pode esconder dependências")
    if re.match(r"^gem\b",texto):
        m=re.match(r"^gem\s*\(?\s*['\"]([^'\"]+)['\"]",texto)
        if not m: raise ValueError(f"linha {n}: gem precisa de nome literal")
        print(m.group(1))
PY
      ;;
    */build.gradle|./build.gradle|*/build.gradle.kts|./build.gradle.kts)
      python3 - "$manifesto" >> "$dependencias" <<'PY' || falha_parser "$manifesto" 'declaração Gradle não suportada'
import re,sys
cfg=r"(?:implementation|api|compileOnly|runtimeOnly|testImplementation|testRuntimeOnly|developmentOnly)"
comentario_bloco=False
def sem_comentario(linha):
    global comentario_bloco
    resultado=[]
    pos=0
    while pos < len(linha):
        if comentario_bloco:
            fim=linha.find("*/",pos)
            if fim < 0: return "".join(resultado)
            comentario_bloco=False; pos=fim+2; continue
        inicio=linha.find("/*",pos)
        linha_comentario=linha.find("//",pos)
        if linha_comentario >= 0 and (inicio < 0 or linha_comentario < inicio):
            resultado.append(linha[pos:linha_comentario]); break
        if inicio < 0:
            resultado.append(linha[pos:]); break
        resultado.append(linha[pos:inicio])
        fim=linha.find("*/",inicio+2)
        if fim < 0: comentario_bloco=True; break
        pos=fim+2
    return "".join(resultado)
for n,line in enumerate(open(sys.argv[1],encoding="utf-8"),1):
    texto=sem_comentario(line).strip()
    if not texto: continue
    if re.search(r"\bclasspath\b",texto):
        raise ValueError(f"linha {n}: classpath não suportado")
    ocorrencias=list(re.finditer(r"\b"+cfg+r"\b",texto))
    for ocorrencia in ocorrencias:
        resto=texto[ocorrencia.end():]
        m=re.match(r"\s*(?:\(\s*)?['\"]([^'\"]+)['\"]",resto)
        if not m: raise ValueError(f"linha {n}: coordenada precisa ser literal")
        p=m.group(1).split(":")
        if len(p)<2: raise ValueError(f"linha {n}: coordenada sem grupo/artefato")
        # ⚠️ DÍVIDA — Gradle emite só o artefato, enquanto Maven emite groupId:artifactId;
        # foi aceito porque o template não declara dependências e o falso-positivo é latente.
        print(p[1] if len(p)>=3 else p[0])
PY
      ;;
    */pyproject.toml|./pyproject.toml|*/Cargo.toml|./Cargo.toml)
      if ! command -v python3 >/dev/null 2>&1; then
        printf 'Manifesto detectado mas não pôde ser lido: %s (python3 é necessário para TOML).\n' "$manifesto" >&2
        exit 2
      fi
      tipo_toml=pyproject
      case "$manifesto" in */Cargo.toml|./Cargo.toml) tipo_toml=cargo;; esac
if ! python3 - "$manifesto" "$tipo_toml" >> "$dependencias" <<'PY'
import sys
try:
    import tomllib
except ImportError:
    raise SystemExit(2)

caminho, tipo = sys.argv[1:]
with open(caminho, "rb") as arquivo:
    dados = tomllib.load(arquivo)
if tipo == "cargo":
    for nome, tabela in dados.items():
        if nome == "target" and isinstance(tabela, dict):
            for alvo, configuracao in tabela.items():
                if isinstance(configuracao, dict) and any(chave in configuracao for chave in ("dependencies", "dev-dependencies", "build-dependencies")):
                    raise ValueError(f"tabela target.{alvo} contém dependências não suportadas")
    for chave in ("dependencies", "dev-dependencies", "build-dependencies"):
        print("\n".join(dados.get(chave, {}).keys()))
else:
    poesia=dados.get("tool", {}).get("poetry", {})
    for chave, valor in poesia.items():
        if chave == "group" or chave == "dev-dependencies":
            raise ValueError(f"tabela Poetry {chave} não suportada")
        if chave not in ("dependencies", "source", "repository", "packages", "scripts", "plugins", "build", "include", "exclude") and isinstance(valor, dict):
            raise ValueError(f"tabela Poetry {chave} não suportada")
    # ⚠️ DÍVIDA — [dependency-groups] (PEP 735) fica invisível e passa em silêncio;
    # foi aceito porque o template não declara dependências e o caso é latente.
    projeto = dados.get("project", {})
    def pep508(item):
        import re
        m = re.match(r"^\s*([A-Za-z0-9][A-Za-z0-9._-]*)", item)
        if not m: raise ValueError("dependência PEP 508 sem nome")
        return re.sub(r"[-_.]+", "-", m.group(1)).lower()
    for item in projeto.get("dependencies", []): print(pep508(item))
    for grupo in projeto.get("optional-dependencies", {}).values():
        for item in grupo: print(pep508(item))
    for nome in poesia.get("dependencies", {}):
        if nome.lower() != "python": print(pep508(nome))
PY
      then
        printf 'Manifesto detectado mas não pôde ser lido: %s (TOML inválido ou tomllib indisponível).\n' "$manifesto" >&2
        exit 2
      fi
      ;;
    */go.mod|./go.mod)
      awk '/^require[[:space:]]+\(/ { em_bloco = 1; next } em_bloco && /^\)/ { em_bloco = 0; next } /^require[[:space:]]+/ { print $2; next } em_bloco && /^[[:space:]]*[^[:space:]\/]/ { print $1 }' "$manifesto" >> "$dependencias"
      ;;
    */package.json|./package.json)
      if ! command -v jq >/dev/null 2>&1 || ! jq -r '(.dependencies // {} | keys[]) , (.devDependencies // {} | keys[]) , (.optionalDependencies // {} | keys[]) , (.peerDependencies // {} | keys[])' "$manifesto" >> "$dependencias"; then
        printf 'Manifesto detectado mas não pôde ser lido: %s (jq ausente ou JSON inválido).\n' "$manifesto" >&2
        exit 2
      fi
      ;;
  esac
done < "$manifestos"
LC_ALL=C sort -u "$dependencias" -o "$dependencias"
awk -F'|' '/^\|/ { nome = $2; gsub(/^[[:space:]]+|[[:space:]]+$/, "", nome); if (nome != "" && nome != "Nome" && nome !~ /^-+$/) print nome }' docs/lib.md | LC_ALL=C sort -u > "$registradas"
achados=0
while IFS= read -r dependencia; do
  if [ -n "$dependencia" ] && ! grep -Fqx -- "$dependencia" "$registradas"; then
    printf 'Dependência declarada sem registro em docs/lib.md: %s\n' "$dependencia" >&2
    achados=1
  fi
done < "$dependencias"
if [ "$achados" -ne 0 ]; then
  exit 1
fi
printf 'Manifestos e docs/lib.md estão coerentes.\n'
