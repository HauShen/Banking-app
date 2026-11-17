/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** API origin, e.g. https://api.example.com. Empty: same origin (Vite dev proxy / reverse proxy). */
  readonly VITE_API_BASE_URL?: string;
  /** Where the Vite dev server forwards /api. Default http://localhost:8080. */
  readonly VITE_API_PROXY_TARGET?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
