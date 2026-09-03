import { expect, test } from '@playwright/test'
import { DIA_UTIL, definirCursorDeData, entrar, lancar } from './apoio'

test.describe('Consulta de posição em segundo plano', () => {
  test('recalcular em segundo plano conclui e atualiza a posição', async ({ page }) => {
    await entrar(page, 'usuario8', 'senha8')
    await definirCursorDeData(page, DIA_UTIL)
    await lancar(page, 'crédito', '500,00', 'aporte para posição assíncrona')

    await page.getByRole('button', { name: 'Carteira', exact: true }).click()
    await page.getByRole('button', { name: 'Nova compra' }).click()
    const compra = page.getByRole('dialog')
    await compra.getByLabel('Ativo').selectOption('ATIVO5')
    await compra.getByLabel('Quantidade').fill('3,00')
    await compra.getByLabel('Valor da movimentação (R$)').fill('30,00')
    await compra.getByRole('button', { name: 'Comprar' }).click()
    await expect(page.getByRole('status')).toContainText('Compra registrada')

    await page.getByRole('button', { name: 'Recalcular em segundo plano' }).click()

    // O 425 é progresso, não erro: enquanto processa não aparece alerta nenhum, e ao concluir a
    // tela informa o horário da atualização.
    await expect(page.getByText(/Atualizada às/)).toBeVisible({ timeout: 30_000 })
    await expect(page.getByRole('row', { name: /ATIVO5/ })).toContainText('3,00')
  })

  test('a tela não apresenta erro enquanto a execução está pendente', async ({ page }) => {
    await entrar(page, 'usuario9', 'senha9')
    await definirCursorDeData(page, DIA_UTIL)

    await page.getByRole('button', { name: 'Carteira', exact: true }).click()
    await page.getByRole('button', { name: 'Recalcular em segundo plano' }).click()

    await expect(page.getByText('Não foi possível carregar')).toBeHidden()
    await expect(page.getByText(/Atualizada às/)).toBeVisible({ timeout: 30_000 })
  })
})
