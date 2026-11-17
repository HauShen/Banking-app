# Banking App — frontend

React frontend for the [Banking App](../README.md) Spring Boot API.

**Stack:** React 18, TypeScript, Vite, MUI 5, React Router 6.

## Run locally

Requires Node.js 18+ and the backend running on `http://localhost:8080` (see the root README).

```bash
npm install
npm run dev      # http://localhost:5173
```

Other scripts: `npm run build`, `npm run lint`, `npm run format`, `npm run preview`.

## Tests

```bash
npm test            # run once (also in CI)
npm run test:watch  # re-run on every change
```

Vitest + React Testing Library in jsdom. The backend is mocked with [MSW](https://mswjs.io/)
(`src/test/server.ts`): each test declares the endpoints it needs, and any other request fails the test.
The CI job `frontend` (`.github/workflows/ci.yml`) runs lint, tests and build on every pull request.

## Configuration

Copy `.env.example` to `.env.local` to override:

| Variable | Default | Purpose |
|---|---|---|
| `VITE_API_BASE_URL` | *(empty)* | API origin. Empty = same origin (`/api`), via the Vite dev proxy or a reverse proxy in production. |
| `VITE_API_PROXY_TARGET` | `http://localhost:8080` | Where the Vite dev server forwards `/api`. |

## Authentication

- The access token (15 min) is kept **in memory only**, never in `localStorage`.
- The refresh token is an **httpOnly, SameSite=Strict cookie** scoped to `/api/auth`; it restores the session after a
  reload and is rotated on every refresh.
- `src/api/client.ts` adds the Bearer token, and on a 401 refreshes once and retries. Parallel requests share one
  refresh, because the backend rotates the refresh token on every use.

## Credits

UI based on [BankDash](https://github.com/themewagon/bankdash) by ThemeWagon (MIT License).
