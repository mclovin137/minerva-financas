import { useEffect, useState } from 'react'
import { api, ErroApi } from '../api/cliente'
import type { Lancamento } from '../api/tipos'
import { Campo, Drawer } from '../componentes/Drawer'
import { Carregando, Erro, Vazio } from '../componentes/Estados'
import { casasDecimais, data as formatarData, inicioDoMes, moeda, paraNumero } from '../formato'
import { useRecurso } from '../useRecurso'

type Operacao = 'credito' | 'debito'

export function Conta({ cursor, avisar }: { cursor: string; avisar: (texto: string) => void }) {
  const [inicio, definirInicio] = useState(() => inicioDoMes(cursor))
  const [drawer, definirDrawer] = useState<Operacao | null>(null)

  useEffect(() => {
    definirInicio(inicioDoMes(cursor))
  }, [cursor])

  const saldo = useRecurso(() => api.saldo(cursor), [cursor])
  const lancamentos = useRecurso(() => api.lancamentos(inicio, cursor), [inicio, cursor])

  async function concluir(texto: string) {
    await Promise.all([saldo.recarregar(), lancamentos.recarregar()])
    definirDrawer(null)
    avisar(texto)
  }

  return (
    <>
      <header>
        <h1>Conta corrente</h1>
        <div className="espacador" />
        <button type="button" className="acao" onClick={() => definirDrawer('debito')}>
          Novo débito
        </button>
        <button type="button" className="acao primaria" onClick={() => definirDrawer('credito')}>
          Novo crédito
        </button>
      </header>

      <div className="indicadores">
        <div className="indicador destaque">
          <div className="rotulo">Saldo em {formatarData(cursor)}</div>
          <div className="valor numero">
            {saldo.carregando ? '…' : saldo.erro ? '—' : moeda(saldo.dados?.saldo)}
          </div>
        </div>
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-2)', marginBottom: 'var(--space-3)' }}>
        <label htmlFor="periodo-inicio" style={{ fontSize: 13, color: 'var(--color-text-secondary)' }}>
          Período de
        </label>
        <input
          id="periodo-inicio"
          type="date"
          value={inicio}
          max={cursor}
          onChange={(e) => definirInicio(e.target.value)}
        />
        <span style={{ fontSize: 13, color: 'var(--color-text-secondary)' }}>
          até {formatarData(cursor)}
        </span>
      </div>

      {lancamentos.carregando && <Carregando rotulo="Carregando lançamentos" />}
      {lancamentos.erro && <Erro mensagem={lancamentos.erro} aoTentarDeNovo={lancamentos.recarregar} />}
      {!lancamentos.carregando && !lancamentos.erro && lancamentos.dados?.length === 0 && (
        <Vazio
          icone="⇅"
          titulo={`Nenhum lançamento até ${formatarData(cursor)}`}
          explicacao="Lance um crédito para começar a acompanhar o saldo."
          acao={
            <button type="button" className="acao primaria" onClick={() => definirDrawer('credito')}>
              Novo crédito
            </button>
          }
        />
      )}
      {!lancamentos.carregando && !lancamentos.erro && (lancamentos.dados?.length ?? 0) > 0 && (
        <TabelaLancamentos lancamentos={lancamentos.dados!} />
      )}

      {drawer && (
        <FormularioLancamento
          operacao={drawer}
          cursor={cursor}
          aoFechar={() => definirDrawer(null)}
          aoConcluir={concluir}
        />
      )}
    </>
  )
}

function TabelaLancamentos({ lancamentos }: { lancamentos: Lancamento[] }) {
  return (
    <div className="tabela-rolagem">
      <table>
        <thead>
          <tr>
            <th scope="col">Data</th>
            <th scope="col">Descrição</th>
            <th scope="col" className="direita">Valor</th>
          </tr>
        </thead>
        <tbody>
          {lancamentos.map((l) => {
            const entrada = l.valor >= 0
            return (
              <tr key={l.id}>
                <td className="numero">{formatarData(l.data)}</td>
                <td>
                  <span className="texto-truncado" title={l.descricao}>
                    {l.descricao}
                  </span>
                </td>
                {/* A seta e o sinal acompanham a cor: nenhum estado depende só dela. */}
                <td className={`direita numero ${entrada ? 'entrada' : 'saida'}`}>
                  <span aria-hidden="true">{entrada ? '↑ ' : '↓ '}</span>
                  {moeda(l.valor)}
                  <span className="visually-hidden"> {entrada ? '(entrada)' : '(saída)'}</span>
                </td>
              </tr>
            )
          })}
        </tbody>
      </table>
    </div>
  )
}

function FormularioLancamento({
  operacao,
  cursor,
  aoFechar,
  aoConcluir,
}: {
  operacao: Operacao
  cursor: string
  aoFechar: () => void
  aoConcluir: (texto: string) => void | Promise<void>
}) {
  const [valor, definirValor] = useState('')
  const [descricao, definirDescricao] = useState('')
  const [data, definirData] = useState(cursor)
  const [erroCampo, definirErroCampo] = useState<Record<string, string>>({})
  const [erroDominio, definirErroDominio] = useState<string | null>(null)
  const [enviando, definirEnviando] = useState(false)

  const alterado = valor !== '' || descricao !== '' || data !== cursor
  const credito = operacao === 'credito'

  function fechar() {
    // Perguntar sempre treinaria o usuário a ignorar a pergunta; só perguntamos quando há o que perder.
    if (alterado && !window.confirm('Descartar as alterações?')) return
    aoFechar()
  }

  /** Espelho da validação do servidor, para dar retorno imediato. A verdade continua sendo a dele. */
  function validar(): boolean {
    const erros: Record<string, string> = {}
    const numero = paraNumero(valor)
    if (numero === null || numero <= 0) erros.valor = 'Informe um valor positivo.'
    else if (casasDecimais(valor) > 2) erros.valor = 'O valor admite no máximo duas casas decimais.'
    if (descricao.trim() === '') erros.descricao = 'A descrição é obrigatória.'
    if (!data) erros.data = 'A data é obrigatória.'
    definirErroCampo(erros)
    return Object.keys(erros).length === 0
  }

  async function submeter() {
    if (!validar()) return
    definirEnviando(true)
    definirErroDominio(null)
    try {
      const numero = paraNumero(valor)!
      if (credito) await api.credito(numero, descricao, data)
      else await api.debito(numero, descricao, data)
      await aoConcluir(credito ? 'Crédito lançado.' : 'Débito lançado.')
    } catch (falha) {
      // 400 é erro de campo e volta para o campo; 409 é conflito com o estado e fica no rodapé,
      // com os valores digitados preservados.
      if (falha instanceof ErroApi && falha.status === 400) {
        definirErroCampo({ valor: falha.message })
      } else if (falha instanceof ErroApi) {
        definirErroDominio(falha.message)
      } else {
        definirErroDominio('Sem conexão com o servidor.')
      }
    } finally {
      definirEnviando(false)
    }
  }

  return (
    <Drawer
      aberto
      titulo={credito ? 'Novo crédito' : 'Novo débito'}
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
            {enviando ? 'Salvando…' : credito ? 'Lançar crédito' : 'Lançar débito'}
          </button>
        </>
      }
    >
      <Campo rotulo="Valor (R$)" id="valor" erro={erroCampo.valor}>
        <input
          id="valor"
          inputMode="decimal"
          value={valor}
          readOnly={enviando}
          aria-invalid={Boolean(erroCampo.valor)}
          aria-describedby={erroCampo.valor ? 'valor-erro' : undefined}
          onChange={(e) => definirValor(e.target.value)}
        />
      </Campo>

      <Campo rotulo="Descrição" id="descricao" erro={erroCampo.descricao}>
        <input
          id="descricao"
          value={descricao}
          readOnly={enviando}
          aria-invalid={Boolean(erroCampo.descricao)}
          onChange={(e) => definirDescricao(e.target.value)}
        />
      </Campo>

      <Campo rotulo="Data do movimento" id="data" erro={erroCampo.data}>
        <input
          id="data"
          type="date"
          value={data}
          readOnly={enviando}
          onChange={(e) => definirData(e.target.value)}
        />
      </Campo>
    </Drawer>
  )
}
