import react from '@vitejs/plugin-react';
import { defineConfig, loadEnv } from 'vite';
import checker from 'vite-plugin-checker';
import tsconfigPaths from 'vite-tsconfig-paths';

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, '.', 'VITE_');

  return {
    optimizeDeps: {
      include: ['@emotion/styled'],
    },
    plugins: [
      tsconfigPaths(),
      react(),
      checker({
        typescript: true,
        eslint: {
          lintCommand: 'eslint "./src/**/*.{ts,tsx}"',
        },
      }),
    ],

    //   preview: {
    //     port: 5000,
    //   },
    server: {
      host: '0.0.0.0',
      port: 5173,
      // Forward /api to Spring Boot, so the browser sees one origin (no CORS in dev)
      // and the refresh cookie (Path=/api/auth) is stored for localhost:5173.
      // /api/notifications goes to notification-service; it must come first (the first matching entry wins).
      proxy: {
        '/api/notifications': {
          target: env.VITE_NOTIFICATIONS_PROXY_TARGET || 'http://localhost:8082',
        },
        '/api': {
          target: env.VITE_API_PROXY_TARGET || 'http://localhost:8080',
        },
      },
    },
  };
});
