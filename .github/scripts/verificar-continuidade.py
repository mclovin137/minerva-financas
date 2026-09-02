#!/usr/bin/env python3
"""Verifica a forma de docs/continuidade.md: cardinalidade das seções e teto de tamanho.

Existe porque a regra sozinha não segurou. Sem gate, o arquivo acumulou sete blocos de
"Resumo decisório mínimo", três de "Decisões vigentes" e sete de "Pendências Obsidian",
crescendo de 2.440 B para 88.950 B em nove dias.

O gate verifica FORMA, não julgamento: ele recusa seção repetida, seção fora do conjunto
declarado e arquivo acima do teto. Ele não sabe dizer se um parágrafo é estado ou registro —
isso continua sendo trabalho do agente. Contrato: docs/rules.md, seção "Estado e continuidade".
"""
import sys
from pathlib import Path

TETO_BYTES = 8192

SECOES_OBRIGATORIAS = [
    "Task ativa",
    "Estado atual",
    "Decisões vigentes",
    "Riscos e lacunas",
    "Próximo passo",
    "Região gerada",
]


def verificar(caminho: Path) -> list[str]:
    falhas: list[str] = []
    bruto = caminho.read_bytes()
    texto = bruto.decode("utf-8")

    if len(bruto) > TETO_BYTES:
        falhas.append(
            f"{caminho}: {len(bruto)} B excede o teto de {TETO_BYTES} B. "
            "Rotacione o registro encerrado para docs/historico/AAAA-MM.md."
        )

    titulos = [
        linha[3:].strip()
        for linha in texto.splitlines()
        if linha.startswith("## ")
    ]

    vistos: dict[str, int] = {}
    for titulo in titulos:
        vistos[titulo] = vistos.get(titulo, 0) + 1

    for titulo, quantidade in vistos.items():
        if quantidade > 1:
            falhas.append(
                f"{caminho}: a seção '## {titulo}' aparece {quantidade} vezes; "
                "estado tem cardinalidade 1 e é sobrescrito, não acrescentado."
            )

    for obrigatoria in SECOES_OBRIGATORIAS:
        if obrigatoria not in vistos:
            falhas.append(f"{caminho}: falta a seção obrigatória '## {obrigatoria}'.")

    for titulo in vistos:
        if titulo not in SECOES_OBRIGATORIAS:
            falhas.append(
                f"{caminho}: seção '## {titulo}' não pertence ao conjunto declarado. "
                "Registro encerrado vai para docs/historico/; obrigação aberta vai para "
                "docs/pendencias-obsidian.md."
            )

    return falhas


def main() -> int:
    caminho = Path(sys.argv[1] if len(sys.argv) > 1 else "docs/continuidade.md")
    if not caminho.is_file():
        print(f"Arquivo inexistente: {caminho}", file=sys.stderr)
        return 2

    falhas = verificar(caminho)
    if falhas:
        for falha in falhas:
            print(f"REPROVADO — {falha}", file=sys.stderr)
        return 1

    print(f"APROVADO — {caminho}: {caminho.stat().st_size} B, seções únicas e declaradas.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
