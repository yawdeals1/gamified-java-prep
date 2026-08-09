#!/bin/sh
set -e

# Convert Deploro's libpq-style connection string (postgresql://user:pass@host:port/db)
# into the JDBC URL + credentials Spring expects.
# Prefer the VPS-internal URL (no firewall allowlist needed); fall back to public.
CONN="${DATABASE_URL_INTERNAL:-${DATABASE_URL:-}}"
if [ -n "$CONN" ]; then
  core="${CONN#*://}"
  userpass="${core%%@*}"
  hostpart="${core#*@}"
  user="${userpass%%:*}"
  pass="${userpass#*:}"
  hostport="${hostpart%%/*}"
  dbname="${hostpart#*/}"
  export DB_URL="jdbc:postgresql://${hostport}/${dbname}"
  export DB_USER="${user}"
  export DB_PASSWORD="${pass}"
  echo "[entrypoint] Database resolved via ${DATABASE_URL_INTERNAL:+internal}${DATABASE_URL_INTERNAL:-public} URL"
fi

exec java ${JAVA_OPTS:-} -jar app.jar