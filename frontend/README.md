# Banking App — frontend

React frontend for the [Banking App](../README.md) Spring Boot API.

**Stack:** React 18, TypeScript, Vite, MUI 5, React Router 6.

## Run locally

Requires Node.js 18+ and the backend running on `http://localhost:8080`, plus notification-service on
`http://localhost:8082` for the notification bell (see the root README).

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

## Docker

`Dockerfile` builds the app (`npm ci`, `npm run build`) and serves `dist/` with nginx (non-root, port 8080).
nginx forwards `/api` to the backend (`API_UPSTREAM`, default `http://app:8080`) and `/api/notifications` to
notification-service (`NOTIFICATIONS_UPSTREAM`, default `http://notification-service:8082`), so the browser sees one
origin.
Unknown paths get `index.html` (React Router), hashed assets are cached for a year, `index.html` never.

From the repository root, together with PostgreSQL and the backend:

```bash
docker compose --profile app up --build
```

Then open **http://localhost:3000**. Use `localhost`, not `127.0.0.1`: the refresh cookie is `Secure`, and
browsers accept Secure cookies over plain http only on `localhost`.

## Configuration

Copy `.env.example` to `.env.local` to override:

| Variable | Default | Purpose |
|---|---|---|
| `VITE_API_BASE_URL` | *(empty)* | API origin. Empty = same origin (`/api`), via the Vite dev proxy or a reverse proxy in production. |
| `VITE_API_PROXY_TARGET` | `http://localhost:8080` | Where the Vite dev server forwards `/api`. |
| `VITE_NOTIFICATIONS_PROXY_TARGET` | `http://localhost:8082` | Where it forwards `/api/notifications` (notification-service). |

The app calls every endpoint under one `/api` prefix; the proxy decides which backend answers. `/api/notifications`
is served by notification-service, the rest by the main app. The frontend's nginx (`docker/nginx.conf.template`)
splits the paths the same way, and a reverse proxy in production must too. With `VITE_API_BASE_URL` set, every call
goes to that one origin, so the split has to happen there.

## Authentication

- The access token (15 min) is kept **in memory only**, never in `localStorage`.
- The refresh token is an **httpOnly, SameSite=Strict cookie** scoped to `/api/auth`; it restores the session after a
  reload and is rotated on every refresh.
- `src/api/client.ts` adds the Bearer token, and on a 401 refreshes once and retries. Parallel requests share one
  refresh, because the backend rotates the refresh token on every use.
