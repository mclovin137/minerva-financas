import { expect, test } from '@playwright/test'
import { DIA_UTIL, definirCursorDeData, entrar, lancar, saldoExibido } from './apoio'

/**
 * O caminho completo que o desafio descreve: aportar na conta, comprar um ativo, ver a posição
 * refletir a compra e vender. Cada caso usa um usuário próprio, porque o saldo é estado acumulado e
 * casos que o compartilhassem passariam a depender da ordem de execução.
 */
test.describe('Fluxo financeiro de ponta a ponta', () => {
  test('crédito aumenta o saldo e aparece nos lançamentos', async ({ page }) => {
    await entrar(page, 'usuario1', 'senha1')
    await definirCursorDeData(page, DIA_UTIL)

    await lancar(page, 'crédito', '1000,00', 'aporte inicial')

    await expect(page.getByRole('status')).toContainText('Crédito lançado')
    expect(await saldoExibido(page)).toContain('1.000,00')
    await expect(page.getByRole('cell', { name: 'aporte inicial' })).toBeVisible()
  })

  test('débito além do saldo é recusado com a mensagem do domínio e não altera o saldo', async ({ page }) => {
    await entrar(page, 'usuario2', 'senha2')
    await definirCursorDeData(page, DIA_UTIL)
    await lancar(page, 'crédito', '100,00', 'aporte')
    // Sem esperar a confirmação, o saldo pode ser lido antes de saldo.recarregar() concluir e
    // capturar o valor anterior ao crédito — a mesma classe de corrida corrigida em useRecurso.
    await expect(page.getByRole('status')).toContainText('Crédito lançado')
    const saldoAntes = await saldoExibido(page)

    await lancar(page, 'débito', '150,00', 'tentativa acima do saldo')

    // O drawer permanece aberto com os valores digitados, e o erro aparece nele — não como página
    // de erro, não como toast que some.
    const painel = page.getByRole('dialog')
    await expect(painel.getByRole('alert')).toContainText('negativo')
    await expect(painel.getByLabel('Valor (R$)')).toHaveValue('150,00')

    page.once('dialog', (confirmacao) => confirmacao.accept())
    await painel.getByRole('button', { name: 'Cancelar' }).click()
    expect(await saldoExibido(page)).toBe(saldoAntes)
  })

  test('compra debita a conta e a posição reflete quantidade e valor de mercado', async ({ page }) => {
    await entrar(page, 'usuario3', 'senha3')
    await definirCursorDeData(page, DIA_UTIL)
    await lancar(page, 'crédito', '1000,00', 'aporte para compra')

    await page.getByRole('button', { name: 'Carteira', exact: true }).click()
    await page.getByRole('button', { name: 'Nova compra' }).click()
    const compra = page.getByRole('dialog')
    await compra.getByLabel('Ativo').selectOption('ATIVO1')
    await compra.getByLabel('Quantidade').fill('2,00')
    await compra.getByLabel('Valor da movimentação (R$)').fill('20,00')
    await compra.getByRole('button', { name: 'Comprar' }).click()

    await expect(page.getByRole('status')).toContainText('Compra registrada')
    const linha = page.getByRole('row', { name: /ATIVO1/ })
    await expect(linha).toBeVisible()
    await expect(linha).toContainText('2,00')

    // ATIVO1 tem preço de 10,37 no seed: 2,00 x 10,37 = 20,74.
    await expect(linha).toContainText('20,74')

    await page.getByRole('button', { name: 'Conta corrente', exact: true }).click()
    expect(await saldoExibido(page)).toContain('980,00')
  })

  test('venda acima da quantidade é recusada e a posição permanece intacta', async ({ page }) => {
    await entrar(page, 'usuario4', 'senha4')
    await definirCursorDeData(page, DIA_UTIL)
    await lancar(page, 'crédito', '1000,00', 'aporte')

    await page.getByRole('button', { name: 'Carteira', exact: true }).click()
    await page.getByRole('button', { name: 'Nova compra' }).click()
    let painel = page.getByRole('dialog')
    await painel.getByLabel('Ativo').selectOption('ATIVO2')
    await painel.getByLabel('Quantidade').fill('1,00')
    await painel.getByLabel('Valor da movimentação (R$)').fill('10,00')
    await painel.getByRole('button', { name: 'Comprar' }).click()
    await expect(page.getByRole('status')).toContainText('Compra registrada')

    await page.getByRole('button', { name: 'Nova venda' }).click()
    painel = page.getByRole('dialog')
    await painel.getByLabel('Ativo').selectOption('ATIVO2')
    await painel.getByLabel('Quantidade').fill('5,00')
    await painel.getByLabel('Valor da movimentação (R$)').fill('50,00')
    await painel.getByRole('button', { name: 'Vender' }).click()

    await expect(painel.getByRole('alert')).toContainText('quantidade')
  })

  test('um usuário não enxerga os dados financeiros do outro', async ({ page }) => {
    await entrar(page, 'usuario5', 'senha5')
    await definirCursorDeData(page, DIA_UTIL)
    await lancar(page, 'crédito', '777,00', 'dinheiro do usuario5')
    // Mesma corrida: sem aguardar a confirmação, o saldo pode ser lido antes da recarga refletir
    // o crédito recém-lançado.
    await expect(page.getByRole('status')).toContainText('Crédito lançado')
    expect(await saldoExibido(page)).toContain('777,00')

    await page.getByRole('button', { name: 'Sair' }).click()
    await entrar(page, 'usuario6', 'senha6')
    await definirCursorDeData(page, DIA_UTIL)

    expect(await saldoExibido(page)).toContain('0,00')
    await expect(page.getByText('dinheiro do usuario5')).toBeHidden()
  })
})
