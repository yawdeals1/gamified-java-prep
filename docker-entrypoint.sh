#!/bin/sh
set -e

# Data store is the Deploro Studio API — no DB string conversion needed.
# The app reads DEPLORO_API_URL / DEPLORO_API_TOKEN directly.
if [ -z "$DEPLORO_API_URL" ] || [ -z "$DEPLORO_API_TOKEN" ]; then
  echo "[entrypoint] WARNING: DEPLORO_API_URL / DEPLORO_API_TOKEN not set — app will fail fast on first data access." 1>&2
fi

exec java ${JAVA_OPTS:-} -jar app.jar