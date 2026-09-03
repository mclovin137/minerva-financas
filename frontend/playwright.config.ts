import { defineConfig, devices } from '@playwright/test'

/**
 * Testes de ponta a ponta contra a aplicação real: o jar do backend servindo a API e a interface
 * compilada no mesmo host. É a única camada de teste que exercita o que o avaliador vai ver.
 *
 * O banco aponta para um arquivo temporário exclusivo da execução. Sem isso, os testes rodariam
 * sobre o banco de desenvolvimento e passariam ou falhariam conforme o que estivesse gravado nele.
 */
const BANCO_DA_EXECUCAO = `${process.env.RUNNER_TEMP ?? '/tmp'}/minerva-e2e-${Date.now()}.db`

export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  // Um worker só: os casos compartilham o mesmo backend e alguns dependem de estado financeiro que
  // eles próprios criam. Paralelizar aqui trocaria cobertura real por corridas difíceis de ler.
  workers: 1,
  forbidOnly: Boolean(process.env.CI),
  retries: process.env.CI ? 1 : 0,
  reporter: process.env.CI
    ? [['html', { outputFolder: 'playwright-report', open: 'never' }], ['list']]
    : [['list']],

  use: {
    baseURL: 'http://localhost:8080',
    locale: 'pt-BR',
    timezoneId: 'America/Sao_Paulo',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },

  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],

  webServer: {
    command: `java -jar ../backend/target/minerva-financas-backend-0.0.1-SNAPSHOT.jar`,
    url: 'http://localhost:8080/',
    reuseExistingServer: !process.env.CI,
    timeout: 120_000,
    stdout: 'pipe',
    stderr: 'pipe',
    env: {
      SPRING_DATASOURCE_URL: `jdbc:sqlite:${BANCO_DA_EXECUCAO}`,
    },
  },
})
