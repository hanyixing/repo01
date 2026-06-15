#!/usr/bin/env bash
set -euo pipefail

APP_NAME="community"
JAR_FILE="target/${APP_NAME}-0.0.1-SNAPSHOT.jar"
PID_FILE="${APP_NAME}.pid"
LOG_FILE="logs/${APP_NAME}-startup.log"

if [ -f "$PID_FILE" ]; then
    PID=$(cat "$PID_FILE")
    if kill -0 "$PID" 2>/dev/null; then
        echo "[ERROR] ${APP_NAME} is already running (PID ${PID})"
        exit 1
    fi
    rm -f "$PID_FILE"
fi

if [ ! -f "$JAR_FILE" ]; then
    echo "[ERROR] JAR not found: ${JAR_FILE}"
    echo "Run: mvn package -P prod -DskipTests"
    exit 1
fi

mkdir -p logs

echo "[INFO] Starting ${APP_NAME} ..."
nohup java \
    -XX:+UnlockExperimentalVMOptions \
    -XX:+UseCGroupMemoryLimitForHeap \
    -jar "$JAR_FILE" \
    --spring.profiles.active=prod \
    > "$LOG_FILE" 2>&1 &

echo $! > "$PID_FILE"
echo "[INFO] ${APP_NAME} started (PID $(cat $PID_FILE)), log: ${LOG_FILE}"
