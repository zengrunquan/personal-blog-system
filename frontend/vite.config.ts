import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

const productionBase = '/personal_blog_system_war_exploded/'
const contextPath = productionBase.slice(0, -1)

export default defineConfig(({ command }) => ({
  base: command === 'serve' ? '/' : productionBase,
  plugins: [vue()],
  resolve: {
    alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) },
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        cookiePathRewrite: '/',
        rewrite: (path) => `${contextPath}${path}`,
      },
      '/uploads': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        cookiePathRewrite: '/',
        rewrite: (path) => `${contextPath}${path}`,
      },
      [contextPath]: {
        target: 'http://localhost:8080',
        changeOrigin: true,
        cookiePathRewrite: '/',
      },
    },
  },
  build: {
    outDir: 'dist',
    emptyOutDir: true,
    sourcemap: true,
  },
}))
