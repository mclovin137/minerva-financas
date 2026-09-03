import { useEffect, useRef } from 'react'

/**
 * Confirmação destrutiva (§6 de docs/design/telas.md). É modal, e não drawer, porque a interrupção
 * é o ponto.
 *
 * O foco inicial vai para **Cancelar** de propósito: um `Enter` acidental não pode remover nada.
 */
export function ModalConfirmacao({
  titulo,
  consequencia,
  rotuloConfirmar,
  enviando,
  erro,
  aoConfirmar,
  aoCancelar,
}: {
  titulo: string
  consequencia: string
  rotuloConfirmar: string
  enviando: boolean
  erro?: string
  aoConfirmar: () => void
  aoCancelar: () => void
}) {
  const cancelar = useRef<HTMLButtonElement>(null)

  useEffect(() => {
    cancelar.current?.focus()
    function aoTeclar(evento: KeyboardEvent) {
      if (evento.key === 'Escape') aoCancelar()
    }
    document.addEventListener('keydown', aoTeclar)
    return () => document.removeEventListener('keydown', aoTeclar)
  }, [aoCancelar])

  return (
    <div className="modal-fundo">
      <div className="modal" role="alertdialog" aria-modal="true" aria-label={titulo}>
        <header>
          <h2>{titulo}</h2>
        </header>
        <div className="corpo">
          <p>{consequencia}</p>
          {erro && (
            <p className="mensagem-erro" role="alert">
              {erro}
            </p>
          )}
        </div>
        <footer>
          <button type="button" className="acao" onClick={aoCancelar} disabled={enviando} ref={cancelar}>
            Cancelar
          </button>
          <button type="button" className="acao destrutiva" onClick={aoConfirmar} disabled={enviando}>
            {enviando ? 'Removendo…' : rotuloConfirmar}
          </button>
        </footer>
      </div>
    </div>
  )
}
