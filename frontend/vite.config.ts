import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // repassa as chamadas /api ao backend Spring; assim o navegador ve uma unica origem e nao ha CORS
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
