import { useState, type FormEvent } from 'react'
import { api, definirCredencial, limparCredencial, ErroApi, ErroDeRede } from '../api/cliente'

/**
 * Tela de entrada (§7 de docs/design/telas.md).
 *
 * Não existe rota de login na API: HTTP Basic vai em toda requisição. A validação da credencial é
 * feita chamando `GET /ativos`, que ambos os papéis podem acessar.
 */
export function Entrada({ aoEntrar }: { aoEntrar: (login: string) => void }) {
  const [login, definirLogin] = useState('')
  const [senha, definirSenha] = useState('')
  const [erro, definirErro] = useState<string | null>(null)
  const [enviando, definirEnviando] = useState(false)

  async function submeter(evento: FormEvent) {
    evento.preventDefault()
    definirEnviando(true)
    definirErro(null)
    definirCredencial(login, senha)

    try {
      await api.ativos({ notificarExpiracao: false })
      aoEntrar(login)
    } catch (falha) {
      limparCredencial()
      if (falha instanceof ErroDeRede) {
        definirErro(falha.message)
      } else if (falha instanceof ErroApi && falha.status === 401) {
        // A mesma mensagem para usuário inexistente e senha errada: distinguir os dois diria a um
        // atacante quais logins existem.
        definirErro('Usuário ou senha inválidos.')
      } else {
        definirErro('Não foi possível entrar. Tente de novo em instantes.')
      }
    } finally {
      definirEnviando(false)
    }
  }

  return (
    <div className="tela-entrada">
      <form onSubmit={submeter}>
        <h1>Minerva Finanças</h1>
        <p style={{ color: 'var(--color-text-secondary)', margin: 0 }}>
          Entre para acompanhar seu saldo e sua carteira.
        </p>

        <div className="campo">
          <label htmlFor="login">Usuário</label>
          <input
            id="login"
            autoComplete="username"
            value={login}
            onChange={(e) => definirLogin(e.target.value)}
            required
          />
        </div>

        <div className="campo">
          <label htmlFor="senha">Senha</label>
          <input
            id="senha"
            type="password"
            autoComplete="current-password"
            value={senha}
            onChange={(e) => definirSenha(e.target.value)}
            required
          />
        </div>

        {erro && (
          <span className="mensagem-erro" role="alert">
            {erro}
          </span>
        )}

        <button type="submit" className="acao primaria" disabled={enviando}>
          {enviando ? 'Entrando…' : 'Entrar'}
        </button>
      </form>
    </div>
  )
}
