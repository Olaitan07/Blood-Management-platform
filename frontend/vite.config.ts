import { fileURLToPath, URL } from 'node:url'
// vitest/config re-exports Vite's defineConfig merged with Vitest's own
// config types, so the `test` block below type-checks without a separate
// vitest.config.ts or a triple-slash type reference.
import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'

// Dev server proxies /api to the backend so the browser never makes a
// cross-origin request — the backend has no CORS configuration today.
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  test: {
    environment: 'jsdom',
    // No globals: true — test files import describe/it/expect explicitly
    // from 'vitest' rather than relying on ambient globals, so tsconfig.json
    // (shared with the whole app, not just tests) doesn't need a
    // vitest-specific `types` entry.
    setupFiles: ['./src/test/setup.ts'],
    css: true,
  },
})
