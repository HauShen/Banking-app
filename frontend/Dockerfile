# syntax=docker/dockerfile:1

# ---------- Stage 1: build the static files ----------
FROM node:22-alpine AS build
WORKDIR /app

# Dependencies first, so this layer is reused while only the source changes.
# npm ci installs exactly package-lock.json; the cache mount keeps downloads between builds.
COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci --no-audit --no-fund

COPY . .
# Same command as CI: type-check, lint (vite-plugin-checker) and the production build into dist/.
RUN npm run build

# ---------- Stage 2: serve them with nginx ----------
# The unprivileged image runs nginx as a non-root user and listens on 8080 (like the backend image).
FROM nginxinc/nginx-unprivileged:stable-alpine

# The official entrypoint fills ${API_UPSTREAM} and ${NOTIFICATIONS_UPSTREAM} into this template at start-up
# (only defined environment variables are replaced; nginx's own $variables stay as they are).
COPY docker/nginx.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist /usr/share/nginx/html

# Where /api is forwarded to: the backend service in docker-compose.yaml,
# and /api/notifications: the notification-service service.
ENV API_UPSTREAM=http://app:8080
ENV NOTIFICATIONS_UPSTREAM=http://notification-service:8082

EXPOSE 8080
