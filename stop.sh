#!/usr/bin/env bash
# Stop the community stack (keeps the MySQL data volume).
set -euo pipefail
cd "$(dirname "$0")"

echo "Stopping community stack..."
docker compose down
