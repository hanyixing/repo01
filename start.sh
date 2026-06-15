#!/usr/bin/env bash
# Start the community stack (app + MySQL) via docker compose.
set -euo pipefail
cd "$(dirname "$0")"

if [ ! -f .env ]; then
  echo "ERROR: .env not found. Copy .env.example to .env and fill in values." >&2
  exit 1
fi

echo "Starting community stack..."
docker compose up -d
docker compose ps
