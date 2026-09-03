import { useEffect, useState } from 'react'
import { api, ErroApi } from '../api/cliente'
import { Campo, Drawer } from '../componentes/Drawer'
import { Carregando, Erro, Vazio } from '../componentes/Estados'
import { ModalConfirmacao } from '../componentes/ModalConfirmacao'
import { casasDecimais, data as formatarData, paraNumero, preco } from '../formato'
import { useRecurso } from '../useRecurso'

/**
 * Preço de mercado por data. A API não expõe listagem de histórico de preços — a rota foi cortada
 * por falta de demanda —, então esta tela mostra o preço **vigente** na data do cursor, obtido pela
 * consulta de posição do ativo, e permite ao administrador definir e remover preços por data.
 */
export function Mercado({
  cursor,
  administrador,
  avisar,
}: {
  cursor: string
  administrador: boolean
  avisar: (texto: string) => void
}) {
  const ativos = useRecurso(() => api.ativos(), [])
  const [selecionado, definirSelecionado] = useState('')
  const [drawerAberto, definirDrawerAberto] = useState(false)
  const [aRemover, definirARemover] = useState<{ ativo: string; data: string } | null>(null)

  return (
    <>
      <header>
        <h1>Mercado histórico</h1>
        <div className="espacador" />
        {administrador && (
          <button
            type="button"
            className="acao primaria"
            onClick={() => definirDrawerAberto(true)}
            disabled={!selecionado}
          >
            Definir preço
          </button>
        )}
      </header>

      {ativos.carregando && <Carregando rotulo="Carregando ativos" />}
      {ativos.erro && <Erro mensagem={ativos.erro} aoTentarDeNovo={ativos.recarregar} />}

      {!ativos.carregando && !ativos.erro && (
        <>
          <div className="campo" style={{ maxWidth: 420, marginBottom: 'var(--space-4)' }}>
            <label htmlFor="ativo-mercado">Ativo</label>
            <select
              id="ativo-mercado"
              value={selecionado}
              onChange={(e) => definirSelecionado(e.target.value)}
            >
              <option value="">Selecione…</option>
              {(ativos.dados ?? []).map((a) => (
                <option key={a.ativo} value={a.ativo}>
                  {a.ativo} — {a.nome}
                </option>
              ))}
            </select>
          </div>

          {!selecionado ? (
            <Vazio
              icone="⌇"
              titulo="Escolha um ativo para ver os preços"
              explicacao="O preço exibido é o mais recente com data igual ou anterior ao cursor."
            />
          ) : (
            <PrecoVigente
              ativo={selecionado}
              cursor={cursor}
              administrador={administrador}
              aoRemover={() => definirARemover({ ativo: selecionado, data: cursor })}
            />
          )}
        </>
      )}

      {drawerAberto && (
        <FormularioPreco
          ativo={selecionado}
          cursor={cursor}
          aoFechar={() => definirDrawerAberto(false)}
          aoConcluir={(texto) => {
            definirDrawerAberto(false)
            avisar(texto)
            definirSelecionado((atual) => atual)
            window.dispatchEvent(new Event('minerva:precos-alterados'))
          }}
        />
      )}

      {aRemover && (
        <RemocaoDePreco
          alvo={aRemover}
          aoCancelar={() => definirARemover(null)}
          aoConcluir={() => {
            definirARemover(null)
            avisar('Preço removido.')
            window.dispatchEvent(new Event('minerva:precos-alterados'))
          }}
        />
      )}
    </>
  )
}

function PrecoVigente({
  ativo,
  cursor,
  administrador,
  aoRemover,
}: {
  ativo: string
  cursor: string
  administrador: boolean
  aoRemover: () => void
}) {
  const [versao, definirVersao] = useState(0)

  // Assinar o evento tem de acontecer em efeito, não no corpo do render: no render, cada
  // renderização registraria mais um ouvinte e nenhum seria removido.
  useEffect(() => {
    const recarregar = () => definirVersao((n) => n + 1)
    window.addEventListener('minerva:precos-alterados', recarregar)
    return () => window.removeEventListener('minerva:precos-alterados', recarregar)
  }, [])

  const posicao = useRecurso(() => api.posicao(cursor), [cursor, versao])
  const linha = (posicao.dados ?? []).find((p) => p.ativo === ativo)

  if (posicao.carregando) return <Carregando linhas={3} rotulo="Carregando preço" />
  if (posicao.erro) return <Erro mensagem={posicao.erro} aoTentarDeNovo={posicao.recarregar} />

  return (
    <>
      <div className="indicadores">
        <div className="indicador destaque">
          <div className="rotulo">Preço vigente em {formatarData(cursor)}</div>
          <div className="valor numero">
            {linha?.precoMercado != null ? preco(linha.precoMercado) : '—'}
          </div>
        </div>
      </div>

      {linha?.precoMercado == null && (
        <p style={{ color: 'var(--color-text-secondary)' }}>
          Sem preço de mercado cadastrado até esta data. A posição do ativo é exibida sem valor de
          mercado, e nenhum preço posterior é usado no lugar.
        </p>
      )}

      {administrador && (
        <button type="button" className="acao" onClick={aoRemover}>
          Remover o preço de {formatarData(cursor)}
        </button>
      )}
    </>
  )
}

function RemocaoDePreco({
  alvo,
  aoCancelar,
  aoConcluir,
}: {
  alvo: { ativo: string; data: string }
  aoCancelar: () => void
  aoConcluir: () => void
}) {
  const [enviando, definirEnviando] = useState(false)
  const [erro, definirErro] = useState<string | undefined>()

  async function remover() {
    definirEnviando(true)
    definirErro(undefined)
    try {
      await api.removerPreco(alvo.ativo, alvo.data)
      aoConcluir()
    } catch (falha) {
      definirErro(falha instanceof ErroApi ? falha.message : 'Sem conexão com o servidor.')
    } finally {
      definirEnviando(false)
    }
  }

  return (
    <ModalConfirmacao
      titulo={`Remover o preço de ${alvo.ativo} em ${formatarData(alvo.data)}?`}
      consequencia="As consultas nessa data passarão a usar o preço anterior mais recente, ou nenhum, se não houver."
      rotuloConfirmar="Remover"
      enviando={enviando}
      erro={erro}
      aoConfirmar={remover}
      aoCancelar={aoCancelar}
    />
  )
}

function FormularioPreco({
  ativo,
  cursor,
  aoFechar,
  aoConcluir,
}: {
  ativo: string
  cursor: string
  aoFechar: () => void
  aoConcluir: (texto: string) => void
}) {
  const [data, definirData] = useState(cursor)
  const [valor, definirValor] = useState('')
  const [erroCampo, definirErroCampo] = useState<Record<string, string>>({})
  const [erroDominio, definirErroDominio] = useState<string | null>(null)
  const [enviando, definirEnviando] = useState(false)

  function validar(): boolean {
    const erros: Record<string, string> = {}
    const numero = paraNumero(valor)
    if (numero === null || numero < 0) erros.valor = 'Informe um preço não negativo.'
    else if (casasDecimais(valor) > 8) erros.valor = 'O preço admite no máximo oito casas decimais.'
    if (!data) erros.data = 'A data é obrigatória.'
    definirErroCampo(erros)
    return Object.keys(erros).length === 0
  }

  async function submeter() {
    if (!validar()) return
    definirEnviando(true)
    definirErroDominio(null)
    try {
      await api.definirPreco(ativo, data, paraNumero(valor)!)
      aoConcluir('Preço de mercado salvo.')
    } catch (falha) {
      definirErroDominio(falha instanceof ErroApi ? falha.message : 'Sem conexão com o servidor.')
    } finally {
      definirEnviando(false)
    }
  }

  return (
    <Drawer
      aberto
      titulo={`Preço de mercado de ${ativo}`}
      aoFechar={aoFechar}
      rodape={
        <>
          {erroDominio && (
            <span className="erro-dominio" role="alert">
              {erroDominio}
            </span>
          )}
          <button type="button" className="acao" onClick={aoFechar} disabled={enviando}>
            Cancelar
          </button>
          <button type="button" className="acao primaria" onClick={submeter} disabled={enviando}>
            {enviando ? 'Salvando…' : 'Salvar preço'}
          </button>
        </>
      }
    >
      <Campo rotulo="Data" id="data-preco" erro={erroCampo.data}>
        <input
          id="data-preco"
          type="date"
          value={data}
          readOnly={enviando}
          onChange={(e) => definirData(e.target.value)}
        />
      </Campo>

      <Campo rotulo="Preço de mercado (R$)" id="valor-preco" erro={erroCampo.valor}>
        <input
          id="valor-preco"
          inputMode="decimal"
          value={valor}
          readOnly={enviando}
          aria-invalid={Boolean(erroCampo.valor)}
          onChange={(e) => definirValor(e.target.value)}
        />
      </Campo>
    </Drawer>
  )
}
