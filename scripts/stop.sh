#!/usr/bin/env bash
set -euo pipefail

APP_NAME="community"
PID_FILE="${APP_NAME}.pid"

if [ ! -f "$PID_FILE" ]; then
    echo "[WARN] PID file not found -- ${APP_NAME} may not be running"
    exit 0
fi

PID=$(cat "$PID_FILE")

if ! kill -0 "$PID" 2>/dev/null; then
    echo "[WARN] Process ${PID} is not running"
    rm -f "$PID_FILE"
    exit 0
fi

echo "[INFO] Stopping ${APP_NAME} (PID ${PID}) ..."
kill "$PID"

# Graceful shutdown: wait up to 30s
for i in $(seq 1 30); do
    if ! kill -0 "$PID" 2>/dev/null; then
        echo "[INFO] ${APP_NAME} stopped."
        rm -f "$PID_FILE"
        exit 0
    fi
    sleep 1
done

echo "[WARN] Graceful shutdown timed out -- sending SIGKILL"
kill -9 "$PID"
rm -f "$PID_FILE"
echo "[INFO] ${APP_NAME} killed."
