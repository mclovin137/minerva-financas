import { expect, test } from '@playwright/test'
import { entrar } from './apoio'

test.describe('Autenticação e papéis', () => {
  test('credencial inválida não entra e não distingue usuário inexistente de senha errada', async ({ page }) => {
    await page.goto('/')

    await page.getByLabel('Usuário').fill('usuario0')
    await page.getByLabel('Senha').fill('senha-errada')
    await page.getByRole('button', { name: 'Entrar' }).click()
    const mensagemComSenhaErrada = await page.getByRole('alert').innerText()

    await page.getByLabel('Usuário').fill('nao-existe')
    await page.getByLabel('Senha').fill('qualquer')
    await page.getByRole('button', { name: 'Entrar' }).click()
    const mensagemComUsuarioInexistente = await page.getByRole('alert').innerText()

    expect(mensagemComSenhaErrada).toBe(mensagemComUsuarioInexistente)
    expect(mensagemComSenhaErrada).toContain('inválidos')
    await expect(page.getByRole('button', { name: 'Sair' })).toBeHidden()
  })

  test('usuário comum entra e alcança os quatro destinos', async ({ page }) => {
    await entrar(page, 'usuario0', 'senha0')

    for (const destino of ['Conta corrente', 'Carteira', 'Ativos', 'Mercado histórico']) {
      const item = page.getByRole('button', { name: destino, exact: true })
      await expect(item).toBeEnabled()
      await item.click()
      await expect(page.getByRole('button', { name: destino, exact: true }))
        .toHaveAttribute('aria-current', 'page')
    }
  })

  test('root administra o acervo e não alcança dados financeiros', async ({ page }) => {
    await entrar(page, 'root', 'spiderman')

    await expect(page.getByText('administrador')).toBeVisible()
    // Os destinos proibidos continuam visíveis, porém desabilitados: esconder rotas faria os dois
    // papéis verem aplicações diferentes.
    await expect(page.getByRole('button', { name: 'Conta corrente', exact: true })).toBeDisabled()
    await expect(page.getByRole('button', { name: 'Carteira', exact: true })).toBeDisabled()
    await expect(page.getByRole('button', { name: 'Ativos', exact: true })).toBeEnabled()
    await expect(page.getByRole('button', { name: 'Novo ativo' })).toBeVisible()
  })

  test('usuário comum não vê as ações administrativas de ativos', async ({ page }) => {
    await entrar(page, 'usuario0', 'senha0')
    await page.getByRole('button', { name: 'Ativos', exact: true }).click()

    await expect(page.getByRole('heading', { name: 'Ativos' })).toBeVisible()
    await expect(page.getByRole('button', { name: 'Novo ativo' })).toBeHidden()
  })

  test('sair encerra a sessão e devolve à tela de entrada', async ({ page }) => {
    await entrar(page, 'usuario0', 'senha0')

    await page.getByRole('button', { name: 'Sair' }).click()

    await expect(page.getByRole('button', { name: 'Entrar' })).toBeVisible()
  })
})
