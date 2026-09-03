/** Contratos de resposta da API, espelhando o que o backend devolve. */

export type TipoAtivo = 'RV' | 'RF' | 'FUNDO'

export interface Ativo {
  ativo: string
  nome: string
  tipo: TipoAtivo
  dataEmissao: string
  dataVencimento: string
}

export interface Lancamento {
  id: number
  data: string
  valor: number
  descricao: string
}

export interface Movimentacao {
  id: number
  ativo: string
  data: string
  tipo: 'COMPRA' | 'VENDA'
  quantidade: number
  valor: number
}

/**
 * Os campos de mercado são anuláveis de propósito: quando não há preço com data menor ou igual à
 * consulta, o servidor devolve `null` em vez de inventar um preço (decisão D-B3).
 */
export interface Posicao {
  ativo: string
  nome: string
  tipo: TipoAtivo
  quantidade: number
  precoMercado: number | null
  valorMercadoTotal: number | null
  precoMedio: number | null
  rendimento: number | null
  lucro: number
}

export interface Saldo {
  saldo: number
}

export interface Execucao {
  id: number
}
