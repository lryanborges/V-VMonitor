import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // repassa as chamadas /api ao backend Spring; assim o navegador ve uma unica origem e nao ha CORS.
    // API_TARGET permite apontar para outra instancia (ex.: API_TARGET=http://localhost:8081 npm run dev)
    proxy: {
      '/api': process.env.API_TARGET ?? 'http://localhost:8080',
    },
  },
})
