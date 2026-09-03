import { useEffect, useRef, useState } from 'react'
import { api, consultarExecucao, ErroApi } from '../api/cliente'
import type { Ativo, Posicao } from '../api/tipos'
import { Campo, Drawer } from '../componentes/Drawer'
import { Carregando, Erro, Vazio } from '../componentes/Estados'
import {
  AUSENTE,
  casasDecimais,
  data as formatarData,
  horaAtual,
  moeda,
  paraNumero,
  preco,
  quantidade,
  rendimento,
} from '../formato'
import { useRecurso } from '../useRecurso'

type Operacao = 'compra' | 'venda'

export function Carteira({ cursor, avisar }: { cursor: string; avisar: (texto: string) => void }) {
  const [drawer, definirDrawer] = useState<Operacao | null>(null)
  const [assincrona, definirAssincrona] = useState<Posicao[] | null>(null)
  const [atualizadaEm, definirAtualizadaEm] = useState<string | null>(null)

  const posicao = useRecurso(() => api.posicao(cursor), [cursor])
  const ativos = useRecurso(() => api.ativos(), [])
  const segundoPlano = usePosicaoAssincrona(cursor, (resultado) => {
    definirAssincrona(resultado)
    definirAtualizadaEm(horaAtual())
  })

  // Uma nova data invalida o resultado calculado em segundo plano para a data anterior.
  useEffect(() => {
    definirAssincrona(null)
    definirAtualizadaEm(null)
  }, [cursor])

  const linhas = assincrona ?? posicao.dados
  const patrimonio = (linhas ?? []).reduce((total, p) => total + (p.valorMercadoTotal ?? 0), 0)
  const lucro = (linhas ?? []).reduce((total, p) => total + p.lucro, 0)

  function concluir(texto: string) {
    definirDrawer(null)
    avisar(texto)
    definirAssincrona(null)
    posicao.recarregar()
  }

  return (
    <>
      <header>
        <h1>Carteira</h1>
        <div className="espacador" />
        <button
          type="button"
          className="acao"
          onClick={segundoPlano.iniciar}
          disabled={segundoPlano.processando}
        >
          {segundoPlano.processando ? 'Calculando…' : 'Recalcular em segundo plano'}
        </button>
        <button type="button" className="acao" onClick={() => definirDrawer('venda')}>
          Nova venda
        </button>
        <button type="button" className="acao primaria" onClick={() => definirDrawer('compra')}>
          Nova compra
        </button>
      </header>

      <div className="indicadores">
        <Indicador rotulo={`Patrimônio em ${formatarData(cursor)}`} valor={moeda(patrimonio)} destaque />
        <Indicador rotulo="Lucro acumulado" valor={moeda(lucro)} />
        <Indicador rotulo="Ativos na carteira" valor={String(linhas?.length ?? 0)} />
      </div>

      {segundoPlano.processando && (
        <div style={{ marginBottom: 'var(--space-3)' }}>
          {/* HTTP 425 é progresso, nunca erro: sem ⚠, sem cor de erro, sem contar como falha. */}
          <p style={{ color: 'var(--color-text-secondary)', margin: '0 0 var(--space-2)' }}>
            Calculando posição…{' '}
            {segundoPlano.demorando && (
              <>
                <button type="button" className="discreta" onClick={segundoPlano.continuar}>
                  Continuar aguardando
                </button>
                <button type="button" className="discreta" onClick={segundoPlano.cancelar}>
                  Cancelar
                </button>
              </>
            )}
          </p>
          <Carregando linhas={4} rotulo="Calculando posição" />
        </div>
      )}

      {segundoPlano.erro && <Erro mensagem={segundoPlano.erro} aoTentarDeNovo={segundoPlano.iniciar} />}

      {atualizadaEm && !segundoPlano.processando && (
        <p style={{ color: 'var(--color-text-secondary)', fontSize: 13, marginTop: 0 }}>
          Atualizada às {atualizadaEm}
        </p>
      )}

      {posicao.carregando && !assincrona && <Carregando rotulo="Carregando posição" />}
      {posicao.erro && !assincrona && <Erro mensagem={posicao.erro} aoTentarDeNovo={posicao.recarregar} />}

      {!posicao.carregando && !posicao.erro && (linhas?.length ?? 0) === 0 && !segundoPlano.processando && (
        <Vazio
          icone="◈"
          titulo={`Nenhuma posição em ${formatarData(cursor)}`}
          explicacao="Compre um ativo para começar a acompanhar seu patrimônio."
          acao={
            <button type="button" className="acao primaria" onClick={() => definirDrawer('compra')}>
              Nova compra
            </button>
          }
        />
      )}

      {(linhas?.length ?? 0) > 0 && <TabelaPosicao posicoes={linhas!} />}

      {drawer && (
        <FormularioMovimento
          operacao={drawer}
          cursor={cursor}
          ativos={ativos.dados ?? []}
          aoFechar={() => definirDrawer(null)}
          aoConcluir={concluir}
        />
      )}
    </>
  )
}

function Indicador({ rotulo, valor, destaque }: { rotulo: string; valor: string; destaque?: boolean }) {
  return (
    <div className={`indicador${destaque ? ' destaque' : ''}`}>
      <div className="rotulo">{rotulo}</div>
      <div className="valor numero">{valor}</div>
    </div>
  )
}

function TabelaPosicao({ posicoes }: { posicoes: Posicao[] }) {
  return (
    <div className="tabela-rolagem">
      <table>
        <thead>
          <tr>
            <th scope="col">Ativo</th>
            <th scope="col">Tipo</th>
            <th scope="col" className="direita">Quantidade</th>
            <th scope="col" className="direita">Preço de mercado</th>
            <th scope="col" className="direita">Valor de mercado</th>
            <th scope="col" className="direita">Preço médio</th>
            <th scope="col" className="direita">Rendimento</th>
            <th scope="col" className="direita">Lucro</th>
          </tr>
        </thead>
        <tbody>
          {posicoes.map((p) => (
            <tr key={p.ativo}>
              <td>
                <strong className="numero">{p.ativo}</strong>
                <div style={{ fontSize: 13, color: 'var(--color-text-secondary)' }}>{p.nome}</div>
              </td>
              <td>{p.tipo}</td>
              <td className="direita numero">{quantidade(p.quantidade)}</td>
              {/* Sem preço elegível na data, o campo é — com explicação, nunca zero. */}
              <td className="direita numero" title={p.precoMercado === null ? SEM_PRECO : undefined}>
                {p.precoMercado === null ? AUSENTE : preco(p.precoMercado)}
              </td>
              <td className="direita numero" title={p.valorMercadoTotal === null ? SEM_PRECO : undefined}>
                {moeda(p.valorMercadoTotal)}
              </td>
              <td className="direita numero">{preco(p.precoMedio)}</td>
              <td className="direita numero" title={p.rendimento === null ? SEM_PRECO : undefined}>
                {rendimento(p.rendimento)}
              </td>
              <td className={`direita numero ${p.lucro >= 0 ? 'entrada' : 'saida'}`}>
                <span aria-hidden="true">{p.lucro >= 0 ? '↑ ' : '↓ '}</span>
                {moeda(p.lucro)}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

const SEM_PRECO = 'Sem preço de mercado cadastrado até esta data.'

/**
 * Ciclo da opção B (§8 de docs/design/telas.md): 202 com o id, polling a cada 600 ms tratando 425
 * como progresso, e uma escolha explícita para o usuário após 60 segundos — em vez de uma espera
 * infinita e silenciosa.
 */
function usePosicaoAssincrona(cursor: string, aoConcluir: (posicoes: Posicao[]) => void) {
  const [processando, definirProcessando] = useState(false)
  const [demorando, definirDemorando] = useState(false)
  const [erro, definirErro] = useState<string | null>(null)
  const cancelado = useRef(false)

  async function iniciar() {
    definirErro(null)
    definirDemorando(false)
    definirProcessando(true)
    cancelado.current = false

    let id: number
    try {
      id = (await api.solicitarPosicao(cursor)).id
    } catch (falha) {
      definirProcessando(false)
      definirErro(falha instanceof ErroApi ? falha.message : 'Sem conexão com o servidor.')
      return
    }

    const inicio = Date.now()
    while (!cancelado.current) {
      await new Promise((resolver) => setTimeout(resolver, 600))
      if (cancelado.current) return

      try {
        const { status, corpo } = await consultarExecucao<Posicao[]>(id)
        if (status === 200 && corpo) {
          definirProcessando(false)
          aoConcluir(corpo)
          return
        }
        if (status === 404) {
          definirProcessando(false)
          definirErro('O resultado expirou. Recalcule para obter a posição atualizada.')
          return
        }
        if (status !== 425) {
          definirProcessando(false)
          definirErro('O servidor não conseguiu concluir o cálculo.')
          return
        }
      } catch (falha) {
        definirProcessando(false)
        definirErro(falha instanceof ErroApi ? falha.message : 'Sem conexão com o servidor.')
        return
      }

      if (Date.now() - inicio > 60_000) definirDemorando(true)
    }
  }

  return {
    processando,
    demorando,
    erro,
    iniciar,
    continuar: () => definirDemorando(false),
    cancelar: () => {
      cancelado.current = true
      definirProcessando(false)
      definirDemorando(false)
    },
  }
}

function FormularioMovimento({
  operacao,
  cursor,
  ativos,
  aoFechar,
  aoConcluir,
}: {
  operacao: Operacao
  cursor: string
  ativos: Ativo[]
  aoFechar: () => void
  aoConcluir: (texto: string) => void
}) {
  const [ativo, definirAtivo] = useState('')
  const [qtd, definirQtd] = useState('')
  const [valor, definirValor] = useState('')
  const [data, definirData] = useState(cursor)
  const [erroCampo, definirErroCampo] = useState<Record<string, string>>({})
  const [erroDominio, definirErroDominio] = useState<string | null>(null)
  const [enviando, definirEnviando] = useState(false)

  const compra = operacao === 'compra'
  const alterado = ativo !== '' || qtd !== '' || valor !== '' || data !== cursor

  function fechar() {
    if (alterado && !window.confirm('Descartar as alterações?')) return
    aoFechar()
  }

  function validar(): boolean {
    const erros: Record<string, string> = {}
    if (!ativo) erros.ativo = 'Escolha um ativo.'
    const q = paraNumero(qtd)
    if (q === null || q <= 0) erros.qtd = 'Informe uma quantidade positiva.'
    else if (casasDecimais(qtd) > 2) erros.qtd = 'A quantidade admite no máximo duas casas decimais.'
    const v = paraNumero(valor)
    if (v === null || v <= 0) erros.valor = 'Informe um valor positivo.'
    else if (casasDecimais(valor) > 2) erros.valor = 'O valor admite no máximo duas casas decimais.'
    definirErroCampo(erros)
    return Object.keys(erros).length === 0
  }

  async function submeter() {
    if (!validar()) return
    definirEnviando(true)
    definirErroDominio(null)
    try {
      const q = paraNumero(qtd)!
      const v = paraNumero(valor)!
      if (compra) await api.comprar(ativo, data, q, v)
      else await api.vender(ativo, data, q, v)
      aoConcluir(compra ? 'Compra registrada.' : 'Venda registrada.')
    } catch (falha) {
      definirErroDominio(falha instanceof ErroApi ? falha.message : 'Sem conexão com o servidor.')
    } finally {
      definirEnviando(false)
    }
  }

  return (
    <Drawer
      aberto
      titulo={compra ? 'Nova compra' : 'Nova venda'}
      aoFechar={fechar}
      rodape={
        <>
          {erroDominio && (
            <span className="erro-dominio" role="alert">
              {erroDominio}
            </span>
          )}
          <button type="button" className="acao" onClick={fechar} disabled={enviando}>
            Cancelar
          </button>
          <button type="button" className="acao primaria" onClick={submeter} disabled={enviando}>
            {enviando ? 'Salvando…' : compra ? 'Comprar' : 'Vender'}
          </button>
        </>
      }
    >
      <Campo rotulo="Ativo" id="ativo" erro={erroCampo.ativo}>
        <select
          id="ativo"
          value={ativo}
          disabled={enviando}
          aria-invalid={Boolean(erroCampo.ativo)}
          onChange={(e) => definirAtivo(e.target.value)}
        >
          <option value="">Selecione…</option>
          {ativos.map((a) => (
            <option key={a.ativo} value={a.ativo}>
              {a.ativo} — {a.nome}
            </option>
          ))}
        </select>
      </Campo>

      <Campo rotulo="Quantidade" id="qtd" erro={erroCampo.qtd}>
        <input
          id="qtd"
          inputMode="decimal"
          value={qtd}
          readOnly={enviando}
          aria-invalid={Boolean(erroCampo.qtd)}
          onChange={(e) => definirQtd(e.target.value)}
        />
      </Campo>

      <Campo rotulo="Valor da movimentação (R$)" id="valor-mov" erro={erroCampo.valor}>
        <input
          id="valor-mov"
          inputMode="decimal"
          value={valor}
          readOnly={enviando}
          aria-invalid={Boolean(erroCampo.valor)}
          onChange={(e) => definirValor(e.target.value)}
        />
      </Campo>

      <Campo rotulo="Data do movimento" id="data-mov">
        <input
          id="data-mov"
          type="date"
          value={data}
          readOnly={enviando}
          onChange={(e) => definirData(e.target.value)}
        />
      </Campo>
    </Drawer>
  )
}
