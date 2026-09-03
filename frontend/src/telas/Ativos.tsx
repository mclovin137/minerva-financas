import { useState } from 'react'
import { api, ErroApi } from '../api/cliente'
import type { Ativo, TipoAtivo } from '../api/tipos'
import { Campo, Drawer } from '../componentes/Drawer'
import { Carregando, Erro, Vazio } from '../componentes/Estados'
import { ModalConfirmacao } from '../componentes/ModalConfirmacao'
import { data as formatarData } from '../formato'
import { useRecurso } from '../useRecurso'

const TIPOS: TipoAtivo[] = ['RV', 'RF', 'FUNDO']

/**
 * Acervo compartilhado: leitura para todos os papéis, escrita só para o administrador. Os botões de
 * escrita não são escondidos do usuário comum — ficam ausentes da tela dele, mas a rota continua
 * acessível para leitura, que é o que o enunciado especifica.
 */
export function Ativos({
  administrador,
  avisar,
}: {
  administrador: boolean
  avisar: (texto: string) => void
}) {
  const ativos = useRecurso(() => api.ativos(), [])
  const [emEdicao, definirEmEdicao] = useState<Ativo | 'novo' | null>(null)
  const [aRemover, definirARemover] = useState<Ativo | null>(null)

  return (
    <>
      <header>
        <h1>Ativos</h1>
        <div className="espacador" />
        {administrador && (
          <button type="button" className="acao primaria" onClick={() => definirEmEdicao('novo')}>
            Novo ativo
          </button>
        )}
      </header>

      {ativos.carregando && <Carregando rotulo="Carregando ativos" />}
      {ativos.erro && <Erro mensagem={ativos.erro} aoTentarDeNovo={ativos.recarregar} />}
      {!ativos.carregando && !ativos.erro && ativos.dados?.length === 0 && (
        <Vazio
          icone="▤"
          titulo="Nenhum ativo cadastrado"
          explicacao={
            administrador
              ? 'Cadastre o primeiro ativo do acervo compartilhado.'
              : 'O acervo ainda não tem ativos. Fale com um administrador.'
          }
          acao={
            administrador ? (
              <button type="button" className="acao primaria" onClick={() => definirEmEdicao('novo')}>
                Novo ativo
              </button>
            ) : undefined
          }
        />
      )}

      {(ativos.dados?.length ?? 0) > 0 && (
        <div className="tabela-rolagem">
          <table>
            <thead>
              <tr>
                <th scope="col">Código</th>
                <th scope="col">Nome</th>
                <th scope="col">Tipo</th>
                <th scope="col">Emissão</th>
                <th scope="col">Vencimento</th>
                {administrador && <th scope="col" className="direita">Ações</th>}
              </tr>
            </thead>
            <tbody>
              {ativos.dados!.map((a) => (
                <tr key={a.ativo}>
                  <td className="numero"><strong>{a.ativo}</strong></td>
                  <td><span className="texto-truncado" title={a.nome}>{a.nome}</span></td>
                  <td>{a.tipo}</td>
                  <td className="numero">{formatarData(a.dataEmissao)}</td>
                  <td className="numero">{formatarData(a.dataVencimento)}</td>
                  {administrador && (
                    <td className="direita">
                      <button type="button" className="discreta" onClick={() => definirEmEdicao(a)}>
                        Editar
                      </button>
                      <button type="button" className="discreta" onClick={() => definirARemover(a)}>
                        Remover
                      </button>
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {emEdicao && (
        <FormularioAtivo
          original={emEdicao === 'novo' ? null : emEdicao}
          aoFechar={() => definirEmEdicao(null)}
          aoConcluir={(texto) => {
            definirEmEdicao(null)
            avisar(texto)
            ativos.recarregar()
          }}
        />
      )}

      {aRemover && (
        <ConfirmarRemocao
          ativo={aRemover}
          aoCancelar={() => definirARemover(null)}
          aoConcluir={() => {
            definirARemover(null)
            avisar('Ativo removido.')
            ativos.recarregar()
          }}
        />
      )}
    </>
  )
}

function ConfirmarRemocao({
  ativo,
  aoCancelar,
  aoConcluir,
}: {
  ativo: Ativo
  aoCancelar: () => void
  aoConcluir: () => void
}) {
  const [enviando, definirEnviando] = useState(false)
  const [erro, definirErro] = useState<string | undefined>()

  async function remover() {
    definirEnviando(true)
    definirErro(undefined)
    try {
      await api.removerAtivo(ativo.ativo)
      aoConcluir()
    } catch (falha) {
      // O modal permanece aberto: um ativo com movimentações não pode ser removido, e o usuário
      // precisa ler o motivo no lugar onde pediu a remoção.
      definirErro(falha instanceof ErroApi ? falha.message : 'Sem conexão com o servidor.')
    } finally {
      definirEnviando(false)
    }
  }

  return (
    <ModalConfirmacao
      titulo={`Remover o ativo ${ativo.ativo}?`}
      consequencia="O ativo sai do acervo compartilhado. Ativos com movimentações não podem ser removidos."
      rotuloConfirmar="Remover"
      enviando={enviando}
      erro={erro}
      aoConfirmar={remover}
      aoCancelar={aoCancelar}
    />
  )
}

function FormularioAtivo({
  original,
  aoFechar,
  aoConcluir,
}: {
  original: Ativo | null
  aoFechar: () => void
  aoConcluir: (texto: string) => void
}) {
  const [codigo, definirCodigo] = useState(original?.ativo ?? '')
  const [nome, definirNome] = useState(original?.nome ?? '')
  const [tipo, definirTipo] = useState<TipoAtivo>(original?.tipo ?? 'RV')
  const [emissao, definirEmissao] = useState(original?.dataEmissao ?? '')
  const [vencimento, definirVencimento] = useState(original?.dataVencimento ?? '')
  const [erroCampo, definirErroCampo] = useState<Record<string, string>>({})
  const [erroDominio, definirErroDominio] = useState<string | null>(null)
  const [enviando, definirEnviando] = useState(false)

  function validar(): boolean {
    const erros: Record<string, string> = {}
    if (!codigo.trim()) erros.codigo = 'O código é obrigatório.'
    if (!nome.trim()) erros.nome = 'O nome é obrigatório.'
    if (!emissao) erros.emissao = 'A data de emissão é obrigatória.'
    if (!vencimento) erros.vencimento = 'A data de vencimento é obrigatória.'
    if (emissao && vencimento && emissao >= vencimento) {
      erros.vencimento = 'O vencimento deve ser posterior à emissão.'
    }
    definirErroCampo(erros)
    return Object.keys(erros).length === 0
  }

  async function submeter() {
    if (!validar()) return
    definirEnviando(true)
    definirErroDominio(null)
    const corpo: Ativo = {
      ativo: codigo.trim(),
      nome: nome.trim(),
      tipo,
      dataEmissao: emissao,
      dataVencimento: vencimento,
    }
    try {
      if (original) await api.alterarAtivo(original.ativo, corpo)
      else await api.criarAtivo(corpo)
      aoConcluir(original ? 'Ativo atualizado.' : 'Ativo cadastrado.')
    } catch (falha) {
      definirErroDominio(falha instanceof ErroApi ? falha.message : 'Sem conexão com o servidor.')
    } finally {
      definirEnviando(false)
    }
  }

  return (
    <Drawer
      aberto
      titulo={original ? `Editar ${original.ativo}` : 'Novo ativo'}
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
            {enviando ? 'Salvando…' : 'Salvar ativo'}
          </button>
        </>
      }
    >
      <Campo rotulo="Código" id="codigo" erro={erroCampo.codigo}>
        <input
          id="codigo"
          value={codigo}
          readOnly={enviando || Boolean(original)}
          aria-invalid={Boolean(erroCampo.codigo)}
          onChange={(e) => definirCodigo(e.target.value)}
        />
      </Campo>

      <Campo rotulo="Nome" id="nome" erro={erroCampo.nome}>
        <input
          id="nome"
          value={nome}
          readOnly={enviando}
          aria-invalid={Boolean(erroCampo.nome)}
          onChange={(e) => definirNome(e.target.value)}
        />
      </Campo>

      <Campo rotulo="Tipo" id="tipo">
        <select
          id="tipo"
          value={tipo}
          disabled={enviando}
          onChange={(e) => definirTipo(e.target.value as TipoAtivo)}
        >
          {TIPOS.map((t) => (
            <option key={t} value={t}>{t}</option>
          ))}
        </select>
      </Campo>

      <Campo rotulo="Data de emissão" id="emissao" erro={erroCampo.emissao}>
        <input
          id="emissao"
          type="date"
          value={emissao}
          readOnly={enviando}
          aria-invalid={Boolean(erroCampo.emissao)}
          onChange={(e) => definirEmissao(e.target.value)}
        />
      </Campo>

      <Campo rotulo="Data de vencimento" id="vencimento" erro={erroCampo.vencimento}>
        <input
          id="vencimento"
          type="date"
          value={vencimento}
          readOnly={enviando}
          aria-invalid={Boolean(erroCampo.vencimento)}
          onChange={(e) => definirVencimento(e.target.value)}
        />
      </Campo>
    </Drawer>
  )
}
