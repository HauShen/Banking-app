import { setupServer } from 'msw/node';

/** Base URL the API client uses in tests (vitest.config.ts → VITE_API_BASE_URL). */
export const API = 'http://localhost';

/** Mock backend. Each test adds the endpoints it needs with server.use(...); unknown requests fail the test. */
export const server = setupServer();
