import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// O proxy evita CORS no desenvolvimento: o navegador fala só com o Vite, que repassa ao backend.
// Em produção a interface é servida pelo mesmo host da API, então não há proxy nem CORS.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/contacorrente': 'http://localhost:8080',
      '/movimentacao': 'http://localhost:8080',
      '/ativos': 'http://localhost:8080',
      '/posicao': 'http://localhost:8080',
    },
  },
  build: {
    outDir: 'dist',
  },
})
