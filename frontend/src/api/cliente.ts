/**
 * Cliente HTTP da API Minerva.
 *
 * A credencial vive em memória, nunca em `localStorage`: HTTP Basic é a senha em claro codificada em
 * base64, e gravá-la em disco a deixaria legível para qualquer script na origem e para quem tiver
 * acesso ao perfil do navegador. O custo aceito — entrar de novo ao recarregar a página — está
 * registrado na seção 7 de docs/design/telas.md.
 */

let credencial: string | null = null

/** Notificado quando o servidor recusa a credencial, para a aplicação voltar à tela de entrada. */
let aoExpirar: () => void = () => {}

export function definirCredencial(login: string, senha: string): void {
  credencial = 'Basic ' + btoa(`${login}:${senha}`)
}

export function limparCredencial(): void {
  credencial = null
}

export function aoExpirarSessao(callback: () => void): void {
  aoExpirar = callback
}

/** Erro com o código e o status do contrato de erro único da API. */
export class ErroApi extends Error {
  constructor(
    readonly status: number,
    readonly codigo: string,
    mensagem: string,
  ) {
    super(mensagem)
  }
}

/** Falha de transporte: o servidor não respondeu. É diferente de o servidor responder erro. */
export class ErroDeRede extends Error {
  constructor() {
    super('Sem conexão com o servidor.')
  }
}

interface Opcoes {
  metodo?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  corpo?: unknown
}

async function requisitar<T>(caminho: string, opcoes: Opcoes = {}): Promise<T> {
  const cabecalhos: Record<string, string> = {}
  if (credencial) cabecalhos['Authorization'] = credencial
  if (opcoes.corpo !== undefined) cabecalhos['Content-Type'] = 'application/json'

  let resposta: Response
  try {
    resposta = await fetch(caminho, {
      method: opcoes.metodo ?? 'GET',
      headers: cabecalhos,
      body: opcoes.corpo === undefined ? undefined : JSON.stringify(opcoes.corpo),
    })
  } catch {
    throw new ErroDeRede()
  }

  if (resposta.status === 401) {
    limparCredencial()
    aoExpirar()
    throw new ErroApi(401, 'NAO_AUTENTICADO', 'Sua sessão expirou. Entre novamente.')
  }

  if (!resposta.ok) {
    // O corpo de erro segue o contrato único {codigo, mensagem}. Se vier algo fora dele, não
    // inventamos texto: usamos uma mensagem genérica em vez de exibir lixo ao usuário.
    let codigo = 'ERRO_INTERNO'
    let mensagem = 'O servidor não conseguiu concluir. Tente de novo em instantes.'
    try {
      const corpo = await resposta.json()
      if (typeof corpo?.codigo === 'string') codigo = corpo.codigo
      if (typeof corpo?.mensagem === 'string') mensagem = corpo.mensagem
    } catch {
      /* resposta sem corpo JSON: a mensagem padrão acima já serve */
    }
    throw new ErroApi(resposta.status, codigo, mensagem)
  }

  if (resposta.status === 204 || resposta.headers.get('Content-Length') === '0') {
    return undefined as T
  }
  const texto = await resposta.text()
  return (texto === '' ? undefined : JSON.parse(texto)) as T
}

/** Status cru, para o polling da posição assíncrona, onde 425 é progresso e não erro. */
export async function consultarExecucao<T>(id: number): Promise<{ status: number; corpo: T | null }> {
  const cabecalhos: Record<string, string> = {}
  if (credencial) cabecalhos['Authorization'] = credencial

  let resposta: Response
  try {
    resposta = await fetch(`/posicao/${id}`, { headers: cabecalhos })
  } catch {
    throw new ErroDeRede()
  }

  if (resposta.status === 401) {
    limparCredencial()
    aoExpirar()
    throw new ErroApi(401, 'NAO_AUTENTICADO', 'Sua sessão expirou. Entre novamente.')
  }
  if (resposta.status === 425) return { status: 425, corpo: null }
  if (resposta.status === 200) return { status: 200, corpo: (await resposta.json()) as T }
  return { status: resposta.status, corpo: null }
}

/** Executa o fluxo completo da posição, mantendo 425 como estado de carregamento. */
export async function consultarPosicao(data: string): Promise<import('./tipos').Posicao[]> {
  const execucao = await requisitar<import('./tipos').Execucao>(`/posicao?data=${data}`)
  const limite = Date.now() + 60_000
  while (Date.now() < limite) {
    await new Promise((resolver) => setTimeout(resolver, 600))
    const resultado = await consultarExecucao<import('./tipos').Posicao[]>(execucao.id)
    if (resultado.status === 200 && resultado.corpo) return resultado.corpo
    if (resultado.status === 404) {
      throw new ErroApi(404, 'RECURSO_NAO_ENCONTRADO', 'O resultado da posição expirou. Tente novamente.')
    }
    if (resultado.status !== 425) {
      throw new ErroApi(resultado.status, 'ERRO_INTERNO', 'O servidor não conseguiu concluir o cálculo.')
    }
  }
  throw new ErroApi(408, 'TEMPO_ESGOTADO', 'O cálculo está demorando mais que o esperado. Tente novamente.')
}

export const api = {
  requisitar,

  credito: (valor: number, descricao: string, data: string) =>
    requisitar<void>('/contacorrente/credito', { metodo: 'POST', corpo: { valor, descricao, data } }),

  debito: (valor: number, descricao: string, data: string) =>
    requisitar<void>('/contacorrente/debito', { metodo: 'POST', corpo: { valor, descricao, data } }),

  saldo: (data: string) => requisitar<{ saldo: number }>(`/contacorrente/saldo?data=${data}`),

  lancamentos: (inicio: string, fim: string) =>
    requisitar<import('./tipos').Lancamento[]>(
      `/contacorrente/lancamentos?dataInicio=${inicio}&dataFim=${fim}`,
    ),

  ativos: () => requisitar<import('./tipos').Ativo[]>('/ativos'),

  ativo: (codigo: string) => requisitar<import('./tipos').Ativo>(`/ativos/${encodeURIComponent(codigo)}`),

  criarAtivo: (ativo: Omit<import('./tipos').Ativo, never>) =>
    requisitar<void>('/ativos', { metodo: 'POST', corpo: ativo }),

  alterarAtivo: (codigo: string, ativo: import('./tipos').Ativo) =>
    requisitar<import('./tipos').Ativo>(`/ativos/${encodeURIComponent(codigo)}`, {
      metodo: 'PUT',
      corpo: ativo,
    }),

  removerAtivo: (codigo: string) =>
    requisitar<void>(`/ativos/${encodeURIComponent(codigo)}`, { metodo: 'DELETE' }),

  definirPreco: (codigo: string, data: string, precoMercado: number) =>
    requisitar<{ ativo: string; data: string; precoMercado: number }>(
      `/ativos/${encodeURIComponent(codigo)}/precos/${data}`,
      { metodo: 'PUT', corpo: { precoMercado } },
    ),

  removerPreco: (codigo: string, data: string) =>
    requisitar<void>(`/ativos/${encodeURIComponent(codigo)}/precos/${data}`, { metodo: 'DELETE' }),

  comprar: (ativo: string, data: string, quantidade: number, valor: number) =>
    requisitar<void>('/movimentacao/compra', { metodo: 'POST', corpo: { ativo, data, quantidade, valor } }),

  vender: (ativo: string, data: string, quantidade: number, valor: number) =>
    requisitar<void>('/movimentacao/venda', { metodo: 'POST', corpo: { ativo, data, quantidade, valor } }),

  movimentacoes: (inicio: string, fim: string) =>
    requisitar<import('./tipos').Movimentacao[]>(
      `/movimentacao?dataInicio=${inicio}&dataFim=${fim}`,
    ),

  posicao: (data: string) => consultarPosicao(data),

  solicitarPosicao: (data: string) =>
    requisitar<import('./tipos').Execucao>(`/posicao?data=${data}`),
}
