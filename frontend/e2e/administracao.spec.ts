import { expect, test } from '@playwright/test'
import { DIA_UTIL, definirCursorDeData, entrar } from './apoio'

/** Fluxo do papel administrativo: CRUD do acervo compartilhado e preço de mercado por data. */
test.describe('Administração do acervo', () => {
  const codigo = `E2E${Date.now()}`

  test('root cria, edita e remove um ativo', async ({ page }) => {
    await entrar(page, 'root', 'spiderman')
    await page.getByRole('button', { name: 'Ativos', exact: true }).click()

    await page.getByRole('button', { name: 'Novo ativo' }).click()
    let painel = page.getByRole('dialog')
    await painel.getByLabel('Código').fill(codigo)
    await painel.getByLabel('Nome').fill('Ativo de ponta a ponta')
    await painel.getByLabel('Tipo').selectOption('FUNDO')
    await painel.getByLabel('Data de emissão').fill('2020-01-01')
    await painel.getByLabel('Data de vencimento').fill('2030-01-01')
    await painel.getByRole('button', { name: 'Salvar ativo' }).click()

    await expect(page.getByRole('status')).toContainText('Ativo cadastrado')
    const linha = page.getByRole('row', { name: new RegExp(codigo) })
    await expect(linha).toContainText('FUNDO')

    await linha.getByRole('button', { name: 'Editar' }).click()
    painel = page.getByRole('dialog')
    await painel.getByLabel('Nome').fill('Nome alterado pelo E2E')
    await painel.getByRole('button', { name: 'Salvar ativo' }).click()
    await expect(page.getByRole('status')).toContainText('Ativo atualizado')
    await expect(page.getByRole('row', { name: new RegExp(codigo) }))
      .toContainText('Nome alterado pelo E2E')

    await page.getByRole('row', { name: new RegExp(codigo) })
      .getByRole('button', { name: 'Remover' }).click()
    const confirmacao = page.getByRole('alertdialog')
    await expect(confirmacao).toContainText(codigo)
    await confirmacao.getByRole('button', { name: 'Remover' }).click()

    await expect(page.getByRole('status')).toContainText('Ativo removido')
    await expect(page.getByRole('row', { name: new RegExp(codigo) })).toBeHidden()
  })

  test('a confirmação destrutiva pode ser cancelada e não remove nada', async ({ page }) => {
    await entrar(page, 'root', 'spiderman')
    await page.getByRole('button', { name: 'Ativos', exact: true }).click()

    const alvo = page.getByRole('row', { name: /ATIVO3/ }).first()
    await alvo.getByRole('button', { name: 'Remover' }).click()

    const confirmacao = page.getByRole('alertdialog')
    // O foco inicial é Cancelar de propósito: um Enter acidental não pode remover nada.
    await expect(confirmacao.getByRole('button', { name: 'Cancelar' })).toBeFocused()
    await confirmacao.getByRole('button', { name: 'Cancelar' }).click()

    await expect(confirmacao).toBeHidden()
    await expect(page.getByRole('row', { name: /ATIVO3/ }).first()).toBeVisible()
  })

  test('root define preço de mercado em uma data e o usuário comum o enxerga', async ({ page }) => {
    await entrar(page, 'root', 'spiderman')
    await definirCursorDeData(page, DIA_UTIL)
    await page.getByRole('button', { name: 'Mercado histórico', exact: true }).click()

    const ativo = page.getByRole('combobox', { name: 'Ativo', exact: true })
    await expect(ativo).toBeVisible()
    await ativo.selectOption('ATIVO4')
    await page.getByRole('button', { name: 'Definir preço' }).click()
    const painel = page.getByRole('dialog')
    await painel.getByLabel('Data').fill(DIA_UTIL)
    await painel.getByLabel('Preço de mercado (R$)').fill('42,50')
    await painel.getByRole('button', { name: 'Salvar preço' }).click()

    await expect(page.getByRole('status')).toContainText('Preço de mercado salvo')

    await page.getByRole('button', { name: 'Sair', exact: true }).click()
    await entrar(page, 'usuario7', 'senha7')
    await page.getByRole('button', { name: 'Mercado histórico', exact: true }).click()

    const precoDoAtivo = page.getByRole('combobox', { name: 'Ativo', exact: true })
    await expect(precoDoAtivo).toBeVisible()
    await precoDoAtivo.selectOption('ATIVO4')
    await expect(page.locator('.indicador.destaque .valor')).toContainText('42,50')
  })

  test('usuário comum recebe negativa ao tentar administrar preço', async ({ page }) => {
    await entrar(page, 'usuario7', 'senha7')
    await page.getByRole('button', { name: 'Mercado histórico', exact: true }).click()

    // A ação administrativa nem é oferecida ao papel que não pode executá-la.
    await expect(page.getByRole('button', { name: 'Definir preço' })).toBeHidden()
  })
})
