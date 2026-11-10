import { defineConfig } from 'vite'

// Service worker de los avisos push del equipo (src/sw.ts): se compila aparte, después de
// la web, a un solo archivo dist/sw.js sin hash para que su dirección no cambie.
export default defineConfig({
  // React (que llega por los textos) lo pide; en un service worker no existe "process".
  define: { 'process.env.NODE_ENV': JSON.stringify('production') },
  publicDir: false,
  build: {
    outDir: 'dist',
    emptyOutDir: false,
    lib: { entry: 'src/sw.ts', formats: ['iife'], name: 'karsySw', fileName: () => 'sw.js' },
  },
})
