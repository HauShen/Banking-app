import react from '@vitejs/plugin-react';
import tsconfigPaths from 'vite-tsconfig-paths';
import { defineConfig } from 'vitest/config';

// Separate from vite.config.ts so that vite-plugin-checker (type-check + lint overlay) doesn't run during tests.
export default defineConfig({
  plugins: [tsconfigPaths(), react()],
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    // Node's fetch needs absolute URLs; in the browser the app uses '' (same origin).
    env: { VITE_API_BASE_URL: 'http://localhost' },
    css: false,
  },
});
