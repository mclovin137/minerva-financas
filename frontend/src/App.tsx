import { useCallback, useEffect, useState } from 'react'
import { aoExpirarSessao, limparCredencial } from './api/cliente'
import { data as formatarData, hoje } from './formato'
import { Ativos } from './telas/Ativos'
import { Carteira } from './telas/Carteira'
import { Conta } from './telas/Conta'
import { Entrada } from './telas/Entrada'
import { Mercado } from './telas/Mercado'

type Destino = 'conta' | 'carteira' | 'ativos' | 'mercado'

const DESTINOS: { id: Destino; icone: string; rotulo: string }[] = [
  { id: 'conta', icone: '⇅', rotulo: 'Conta corrente' },
  { id: 'carteira', icone: '◈', rotulo: 'Carteira' },
  { id: 'ativos', icone: '▤', rotulo: 'Ativos' },
  { id: 'mercado', icone: '⌇', rotulo: 'Mercado histórico' },
]

/** O administrador gerencia o acervo e não transaciona; o comum transaciona e não administra. */
const PROIBIDOS_AO_ADMINISTRADOR: Destino[] = ['conta', 'carteira']

function lerUrl(): { destino: Destino; data: string } {
  const parametros = new URLSearchParams(window.location.search)
  const destino = parametros.get('destino') as Destino | null
  const data = parametros.get('data')
  return {
    destino: DESTINOS.some((d) => d.id === destino) ? destino! : 'conta',
    data: data && /^\d{4}-\d{2}-\d{2}$/.test(data) ? data : hoje(),
  }
}

export function App() {
  const [login, definirLogin] = useState<string | null>(null)
  const [inicial] = useState(lerUrl)
  const [destino, definirDestino] = useState<Destino>(inicial.destino)
  const [cursor, definirCursor] = useState(inicial.data)
  const [aviso, definirAviso] = useState<string | null>(null)
  const [offline, definirOffline] = useState(!navigator.onLine)
  const [sessaoExpirada, definirSessaoExpirada] = useState(false)

  const administrador = login === 'root'

  // O cursor de data vive na URL: sem isso, um link compartilhado e o botão voltar do navegador
  // mostrariam outra data — o risco registrado na ADR-002.
  useEffect(() => {
    if (!login) return
    const parametros = new URLSearchParams({ destino, data: cursor })
    window.history.replaceState(null, '', `?${parametros}`)
  }, [destino, cursor, login])

  useEffect(() => {
    aoExpirarSessao(() => {
      definirLogin(null)
      definirSessaoExpirada(true)
    })
  }, [])

  useEffect(() => {
    const online = () => definirOffline(false)
    const foraDoAr = () => definirOffline(true)
    window.addEventListener('online', online)
    window.addEventListener('offline', foraDoAr)
    return () => {
      window.removeEventListener('online', online)
      window.removeEventListener('offline', foraDoAr)
    }
  }, [])

  const avisar = useCallback((texto: string) => {
    definirAviso(texto)
    window.setTimeout(() => definirAviso(null), 4000)
  }, [])

  // O administrador não pode ver conta nem carteira; ao entrar como root, o destino padrão muda.
  useEffect(() => {
    if (administrador && PROIBIDOS_AO_ADMINISTRADOR.includes(destino)) definirDestino('ativos')
  }, [administrador, destino])

  if (!login) {
    return (
      <>
        {sessaoExpirada && (
          <div className="faixa-offline" role="alert">
            Sua sessão expirou. Entre novamente.
          </div>
        )}
        <Entrada
          aoEntrar={(usuario) => {
            definirSessaoExpirada(false)
            definirLogin(usuario)
          }}
        />
      </>
    )
  }

  function deslocarDia(dias: number) {
    const [ano, mes, dia] = cursor.split('-').map(Number)
    const referencia = new Date(Date.UTC(ano, mes - 1, dia + dias))
    definirCursor(referencia.toISOString().slice(0, 10))
  }

  return (
    <div className="aplicacao">
      <a className="pular-para-conteudo" href="#conteudo">
        Pular para o conteúdo
      </a>

      {offline && (
        <div className="faixa-offline" role="alert">
          Sem conexão. As ações estão indisponíveis.
        </div>
      )}

      <header className="cabecalho">
        <span className="marca">Minerva Finanças</span>

        <div className="cursor-data">
          <button type="button" className="discreta" aria-label="Dia anterior" onClick={() => deslocarDia(-1)}>
            ◀
          </button>
          <input
            type="date"
            aria-label="Data de referência"
            value={cursor}
            onChange={(e) => e.target.value && definirCursor(e.target.value)}
          />
          <button type="button" className="discreta" aria-label="Próximo dia" onClick={() => deslocarDia(1)}>
            ▶
          </button>
          <button type="button" className="discreta" onClick={() => definirCursor(hoje())}>
            Hoje
          </button>
        </div>

        <div className="espacador" />

        <div className="usuario">
          <strong>{login}</strong>
          {administrador ? 'administrador' : 'usuário'}
        </div>
        <button
          type="button"
          className="acao"
          onClick={() => {
            limparCredencial()
            definirLogin(null)
          }}
        >
          Sair
        </button>
      </header>

      <div className="corpo">
        <nav className="trilho" aria-label="Destinos">
          {DESTINOS.map((d) => {
            const indisponivel = administrador && PROIBIDOS_AO_ADMINISTRADOR.includes(d.id)
            return (
              <button
                key={d.id}
                type="button"
                title={
                  indisponivel
                    ? `${d.rotulo} — indisponível para o perfil administrativo`
                    : d.rotulo
                }
                aria-label={d.rotulo}
                aria-current={destino === d.id ? 'page' : undefined}
                aria-disabled={indisponivel}
                disabled={indisponivel}
                onClick={() => definirDestino(d.id)}
              >
                <span aria-hidden="true">{d.icone}</span>
              </button>
            )
          })}
        </nav>

        <main className="conteudo" id="conteudo">
          {destino === 'conta' && <Conta cursor={cursor} avisar={avisar} />}
          {destino === 'carteira' && <Carteira cursor={cursor} avisar={avisar} />}
          {destino === 'ativos' && <Ativos administrador={administrador} avisar={avisar} />}
          {destino === 'mercado' && (
            <Mercado cursor={cursor} administrador={administrador} avisar={avisar} />
          )}
        </main>
      </div>

      {aviso && (
        <div className="aviso" role="status">
          {aviso}
        </div>
      )}

      <span hidden>{formatarData(cursor)}</span>
    </div>
  )
}
