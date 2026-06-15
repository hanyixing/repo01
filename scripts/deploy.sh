#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "====== Deploy: community ======"

echo "[1/4] Pulling latest code ..."
git pull origin master

echo "[2/4] Building ..."
./mvnw clean package -P prod -DskipTests -B

echo "[3/4] Stopping existing instance ..."
"${SCRIPT_DIR}/stop.sh"

echo "[4/4] Starting new instance ..."
"${SCRIPT_DIR}/start.sh"

echo "====== Deploy complete ======"
