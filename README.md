# 🏦 Banking App

[![CI](https://github.com/HauShen/Banking-app/actions/workflows/ci.yml/badge.svg?branch=master)](https://github.com/HauShen/Banking-app/actions/workflows/ci.yml)

A full-stack banking simulator: a **Java Spring Boot** API (hexagonal modules, PostgreSQL) and a **React +
TypeScript** web app. JWT authentication with refresh cookies, role-based access control, fund transfers (also paid
in a foreign currency at a locked rate), foreign remittances, and an admin area.

---

## 📸 Screenshots

> Taken from the React app with demo data.

**Dashboard** — the customer's accounts, total balance and recent transactions, including foreign-currency ones.

![Dashboard](screenshots/dashboard.png)

**Balance history** — the selected account's closing balance per day over the last 7, 30 or 90 days, from
`GET /api/transfers/account/{accountNumber}/balance-history` (see [Balance history](#-balance-history)).

![Balance history](screenshots/balance-history.png)

**Transfer paid in US dollars** — the review step locks the exchange rate for 60 seconds; confirming pays exactly
that rate.

![Transfer with a locked exchange rate](screenshots/transfer-locked-rate.png)

**Transaction history** — server-side paging; the details panel shows the original currency, amount and rate.

![Transaction details](screenshots/transaction-details.png)

**Exchange rates** — the bank's sell and buy rates and a quote calculator.

![Exchange rates](screenshots/exchange-rates.png)

**Admin: users** — paging and sorting on the server; edit details, change the role, delete, or open the user's
accounts.

![Admin users](screenshots/admin-users.png)

**Admin: accounts** — every account with its owner and status; lock, unlock, delete, or record a remittance from
abroad.

![Admin accounts](screenshots/admin-accounts.png)

---

## 🏗️ Architecture

```mermaid
flowchart LR
    browser["Browser<br/>React app"]
    subgraph frontend["frontend container (nginx)"]
        spa["/ → React build<br/>(index.html fallback)"]
        proxy["/api → proxy"]
    end
    subgraph backend["app container (Spring Boot)"]
        sec["Spring Security<br/>JWT filter"]
        modules["Modules: user · account ·<br/>transaction · fx<br/>(adapter → application → domain)"]
    end
    db[("PostgreSQL<br/>Flyway migrations")]

    browser -- "http://localhost:3000" --> spa
    browser -- "/api/** (Bearer token,<br/>refresh cookie)" --> proxy
    proxy --> sec --> modules --> db
```

- **One origin.** With Docker Compose the browser only talks to nginx on port 3000: it serves the React build and
  forwards `/api` to the backend, so no CORS is needed. In development the Vite dev server (port 5173) does the
  same forwarding to `localhost:8080`.
- **Auth.** The access token (15 minutes) lives only in the React app's memory. The refresh token is an
  `HttpOnly`, `SameSite=Strict` cookie on `/api/auth`: it restores the session after a reload and is rotated on
  every use (see [Security](#-security)).
- **Backend.** Each module (`userProfile`, `account`, `transaction`, `fx`) has its own domain, application
  (use cases and ports) and adapters; modules call each other only through inbound ports, checked by ArchUnit
  rules (`ArchitectureTest`).

---

## 🚀 Tech Stack

### Backend
| Technology | Purpose |
|---|---|
| Java 21 + Spring Boot 3.4.5 | REST API framework |
| Spring Security + JWT | Authentication & authorization |
| Spring Data JPA + Hibernate | ORM & database access |
| PostgreSQL | Relational database |
| Flyway | Database schema migrations |
| Apache Kafka + Spring Kafka | Events after a money movement: transactional outbox → relay → topic → notification consumer (dead-letter topic for bad messages) |
| Lombok | Boilerplate reduction |
| springdoc-openapi (Swagger UI) | Interactive API documentation |
| JUnit 5 + Spring Boot Test + Testcontainers | HTTP-level integration tests against a real PostgreSQL (and a real Kafka where needed) |

### Frontend
| Technology | Purpose |
|---|---|
| React 18 + TypeScript + Vite | UI framework & build tool |
| MUI 5 + MUI X Data Grid | Components and the paged, sortable tables |
| TanStack Query 5 | Server state: caching, loading and error states, refetching after a change |
| React Router 6 | Client-side routing, with route guards for signed-in users and for admins |
| `fetch` client (`src/api/client.ts`) | Bearer token, one shared refresh-and-retry on 401 |
| Vitest + React Testing Library + MSW | Component and API-client tests against a mocked backend |
| [BankDash](https://github.com/themewagon/bankdash) (ThemeWagon, MIT) | UI template the app is based on |

### DevOps
| Technology | Purpose |
|---|---|
| Docker | Multi-stage images: the backend (JDK 21 to build, JRE 21 to run) and the frontend (Node 22 to build, nginx to serve); both run as non-root users |
| Docker Compose | PostgreSQL and Kafka; with `--profile app` also the backend and the frontend; `--profile tools` adds a Kafka web UI |
| GitHub Actions | CI: backend build and tests, a Docker smoke test of the whole stack, and the frontend's lint, tests and build on each push and pull request; CD: publishes the backend image after each merge to `master` |
| GitHub Container Registry | Home of the published backend image: `ghcr.io/haushen/banking-app` (`latest`, one `sha-…` tag per commit, and a version tag per release) |

---

## ✨ Features

### 👤 Customer
- Register & login with JWT authentication
- View dashboard with account summary
- Top up own accounts (cash deposit)
- Transfer funds **from own accounts only**, with **idempotency protection** (no double transfers)
- Pay a transfer in **USD, SGD, EUR or JPY**: the MYR equivalent at the bank's sell rate moves
  (see [Foreign currency](#-foreign-currency))
- See the bank's exchange rates and a quote before paying, or **lock a rate for 60 seconds** and pay at exactly
  that rate
- View transaction history of own accounts, including the original currency, amount and rate
- Daily balance history of own accounts, shown as a chart on the dashboard (7, 30 or 90 days)
- **Notifications** for money sent, received, deposited or arriving from abroad: a bell in the app bar with the
  unread count, the latest notifications in a dropdown, mark one or all as read. Created from Kafka events a second
  or two after the money moves; when a new one arrives, the balances refresh on their own
- Update profile

### 🛡️ Admin
- Open bank accounts (Savings / Current) for customers
- Manage all users — view, update profile, change role, delete
- Manage all accounts — view, lock / unlock, delete (blocked when the account has transaction history)
- Record **incoming foreign remittances** (simulates the bank's payment gateway): credited in MYR at the buy rate
- Safety rails: an admin cannot change their own role or delete themselves, and the last admin cannot be removed
- Privileged actions (role changes, deletions, edits to other users, bootstrap) are written to an audit log

---

## ⚙️ Getting Started

### Prerequisites
- Java 21+
- Node.js 22 (18+ works; CI uses 22)
- Docker Desktop

### 1. Clone the repository
```bash
git clone https://github.com/HauShen/Banking-app.git
cd Banking-app
```

### 2. Start PostgreSQL and Kafka with Docker
```bash
docker compose up -d
```
Kafka listens on `localhost:9092`. To look at topics and messages, also start the web UI
(`docker compose --profile tools up -d`, then `http://localhost:8081`). To run the backend without Kafka,
set `KAFKA_ENABLED=false`: no topics are created and nothing is sent.

### 3. Configure environment
Secrets are never committed. Create a `.env` file in the project root (it is git-ignored and loaded at startup):
```properties
POSTGRES_PASSWORD=abc123
JWT_SECRET=<output of: openssl rand -base64 48>
```
`abc123` is the default password in `docker-compose.yaml`. The JWT secret must be Base64 on a single line.

The `dev` profile creates a demo admin on first start, when no admin exists yet:
**`devadmin` / `DevAdmin123!`** (dev only — intentionally public). Outside dev, set
`BOOTSTRAP_ADMIN_ENABLED=true` plus `BOOTSTRAP_ADMIN_USERNAME`, `BOOTSTRAP_ADMIN_FULLNAME`,
`BOOTSTRAP_ADMIN_EMAIL` and `BOOTSTRAP_ADMIN_PASSWORD` for the first deployment.

### 4. Run the backend
```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```
Backend runs on `http://localhost:8080`. Swagger UI (dev profile only): `http://localhost:8080/swagger-ui/index.html`
(log in with `POST /api/auth/login`, then paste the `accessToken` into **Authorize**).
If the frontend runs somewhere other than `http://localhost:5173`, set `CORS_ALLOWED_ORIGINS` (comma-separated).

**Or run the backend in Docker too** (no local JDK needed):
```bash
docker compose --profile app up --build
```
This builds both images and starts them next to PostgreSQL: the backend with the `dev` profile on the same
`http://localhost:8080`, and the frontend on **http://localhost:3000** (nginx serves the React app and forwards
`/api` to the backend). It reads `POSTGRES_PASSWORD` and `JWT_SECRET` from the same `.env`. Stop a running
`bootRun` first, because both use port 8080. A plain `docker compose up -d` still starts PostgreSQL and Kafka only.

Open `http://localhost:3000`, not `http://127.0.0.1:3000`: the refresh cookie is `Secure`, and browsers accept
Secure cookies over plain http only on `localhost`.

### 5. Run the frontend
```bash
cd frontend
npm install
npm run dev
```
Frontend runs on `http://localhost:5173` and forwards `/api` to the backend on `http://localhost:8080`; sign in
with `devadmin` / `DevAdmin123!` or register a customer. No configuration is needed. To point the dev server at
another backend, set `VITE_API_PROXY_TARGET` in `frontend/.env.local` (see `frontend/README.md`).

---

## 🔐 API Endpoints

All endpoints except `/api/auth/**` and `GET /actuator/health` require:
```
Authorization: Bearer <your-jwt-token>
```
Try them interactively in Swagger UI (dev profile): `http://localhost:8080/swagger-ui/index.html`.

**Access:** *Public* = no token · *Logged in* = any valid token · *Owner* = the logged-in user owns the account ·
*ADMIN* = admin role. Rules marked *Owner* are enforced in the use cases, not just by role.

#### Authentication
| Method | Endpoint | Access | Description |
|---|---|---|---|
| POST | `/api/auth/register` | Public | Register as a customer: access token in the body, refresh token in a cookie |
| POST | `/api/auth/login` | Public | Log in: access token in the body, refresh token in a cookie |
| POST | `/api/auth/refresh` | Refresh cookie | New access token + new refresh cookie (the old one is used up) |
| POST | `/api/auth/logout` | Logged in | Log out on every device: all access and refresh tokens stop working, cookie cleared (204) |
| GET | `/api/auth/bootstrap-status` | Public | Whether bootstrap is enabled and an admin exists |

#### Health
| Method | Endpoint | Access | Description |
|---|---|---|---|
| GET | `/actuator/health` | Public | `{"status":"UP"}` when the app and its database connection work, otherwise 503 with `DOWN`. No details, and nothing else under `/actuator` is exposed |

#### Users
| Method | Endpoint | Access | Description |
|---|---|---|---|
| GET | `/api/users/me` | Logged in | Own profile |
| PUT | `/api/users/me` | Logged in | Update own name / email |
| POST | `/api/users/create` | Logged in | Create a customer profile (no token returned) |
| GET | `/api/admin/users` | ADMIN | List users (paged, sortable) |
| GET | `/api/admin/users/{id}` | ADMIN | User by id |
| GET | `/api/admin/users/by-username/{username}` | ADMIN | User by username |
| POST | `/api/admin/users` | ADMIN | Create another admin |
| PUT | `/api/admin/users/{id}` | ADMIN | Update a user's profile (audited) |
| PATCH | `/api/admin/users/{id}/role` | ADMIN | Change role (audited; not own role, not the last admin) |
| POST | `/api/admin/users/{id}/revoke-tokens` | ADMIN | Log a user out of every device, e.g. a compromised account (204) |
| DELETE | `/api/admin/users/{id}` | ADMIN | Delete a user (audited; not self, not the last admin) |

#### Accounts
`{id}` is the account's database id (`accountId`), not the account number.

| Method | Endpoint | Access | Description |
|---|---|---|---|
| GET | `/api/accounts/summary` | Logged in | Own accounts and total balance |
| POST | `/api/accounts/create` | ADMIN | Open an account for a customer (RM20 opening balance) |
| PATCH | `/api/accounts/{id}/topup?amount=` | Owner | Cash deposit |
| GET | `/api/accounts/{id}` | Owner or ADMIN | Account by id |
| GET | `/api/accounts/number/{accountNumber}` | Owner or ADMIN | Account by account number |
| GET | `/api/accounts/get_all/{user_id}` | Owner or ADMIN | All accounts of a user |
| GET | `/api/accounts/get_all?status=` | ADMIN | All accounts, optional `ACTIVE` / `LOCKED` filter |
| PATCH | `/api/accounts/{id}/status?status=` | ADMIN | Lock or unlock an account |
| DELETE | `/api/accounts/{id}` | ADMIN | Delete an account (409 if it has transaction history) |

#### Transfers
| Method | Endpoint | Access | Description |
|---|---|---|---|
| POST | `/api/transfers` | Owner of the from-account | Transfer funds (201); `idempotencyKey` makes retries safe (409 if reused for a different transfer); optional `currency` of the amount (default MYR) and `quoteId` (pay at a locked rate) |
| GET | `/api/transfers/{reference}` | Sender, recipient or ADMIN | One transaction |
| GET | `/api/transfers/account/{accountNumber}` | Owner or ADMIN | Transaction history of an account (paged) |
| GET | `/api/transfers/account/{accountNumber}/balance-history?days=30` | Owner or ADMIN | Closing balance of each of the last `days` days (1–366, default 30), oldest first; see [Balance history](#-balance-history) |
| POST | `/api/admin/remittances` | ADMIN | Money from abroad arrives for an account (201): converted at the buy rate, idempotent like transfers |

#### Foreign exchange
| Method | Endpoint | Access | Description |
|---|---|---|---|
| GET | `/api/fx/rates` | Logged in | The bank's buy and sell rate for every offered currency (MYR per 1 unit) |
| GET | `/api/fx/quote?currency=USD&amount=100.00&direction=PAY` | Logged in | What an amount is worth in MYR now; `direction` `PAY` (sell rate, default) or `RECEIVE` (buy rate) |
| POST | `/api/fx/quotes` | Logged in | Lock the sell rate for `currency` + `amount` (201): `quoteId`, rate, MYR amount, `expiresAt` (60 s) |

#### Notifications
Created from the transaction events on Kafka, usually within a second or two of the money moving. Everyone sees only
their own (admins too).

| Method | Endpoint | Access | Description |
|---|---|---|---|
| GET | `/api/notifications?unreadOnly=false&page=0&size=20` | Logged in | Own notifications, newest first (paged; `size` 1–100) |
| GET | `/api/notifications/unread-count` | Logged in | `{"unread": n}` for the header bell |
| PATCH | `/api/notifications/{id}/read` | Owner | Mark one as read (204); someone else's or unknown → 404 |
| POST | `/api/notifications/read-all` | Logged in | Mark all own as read; `{"marked": n}` |

---

## 💱 Foreign currency

Modelled on everyday Malaysian savings accounts: **accounts stay in MYR**, and foreign money is converted when it
comes in or goes out. The transaction remembers the original currency, amount and rate.

- **Pay in a foreign currency:** `POST /api/transfers` with `"amount": 100.00, "currency": "USD"` moves the MYR
  equivalent between two MYR accounts. Without `currency` (or with `MYR`) a transfer works exactly as before.
- **Receive money from abroad:** `POST /api/admin/remittances` credits a MYR account. Admin only: customers can't
  create incoming money; in a real bank this would be the payment gateway.
- **Which rate:** as banks quote it, from the bank's side. The customer pays in USD → the bank sells USD → **sell
  rate**. Money arrives in USD → the bank buys it → **buy rate** (lower). `GET /api/fx/quote` shows the same numbers
  before confirming.
- **Locking a rate:** a quote alone is only a preview: the transfer uses the rate at the moment it runs. To pay at
  a fixed rate, `POST /api/fx/quotes` with the currency and amount returns a `quoteId` valid for 60 seconds
  (`fx.quote-validity`); the transfer then sends the same `currency` and `amount` plus `quoteId`:
  ```json
  { "fromAccountNumber": "…", "toAccountNumber": "…", "amount": 100.00, "currency": "USD",
    "quoteId": "3f9a0c12-b7d4-4e5f-8a6b-1c2d3e4f5a6b", "description": "Rent", "idempotencyKey": "…" }
  ```
  A quote belongs to the user who locked it and works once. It is used up inside the transfer's database
  transaction (its row locked with `SELECT … FOR UPDATE`), so a transfer that fails — e.g. on the minimum balance —
  leaves it usable, and two transfers racing for one quote get it only once. Refused with 400: "Quote not found"
  (also for someone else's quote, so ids reveal nothing), "The quote has already been used", "The quote has
  expired", "The quote is for a different currency or amount", and a quote with a MYR transfer.
- **Rules:** MYR amount = foreign amount × rate, rounded half-up to 2 decimals, at least RM 0.01. The foreign amount
  can't have more decimals than its currency (JPY none, others 2). Rates are stored with 8 decimals (JPY ≈ 0.03).
- **Retries:** a retry with the same `idempotencyKey` is matched on the **original** currency and amount, so it
  returns the original transaction even if the rate has changed since — or its quote has been used — and nothing
  is converted or moved twice.
- **History:** every transaction now has a `type` (`TRANSFER`, `CASH_DEPOSIT`, `FOREIGN_REMITTANCE`) and, for foreign
  ones, `originalCurrency`, `originalAmount` and `exchangeRate`. Remittances show "Foreign Remittance" as the sender.
- **Rates** come from configuration (`fx.rates.USD.buy`, `fx.rates.USD.sell`, ...); the currencies listed there are
  the ones offered. A wrong rate (zero, buy above sell, too many decimals) stops the application at startup.
- **Design:** exchange rates live in their own `fx` module. It owns the conversion rule; the transaction module asks
  it through its own port (`CurrencyConversionPort`), so a transfer books exactly what the quote shows. Locked
  quotes are fx's own table (`fx_quotes`). A live rate feed would replace one adapter
  (`ConfiguredExchangeRateSource`) without touching any business code.

---

## 📈 Balance history

`GET /api/transfers/account/{accountNumber}/balance-history?days=30` returns the balance at the end of each day:
```json
{ "accountNumber": "1234567890", "currency": "MYR", "timeZone": "Asia/Kuala_Lumpur",
  "days": [ { "date": "2026-11-19", "closingBalance": 120.00 }, { "date": "2026-11-20", "closingBalance": 90.00 } ] }
```
- **Computed from the ledger, nothing extra is stored.** Every balance change (transfer, cash deposit, remittance)
  writes a ledger entry with the balance right after it. A day closes at the last entry posted that day; a day
  without entries keeps the previous day's balance. Two indexed queries per request: the last entry before the
  period, and the entries in it (index on `ledgers (account_id, created_at, id)`, V11).
- **Days are Malaysian days.** "Today" and midnight come from `bank.time-zone` (default `Asia/Kuala_Lumpur`), not
  UTC: a transfer at 00:30 in Kuala Lumpur counts for that day, although it is still the day before in UTC.
- The list ends today and starts `days − 1` days earlier, or on the day the account was opened if that is later.
- Same access rule as the transaction history: the owner or an admin.

---

## 🔒 Security

### Authentication: stateless JWT
1. `POST /api/auth/login` checks the password against its **BCrypt** hash and returns a JWT signed with
   HS256 (`sub` = username, **15-minute** expiry by default). The signing key comes from `JWT_SECRET`, never from Git.
2. Every request sends `Authorization: Bearer <token>`. `JwtRequestFilter` verifies the signature and expiry,
   then **reloads the user from the database**, so a role change or deletion takes effect on the next request,
   not when the token expires.
3. No server-side session (`SessionCreationPolicy.STATELESS`). API calls authenticate with the `Authorization`
   header, which a browser never adds by itself, so CSRF protection is disabled. The one cookie (below) is
   protected by `SameSite=Strict` instead.
4. **Revocation without a blacklist:** every user has a token version (table `user_token_versions`) and every
   token carries the version it was issued with (claim `ver`). `POST /api/auth/logout` — or an admin via
   `/revoke-tokens` — increases the version, so all older tokens of that user are rejected immediately.
5. **Refresh tokens:** login also sets a `refresh_token` cookie — `HttpOnly` (JavaScript can't read it),
   `Secure`, `SameSite=Strict` (never sent from another site), `Path=/api/auth`, valid 7 days. When the access token
   expires, `POST /api/auth/refresh` exchanges the cookie for a new access token and a **new** refresh token.
   - Only a SHA-256 hash of each refresh token is stored (table `refresh_tokens`), so a database leak leaks no usable tokens.
   - Each refresh token works **once** (rotation). Presenting a used one again means someone has a copy: the whole
     token family is revoked and the user's token version increases, so the thief's access token dies too.

A missing, garbage, tampered, expired, revoked or deleted-user token is ignored by the filter (the request continues
as anonymous) — it never crashes the request, and a stale token never blocks `/api/auth/login`.

### Authorization: three layers
| Layer | Where | Example |
|---|---|---|
| URL rules | `SecurityConfig` | `/api/auth/**` public, `/api/admin/**` admin only, everything else needs a token |
| Role checks | `@PreAuthorize("hasRole('ADMIN')")` | Opening, locking and deleting accounts |
| Ownership checks | Use cases / services | Only the owner can transfer from or top up an account; history is owner-or-admin |

Ownership rules live in the application layer: the web adapter turns the logged-in user into an
`AccountRequester(userId, admin)`, so the use cases enforce "owner or admin" without depending on Spring Security.
**Admins cannot move a customer's money** — transfers and top-ups are owner-only for everyone.

### Status codes
| Code | Meaning |
|---|---|
| 401 | Not logged in: no token, or a token that is invalid, expired, revoked (after logout), or belongs to a deleted user |
| 403 | Logged in, but not allowed (wrong role, or not your account) |
| 401 + `Invalid credentials` | Failed login — the same message for a wrong password and an unknown user, so usernames can't be probed |
| 429 + `Retry-After` | Too many failed logins for this username or from this IP — try again after the given number of seconds |

### Other protections
- **Login brute-force protection:** 5 failed logins for one username within 15 minutes lock that username for
  15 minutes (unknown usernames too, so a lockout reveals nothing); 20 failures from one IP block that IP.
  While locked, even the correct password gets 429. Limits are configurable (`security.login.*`).
- Registration has no `role` field: nobody can sign themselves up as admin.
- Every error, including 401/403 from the security layer, uses the same JSON body
  (`timestamp`, `status`, `error`, `message`, `path`); unexpected errors are logged, never shown to the client.
- A missing query parameter or one of the wrong type (`?page=abc`) is answered with 400 and the parameter's name,
  never with a 500.
- CORS only allows the configured frontend origin (`CORS_ALLOWED_ORIGINS`, default `http://localhost:5173`)
  and only the `Authorization` and `Content-Type` headers.
- Swagger UI and the API docs are only enabled in the `dev` profile.
- Transaction responses never contain another user's internal id.
- An idempotency key belongs to the account that first used it: reusing another account's key returns 400
  instead of that account's transaction — also when both requests arrive at the same moment.
- Retrying with the same key returns the original transfer (same reference and time) and debits only once, even
  when the retries arrive at the same moment. Reusing a key for a different amount or recipient returns 409.
  For a transfer paid in a foreign currency, "the same amount" means the same original currency and amount.
- Remittances follow the same rules: the same request again returns the original and credits once; a key used for
  anything else returns 409.
- Amounts can have at most 2 decimals (400 otherwise), matching the database columns.
- Transfers, top-ups and remittances lock the account rows (`SELECT … FOR UPDATE`, always in the same order), so
  concurrent requests can't overwrite each other's balance or deadlock. A locked quote's row is locked last, after
  the accounts.

### Known limitations
- Login throttling is in memory: it resets on restart and isn't shared between several instances
  (production: Redis, or rate limiting at the API gateway).
- A username lockout can be abused to lock someone out for 15 minutes by failing on purpose — the usual trade-off
  of account lockout; real systems add CAPTCHA or progressive delays.
- Behind a reverse proxy, enable `server.forward-headers-strategy=native`, otherwise every client shares the
  proxy's IP for the per-IP limit. Docker Compose sets it (`SERVER_FORWARD_HEADERS_STRATEGY`) for the nginx
  frontend.
- Logout is "log out everywhere": there is no per-device logout.
- Two browser tabs refreshing at the same moment with the same cookie look like token reuse and log the user out.
  Within one tab the frontend runs one refresh at a time; coordinating tabs (e.g. a `BroadcastChannel`) is not
  implemented.
- Used and expired refresh tokens stay in the table; a scheduled cleanup job is not implemented yet.
- Exchange rates are fixed in configuration (example values, not live rates). A live feed (e.g. a central bank's
  API, with caching and a fallback) would be a new adapter behind `ExchangeRateSourcePort`.
- Locked quotes are only for paying transfers; remittances always use the current buy rate. Used and expired
  quotes stay in `fx_quotes`; a scheduled cleanup job is not implemented yet (like refresh tokens).
- The bank's margin between buy and sell rate is not booked anywhere: a transfer debits and credits the same MYR
  amount, and there is no FX revenue account.
- Remittances are simulated: an admin call stands in for the payment gateway, and the sender is just a name.
- Only currencies with at most 2 decimal places can be offered (e.g. not KWD, which has 3).
- **By design:** on account-number lookups, 404 (unknown) vs 403 (not yours) shows whether an account number
  exists. Hiding it would not help: a transfer has to confirm that the recipient's account exists anyway, as
  real banks do. The defence against probing account numbers is rate limiting, not hiding the status code.

---

## 🧪 Running Tests

Requires **Docker** running: the tests start a real PostgreSQL 17 container with Testcontainers.
```bash
./gradlew test
```

Frontend (171 tests; the backend is mocked with MSW, so no backend or Docker is needed):
```bash
cd frontend
npm test
```

GitHub Actions runs the full build (`./gradlew build`, which includes every test below) on each push to
`master` and on each pull request: see `.github/workflows/ci.yml`. The HTML test report is attached to every run.
A second job builds both Docker images, starts the whole stack through Docker Compose, waits for
`/actuator/health` to answer UP, and checks that nginx serves the app and forwards `/api`.
A third job lints, tests and builds the frontend.
After a merge to `master`, and only if the build and Docker jobs passed, a fourth job publishes the backend image
to GitHub Container Registry.
Pushing a version tag such as `v0.1.0` runs the same checks, publishes the image as `0.1.0` and creates a GitHub Release.

The security tests are black-box HTTP tests: a real embedded server on a random port, real JWTs, a real database
with every Flyway migration applied. Because they only talk HTTP, they stay valid while the code behind the
endpoints is refactored.

| Test Class | What It Covers |
|---|---|
| `AuthenticationSecurityTest` | Missing, garbage, tampered, forged, expired and deleted-user tokens → 401; a stale token doesn't block login; failed logins look identical; the bootstrap admin exists |
| `AuthorizationSecurityTest` | Customer on admin URLs and admin-only methods → 403; `role` in registration is ignored; viewing or topping up someone else's account → 403 |
| `LoginThrottlingSecurityTest` | 5 failures lock a username → 429 with `Retry-After`, even with the right password; unknown usernames lock the same way; other users unaffected; a success resets the count; the lock expires |
| `LoginIpThrottlingSecurityTest` | Failures spread over many usernames from one IP block that IP, then the block expires (own Spring context with a low limit) |
| `RefreshTokenSecurityTest` | Login/register set a hardened cookie (HttpOnly, Secure, SameSite=Strict, Path); refresh gives a working access token and rotates the cookie; reusing a used token revokes the family and the current access token; missing/garbage cookie → 401; logout and admin revoke-tokens kill refresh tokens; deleted user → 401 |
| `TokenRevocationSecurityTest` | Logout → the same token gets 401, on every device, while other users and a fresh login still work; admin revoke-tokens; customers can't revoke others (403); unknown user (404) |
| `CorsSecurityTest` | Preflight from the configured frontend → allowed; from another site, or asking for an unlisted header → 403 |
| `HealthEndpointSecurityTest` | `/actuator/health` is public and says only UP, without details; health sub-paths and the rest of Actuator → 401 |
| `TransferSecurityTest` | Only the owner can transfer (admins too get 403) and balances are checked; idempotent retries debit once; reusing another account's key → 400; history visible only to owner / recipient / admin |

Feature tests in the same style cover the rest, for example:

| Test Class | What It Covers |
|---|---|
| `ForeignCurrencyTransferTest` | Paying in USD / JPY moves the right MYR amount at the sell rate; history shows the original amount and rate; MYR unchanged; bad currency or decimals → 400; a retry after a rate change returns the original; mismatches → 409 |
| `BalanceHistoryEndpointTest` | One closing balance per day, today at the current balance; owner or admin only (403 / 401 / 404); bad `days` → 400. Multi-day sums: `GetBalanceHistoryServiceTest`, `LedgerPersistenceAdapterTest` |
| `RemittanceEndpointTest` | Admin-only remittances credited at the buy rate; "Foreign Remittance" in the history; locked / unknown account; idempotency; delete after a remittance → 409 |
| `FxEndpointTest` | Rates list and quotes (PAY / RECEIVE), unsupported currency, bad parameters → 400 (never 500), login required; locking a quote (201, stored for the user, valid 60 s) |
| `LockedQuoteTransferTest` | Paying at a locked rate even when today's differs; the quote works once; a failed transfer leaves it usable; expired, someone else's, unknown, other amount / currency, or with MYR → 400; a retry returns the original |
| `ArchitectureTest` | 22 ArchUnit rules: each module's domain is plain Java, adapters → application → domain, and modules reach each other only through inbound ports (account ↔ transaction, transaction → fx) |
