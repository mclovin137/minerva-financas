import type { ReactNode } from 'react'

/**
 * Skeleton com o formato do conteúdo real, e não um spinner centralizado: manter a forma evita o
 * salto de layout quando o conteúdo chega (docs/design/telas.md, §4.1).
 */
export function Carregando({ linhas = 6, rotulo = 'Carregando' }: { linhas?: number; rotulo?: string }) {
  return (
    <div aria-busy="true" aria-live="polite" aria-label={rotulo}>
      {Array.from({ length: linhas }, (_, i) => (
        <div className="skeleton-linha" key={i} style={{ width: i === 0 ? '40%' : '100%' }} />
      ))}
    </div>
  )
}

export function Vazio({
  icone,
  titulo,
  explicacao,
  acao,
}: {
  icone: string
  titulo: string
  explicacao: string
  acao?: ReactNode
}) {
  return (
    <div className="estado-vazio">
      <div className="icone" aria-hidden="true">{icone}</div>
      <h2>{titulo}</h2>
      <p>{explicacao}</p>
      {acao}
    </div>
  )
}

/**
 * O ⚠ acompanha a cor de propósito: nenhum estado pode depender só de cor
 * (docs/design/acessibilidade.md).
 */
export function Erro({ mensagem, aoTentarDeNovo }: { mensagem: string; aoTentarDeNovo?: () => void }) {
  return (
    <div className="estado-erro" role="alert">
      <div className="titulo">
        <span aria-hidden="true">⚠ </span>
        Não foi possível carregar
      </div>
      <p>{mensagem}</p>
      {aoTentarDeNovo && (
        <button type="button" className="acao" onClick={aoTentarDeNovo}>
          Tentar de novo
        </button>
      )}
    </div>
  )
}
