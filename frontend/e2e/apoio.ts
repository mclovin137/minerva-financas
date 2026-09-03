import { expect, type Page } from '@playwright/test'

/** 2020-02-28 é uma sexta-feira dentro da janela de negociação de todo ativo do seed. */
export const DIA_UTIL = '2020-02-28'

export async function entrar(pagina: Page, usuario: string, senha: string) {
  await pagina.goto('/')
  await pagina.getByLabel('Usuário').fill(usuario)
  await pagina.getByLabel('Senha').fill(senha)
  await pagina.getByRole('button', { name: 'Entrar' }).click()
  await expect(pagina.getByRole('button', { name: 'Sair' })).toBeVisible()
}

export async function definirCursorDeData(pagina: Page, data: string) {
  await pagina.getByLabel('Data de referência').fill(data)
}

export async function irPara(pagina: Page, destino: string) {
  await pagina.getByRole('button', { name: destino, exact: true }).click()
  await expect(pagina.getByRole('heading', { level: 1 })).toBeVisible()
}

/** Lê o saldo do indicador da conta corrente, já sem a formatação de moeda. */
export async function saldoExibido(pagina: Page): Promise<string> {
  const indicador = pagina.locator('.indicador.destaque .valor')
  await expect(indicador).toBeVisible()
  // O indicador permanece visível durante a recarga; aguardar apenas visibilidade pode ler o
  // placeholder antes de saldo.recarregar() concluir.
  await expect(indicador).not.toHaveText('…')
  return (await indicador.innerText()).trim()
}

export async function lancar(pagina: Page, operacao: 'crédito' | 'débito', valor: string, descricao: string) {
  const botaoAbrir = operacao === 'crédito' ? 'Novo crédito' : 'Novo débito'
  const botaoConfirmar = operacao === 'crédito' ? 'Lançar crédito' : 'Lançar débito'

  await pagina.getByRole('button', { name: botaoAbrir }).click()
  const painel = pagina.getByRole('dialog')
  await painel.getByLabel('Valor (R$)').fill(valor)
  await painel.getByLabel('Descrição').fill(descricao)
  await painel.getByRole('button', { name: botaoConfirmar }).click()
}
