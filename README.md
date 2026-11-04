# 🏦 Banking App

A full-stack banking simulator built with **Java Spring Boot** and **React**, featuring JWT
authentication, role-based access control, fund transfers, and an admin management panel.



---

## 🎬 Demo

### 🔐 First Admin (Bootstrap)
> The first admin is created automatically at startup from configuration when no admin exists.
> There is no public sign-up for admins — see [Getting Started](#3-configure-environment).

---

### 👤 Register as Customer
> New users can self-register as a customer.

![Register Customer](screenshots/register-customer.gif)

---

### ⚠️ Username & Email Validation
> System rejects duplicate usernames and emails with clear error messages.

![Duplicate Validation](screenshots/duplicate-validation.gif)

---

### 🏧 Admin — Create Account for Customer
> Admin can create bank accounts and assign them to customers.

![Admin Create Account](screenshots/admin-create-account.gif)

---

### 🛠️ Admin — Manage Customers & Accounts
> Admin panel for managing all users and accounts, including status updates.

![Admin Manage](screenshots/admin-manage.gif)

---

### 💸 Customer — Top Up, Transfer & Transactions
> Customers can top up balance, transfer funds, and view transaction history.

![Customer Transactions](screenshots/customer-transactions.gif)

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
| Lombok | Boilerplate reduction |
| springdoc-openapi (Swagger UI) | Interactive API documentation |
| JUnit 5 + Spring Boot Test + Testcontainers | HTTP-level integration tests against a real PostgreSQL |

### Frontend
| Technology | Purpose |
|---|---|
| React 18 + Vite | UI framework & build tool |
| TanStack React Query | Server state management |
| React Hook Form | Form handling |
| React Router v6 | Client-side routing |
| Axios | HTTP client |
| Bootstrap 5 | Styling |

### DevOps
| Technology | Purpose |
|---|---|
| Docker Compose | PostgreSQL containerization |

---

## ✨ Features

### 👤 Customer
- Register & login with JWT authentication
- View dashboard with account summary
- Top up own accounts (cash deposit)
- Transfer funds **from own accounts only**, with **idempotency protection** (no double transfers)
- View transaction history of own accounts
- Update profile

### 🛡️ Admin
- Open bank accounts (Savings / Current) for customers
- Manage all users — view, update profile, change role, delete
- Manage all accounts — view, lock / unlock, delete (blocked when the account has transaction history)
- Safety rails: an admin cannot change their own role or delete themselves, and the last admin cannot be removed
- Privileged actions (role changes, deletions, edits to other users, bootstrap) are written to an audit log

---

## ⚙️ Getting Started

### Prerequisites
- Java 21+
- Node.js 18+
- Docker Desktop

### 1. Clone the repository
```bash
git clone https://github.com/HauShen/Banking-app.git
cd Banking-app
```

### 2. Start PostgreSQL with Docker
```bash
docker compose up -d
```

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

### 5. Run the frontend
```bash
cd src/frontend
cp .env.example .env
npm install
npm run dev
```
Frontend runs on `http://localhost:5173`

---

## 🔐 API Endpoints

All endpoints except `/api/auth/**` require:
```
Authorization: Bearer <your-jwt-token>
```
Try them interactively in Swagger UI (dev profile): `http://localhost:8080/swagger-ui/index.html`.

**Access:** *Public* = no token · *Logged in* = any valid token · *Owner* = the logged-in user owns the account ·
*ADMIN* = admin role. Rules marked *Owner* are enforced in the use cases, not just by role.

#### Authentication
| Method | Endpoint | Access | Description |
|---|---|---|---|
| POST | `/api/auth/register` | Public | Register as a customer, returns a JWT |
| POST | `/api/auth/login` | Public | Log in, returns a JWT |
| POST | `/api/auth/logout` | Logged in | Log out on every device: all of your tokens stop working (204) |
| GET | `/api/auth/bootstrap-status` | Public | Whether bootstrap is enabled and an admin exists |

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
| POST | `/api/transfers` | Owner of the from-account | Transfer funds (201); `idempotencyKey` makes retries safe |
| GET | `/api/transfers/{reference}` | Sender, recipient or ADMIN | One transaction |
| GET | `/api/transfers/account/{accountNumber}` | Owner or ADMIN | Transaction history of an account (paged) |

---

## 🔒 Security

### Authentication: stateless JWT
1. `POST /api/auth/login` checks the password against its **BCrypt** hash and returns a JWT signed with
   HS256 (`sub` = username, 1-hour expiry by default). The signing key comes from `JWT_SECRET`, never from Git.
2. Every request sends `Authorization: Bearer <token>`. `JwtRequestFilter` verifies the signature and expiry,
   then **reloads the user from the database**, so a role change or deletion takes effect on the next request,
   not when the token expires.
3. No server-side session (`SessionCreationPolicy.STATELESS`), so CSRF protection is not needed and is disabled.
4. **Revocation without a blacklist:** every user has a token version (table `user_token_versions`) and every
   token carries the version it was issued with (claim `ver`). `POST /api/auth/logout` — or an admin via
   `/revoke-tokens` — increases the version, so all older tokens of that user are rejected immediately.

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
- CORS only allows the configured frontend origin (`CORS_ALLOWED_ORIGINS`, default `http://localhost:5173`)
  and only the `Authorization` and `Content-Type` headers.
- Swagger UI and the API docs are only enabled in the `dev` profile.
- Transaction responses never contain another user's internal id.
- An idempotency key belongs to the account that first used it: reusing another account's key returns 400
  instead of that account's transaction.
- Transfers and top-ups lock the account rows (`SELECT … FOR UPDATE`, always in the same order), so concurrent
  requests can't overwrite each other's balance or deadlock.

### Known limitations
- Login throttling is in memory: it resets on restart and isn't shared between several instances
  (production: Redis, or rate limiting at the API gateway).
- A username lockout can be abused to lock someone out for 15 minutes by failing on purpose — the usual trade-off
  of account lockout; real systems add CAPTCHA or progressive delays.
- Behind a reverse proxy, enable `server.forward-headers-strategy=native`, otherwise every client shares the
  proxy's IP for the per-IP limit.
- No refresh tokens: an access token lasts 1 hour, then the user logs in again.
- Logout is "log out everywhere": there is no per-device logout, because tokens are not stored per device.
- The frontend keeps the JWT in `localStorage`, which any XSS could read.
- **By design:** on account-number lookups, 404 (unknown) vs 403 (not yours) shows whether an account number
  exists. Hiding it would not help: a transfer has to confirm that the recipient's account exists anyway, as
  real banks do. The defence against probing account numbers is rate limiting, not hiding the status code.

---

## 🧪 Running Tests

Requires **Docker** running: the tests start a real PostgreSQL 17 container with Testcontainers.
```bash
./gradlew test
```

The security tests are black-box HTTP tests: a real embedded server on a random port, real JWTs, a real database
with every Flyway migration applied. Because they only talk HTTP, they stay valid while the code behind the
endpoints is refactored.

| Test Class | What It Covers |
|---|---|
| `AuthenticationSecurityTest` | Missing, garbage, tampered, forged, expired and deleted-user tokens → 401; a stale token doesn't block login; failed logins look identical; the bootstrap admin exists |
| `AuthorizationSecurityTest` | Customer on admin URLs and admin-only methods → 403; `role` in registration is ignored; viewing or topping up someone else's account → 403 |
| `LoginThrottlingSecurityTest` | 5 failures lock a username → 429 with `Retry-After`, even with the right password; unknown usernames lock the same way; other users unaffected; a success resets the count; the lock expires |
| `LoginIpThrottlingSecurityTest` | Failures spread over many usernames from one IP block that IP, then the block expires (own Spring context with a low limit) |
| `TokenRevocationSecurityTest` | Logout → the same token gets 401, on every device, while other users and a fresh login still work; admin revoke-tokens; customers can't revoke others (403); unknown user (404) |
| `CorsSecurityTest` | Preflight from the configured frontend → allowed; from another site, or asking for an unlisted header → 403 |
| `TransferSecurityTest` | Only the owner can transfer (admins too get 403) and balances are checked; idempotent retries debit once; reusing another account's key → 400; history visible only to owner / recipient / admin |
