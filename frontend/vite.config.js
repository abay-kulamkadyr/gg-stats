import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/teams': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },

      '/heroes': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },

      '/highlights': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },

      '/img': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
