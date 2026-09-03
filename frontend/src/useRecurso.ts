import { useCallback, useEffect, useRef, useState } from 'react'
import { ErroApi, ErroDeRede } from './api/cliente'

export interface Recurso<T> {
  dados: T | null
  carregando: boolean
  erro: string | null
  recarregar: () => Promise<void>
}

/**
 * Carrega um recurso da API e expõe os três estados que toda tela precisa ter (§4 de
 * docs/design/telas.md). Concentrar isso aqui é o que impede uma tela de "esquecer" o estado de erro
 * e ficar carregando para sempre.
 */
export function useRecurso<T>(carregar: () => Promise<T>, dependencias: unknown[]): Recurso<T> {
  const [dados, definirDados] = useState<T | null>(null)
    const [carregando, definirCarregando] = useState(true)
  const [erro, definirErro] = useState<string | null>(null)
  const [gatilho, definirGatilho] = useState(0)
  const recarregamentosPendentes = useRef<Array<() => void>>([])

  function concluirRecarregamentos() {
    recarregamentosPendentes.current.splice(0).forEach((resolver) => resolver())
  }

  useEffect(() => concluirRecarregamentos, [])

  // eslint-disable-next-line react-hooks/exhaustive-deps
  const executar = useCallback(carregar, dependencias)

  useEffect(() => {
    let cancelado = false
    definirCarregando(true)
    definirErro(null)

    Promise.resolve()
      .then(() => executar())
      .then((resultado) => {
        if (!cancelado) definirDados(resultado)
      })
      .catch((falha: unknown) => {
        if (cancelado) return
        // O 401 já derrubou a sessão no cliente; mostrar erro aqui piscaria uma mensagem inútil
        // por cima da tela de entrada.
        if (falha instanceof ErroApi && falha.status === 401) return
        definirErro(
          falha instanceof ErroApi || falha instanceof ErroDeRede
            ? falha.message
            : 'Ocorreu um erro inesperado.',
        )
      })
      .finally(() => {
        if (!cancelado) {
          definirCarregando(false)
          // A Promise de recarregar só pode concluir depois que esta execução terminou. Resolver
          // no rerender que dispara a execução libera a tela antes da resposta e deixa os
          // consumidores lendo o valor anterior.
          concluirRecarregamentos()
        }
      })

    return () => {
      cancelado = true
    }
  }, [executar, gatilho])

  const recarregar = useCallback(() => {
    const concluido = new Promise<void>((resolver) => {
      recarregamentosPendentes.current.push(resolver)
    })
    definirGatilho((n) => n + 1)
    return concluido
  }, [])

  return { dados, carregando, erro, recarregar }
}
