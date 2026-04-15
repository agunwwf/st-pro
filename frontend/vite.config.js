import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'
import tailwindcss from "@tailwindcss/vite";

export default defineConfig({
  plugins: [
      vue(),
      tailwindcss(),
  ],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    host: '0.0.0.0',
    hmr: false, // Disable HMR as per platform rules
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '/api') // Keep /api prefix if backend expects it
      },
      '/ws': {
        target: 'ws://localhost:8080',
        ws: true,
        changeOrigin: true
      },
      // 本地开发：iframe 为 /st/...，须与本机 Streamlit 的 --server.baseUrlPath=st 一致，否则静态资源会 404
      '/st': {
        target: 'http://localhost:8501',
        changeOrigin: true,
        ws: true,
      },
    }
  }
})
