import { useEffect, useRef, type ReactNode } from 'react'

/**
 * Drawer lateral (bottom sheet abaixo de 768px), conforme §5 de docs/design/telas.md.
 *
 * Três comportamentos de teclado são obrigatórios e ficam todos aqui, para que nenhum formulário
 * precise lembrar deles: o foco entra no primeiro campo, fica preso enquanto o drawer está aberto e
 * volta ao elemento que o abriu ao fechar. Sem isso o teclado se perde atrás da sobreposição.
 */
export function Drawer({
  titulo,
  aberto,
  aoFechar,
  children,
  rodape,
}: {
  titulo: string
  aberto: boolean
  aoFechar: () => void
  children: ReactNode
  rodape: ReactNode
}) {
  const painel = useRef<HTMLDivElement>(null)
  const focoAnterior = useRef<HTMLElement | null>(null)

  useEffect(() => {
    if (!aberto) return
    focoAnterior.current = document.activeElement as HTMLElement | null

    const focaveis = painel.current?.querySelectorAll<HTMLElement>(
      'input, select, textarea, button, [href], [tabindex]:not([tabindex="-1"])',
    )
    focaveis?.[0]?.focus()

    function aoTeclar(evento: KeyboardEvent) {
      if (evento.key === 'Escape') {
        aoFechar()
        return
      }
      if (evento.key !== 'Tab') return

      const lista = painel.current?.querySelectorAll<HTMLElement>(
        'input:not([disabled]), select:not([disabled]), textarea:not([disabled]), button:not([disabled]), [href]',
      )
      if (!lista || lista.length === 0) return
      const primeiro = lista[0]
      const ultimo = lista[lista.length - 1]

      if (evento.shiftKey && document.activeElement === primeiro) {
        evento.preventDefault()
        ultimo.focus()
      } else if (!evento.shiftKey && document.activeElement === ultimo) {
        evento.preventDefault()
        primeiro.focus()
      }
    }

    document.addEventListener('keydown', aoTeclar)
    return () => {
      document.removeEventListener('keydown', aoTeclar)
      focoAnterior.current?.focus()
    }
  }, [aberto, aoFechar])

  if (!aberto) return null

  return (
    <div
      className="sobreposicao"
      onMouseDown={(evento) => {
        if (evento.target === evento.currentTarget) aoFechar()
      }}
    >
      <div className="drawer" role="dialog" aria-modal="true" aria-label={titulo} ref={painel}>
        <header>
          <h2>{titulo}</h2>
          <button type="button" className="discreta" onClick={aoFechar} aria-label="Fechar">
            ✕
          </button>
        </header>
        <div className="campos">{children}</div>
        <footer>{rodape}</footer>
      </div>
    </div>
  )
}

export function Campo({
  rotulo,
  id,
  erro,
  children,
}: {
  rotulo: string
  id: string
  erro?: string
  children: ReactNode
}) {
  return (
    <div className="campo">
      <label htmlFor={id}>{rotulo}</label>
      {children}
      {erro && (
        <span className="mensagem-erro" id={`${id}-erro`} role="alert">
          {erro}
        </span>
      )}
    </div>
  )
}
