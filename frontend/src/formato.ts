/**
 * Formatação de exibição, conforme a seção 1 de docs/design/telas.md.
 *
 * A regra que organiza este arquivo: a interface exibe em pt-BR (`28/02/2020`, `R$ 1.234,56`) e
 * conversa com a API no formato do domínio (`2020-02-28`, número JSON). A conversão acontece só
 * aqui, nas bordas — espalhá-la pelos componentes é como uma tela acaba mostrando data americana.
 */

const MOEDA = new Intl.NumberFormat('pt-BR', {
  style: 'currency',
  currency: 'BRL',
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
})

const DECIMAL_2 = new Intl.NumberFormat('pt-BR', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
})

/** Zeros à direita são suprimidos a partir da terceira casa: `105,53`, mas `1,23456789`. */
const PRECO = new Intl.NumberFormat('pt-BR', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 8,
})

const RENDIMENTO = new Intl.NumberFormat('pt-BR', {
  minimumFractionDigits: 4,
  maximumFractionDigits: 4,
})

/** Campo indisponível, e não zero. Vazio significaria "não sei"; `—` diz que não há valor. */
export const AUSENTE = '—'

export function moeda(valor: number | null | undefined): string {
  return valor === null || valor === undefined ? AUSENTE : MOEDA.format(valor)
}

export function quantidade(valor: number | null | undefined): string {
  return valor === null || valor === undefined ? AUSENTE : DECIMAL_2.format(valor)
}

export function preco(valor: number | null | undefined): string {
  return valor === null || valor === undefined ? AUSENTE : PRECO.format(valor)
}

/** Rendimento com o percentual explícito ao lado, que é o que a pessoa realmente lê. */
export function rendimento(valor: number | null | undefined): string {
  if (valor === null || valor === undefined) return AUSENTE
  const variacao = (valor - 1) * 100
  const sinal = variacao >= 0 ? '+' : '−'
  return `${RENDIMENTO.format(valor)} (${sinal}${DECIMAL_2.format(Math.abs(variacao))}%)`
}

/** `2020-02-28` → `28/02/2020`. Sem `new Date`, que aplicaria fuso e poderia trocar o dia. */
export function data(iso: string): string {
  const [ano, mes, dia] = iso.split('-')
  return `${dia}/${mes}/${ano}`
}

export function hoje(): string {
  const agora = new Date()
  const mes = String(agora.getMonth() + 1).padStart(2, '0')
  const dia = String(agora.getDate()).padStart(2, '0')
  return `${agora.getFullYear()}-${mes}-${dia}`
}

/** Primeiro dia do mês do cursor, usado como início padrão dos filtros de período. */
export function inicioDoMes(iso: string): string {
  return `${iso.slice(0, 7)}-01`
}

export function horaAtual(): string {
  return new Date().toLocaleTimeString('pt-BR')
}

/**
 * Converte o que a pessoa digitou em número. Aceita vírgula e ponto, porque exigir um separador
 * específico é atrito sem propósito num campo de valor.
 */
export function paraNumero(texto: string): number | null {
  const limpo = texto.trim().replace(/\./g, '').replace(',', '.')
  if (limpo === '') return null
  const numero = Number(limpo)
  return Number.isFinite(numero) ? numero : null
}

/** Casas decimais do que foi digitado, para espelhar a validação de escala do servidor. */
export function casasDecimais(texto: string): number {
  const limpo = texto.trim().replace(/\./g, '').replace(',', '.')
  const ponto = limpo.indexOf('.')
  return ponto < 0 ? 0 : limpo.length - ponto - 1
}
