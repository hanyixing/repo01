#!/usr/bin/env bash
# Build images, (re)start the stack, and wait until the app is healthy.
set -euo pipefail
cd "$(dirname "$0")"

if [ ! -f .env ]; then
  echo "ERROR: .env not found. Copy .env.example to .env and fill in values." >&2
  exit 1
fi

# Load SERVER_PORT (and other vars) for the health check; default port is 8887.
set -a
# shellcheck disable=SC1091
. ./.env
set +a
PORT="${SERVER_PORT:-8887}"

echo "==> Building images..."
docker compose build

echo "==> Starting services..."
docker compose up -d

echo "==> Waiting for the app to become healthy on http://localhost:${PORT}/ ..."
ATTEMPTS=60
until curl -fsS "http://localhost:${PORT}/" >/dev/null 2>&1; do
  ATTEMPTS=$((ATTEMPTS - 1))
  if [ "$ATTEMPTS" -le 0 ]; then
    echo "ERROR: app did not become healthy in time. Recent logs:" >&2
    docker compose logs --tail=50 app >&2
    exit 1
  fi
  sleep 5
done

echo "==> Deployment complete. Service status:"
docker compose ps
