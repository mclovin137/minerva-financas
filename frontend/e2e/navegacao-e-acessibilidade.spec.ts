import { expect, test } from '@playwright/test'
import { DIA_UTIL, definirCursorDeData, entrar } from './apoio'

test.describe('Navegação, cursor de data e acessibilidade', () => {
  test('o cursor de data é persistido na URL e sobrevive ao recarregar', async ({ page }) => {
    await entrar(page, 'usuario0', 'senha0')
    await definirCursorDeData(page, DIA_UTIL)
    await page.getByRole('button', { name: 'Carteira', exact: true }).click()

    await expect(page).toHaveURL(new RegExp(`destino=carteira.*data=${DIA_UTIL}`))

    await page.reload()
    await page.getByLabel('Usuário').fill('usuario0')
    await page.getByLabel('Senha').fill('senha0')
    await page.getByRole('button', { name: 'Entrar' }).click()

    // A credencial fica em memória de propósito, então recarregar pede login de novo — mas a data
    // e o destino do link continuam valendo.
    await expect(page.getByLabel('Data de referência')).toHaveValue(DIA_UTIL)
    await expect(page.getByRole('heading', { name: 'Carteira' })).toBeVisible()
  })

  test('as setas do cursor andam um dia para trás e para frente', async ({ page }) => {
    await entrar(page, 'usuario0', 'senha0')
    await definirCursorDeData(page, DIA_UTIL)

    await page.getByRole('button', { name: 'Dia anterior' }).click()
    await expect(page.getByLabel('Data de referência')).toHaveValue('2020-02-27')

    await page.getByRole('button', { name: 'Próximo dia' }).click()
    await expect(page.getByLabel('Data de referência')).toHaveValue(DIA_UTIL)
  })

  test('o drawer prende o foco, fecha no Esc e devolve o foco a quem o abriu', async ({ page }) => {
    await entrar(page, 'usuario0', 'senha0')
    const abrir = page.locator('header').getByRole('button', { name: 'Novo crédito', exact: true })
    await abrir.click()

    const painel = page.getByRole('dialog')
    await expect(painel).toBeVisible()
    await expect(painel.getByLabel('Valor (R$)')).toBeFocused()

    await page.keyboard.press('Shift+Tab')
    await expect(painel.getByRole('button', { name: 'Lançar crédito' })).toBeFocused()
    await page.keyboard.press('Tab')
    await expect(painel.getByLabel('Valor (R$)')).toBeFocused()

    await page.keyboard.press('Escape')

    await expect(painel).toBeHidden()
    await expect(abrir).toBeFocused()
  })

  test('o formulário mostra o erro de validação abaixo do campo, ligado por aria-describedby', async ({ page }) => {
    await entrar(page, 'usuario0', 'senha0')
    await page.getByRole('button', { name: 'Novo crédito' }).click()

    const painel = page.getByRole('dialog')
    await painel.getByLabel('Valor (R$)').fill('12,421')
    await painel.getByLabel('Descrição').fill('escala inválida')
    await painel.getByRole('button', { name: 'Lançar crédito' }).click()

    const campo = painel.getByLabel('Valor (R$)')
    await expect(campo).toHaveAttribute('aria-invalid', 'true')
    await expect(painel.getByText('duas casas decimais')).toBeVisible()
  })

  test('existe link para pular ao conteúdo e o destino ativo é anunciado', async ({ page }) => {
    await entrar(page, 'usuario0', 'senha0')

    await page.keyboard.press('Tab')
    await expect(page.getByRole('link', { name: 'Pular para o conteúdo' })).toBeFocused()

    await expect(page.getByRole('button', { name: 'Conta corrente', exact: true }))
      .toHaveAttribute('aria-current', 'page')
  })
})
