import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],

  server: {
    // Proxy em desenvolvimento: /api/* → Spring Boot local
    // Isso garante que BASE_URL = "/api" funcione tanto em dev quanto em prod (nginx).
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
