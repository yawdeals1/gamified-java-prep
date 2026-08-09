# Deploy runbook — Gamified Java Prep

Stack: Spring Boot 3 (Java 21) · Deploro Studio API as the data store · Deploro Auth-as-a-Service.

## 1. Set up the environment on the host

The image reads everything from env vars. No database credentials, no config files.

| Variable | Required | Meaning |
|---|---|---|
| `DEPLORO_API_URL` | yes | `https://<deploro-worker>/api/projects/<project-id>/studio` |
| `DEPLORO_API_TOKEN` | yes | Project-scoped PAT (`deploro token create`, 365d) |
| `DEPLORO_AUTH_BASE_URL` | yes | Your Deploro worker base URL |
| `DEPLORO_AUTH_SLUG` | yes | Project slug (`gamified-java-prep`) |
| `OLLAMA_BASE_URL` | no | AI backend (local Ollama or cloud gateway) |
| `OLLAMA_API_KEY` | no | Bearer key for cloud gateways only |
| `OLLAMA_MODEL` | no | Default `gemma4:31b-cloud` |
| `OLLAMA_PROTOCOL` | no | `ollama` (default) or `openai` |
| `BIND_ADDRESS` | no | Default `127.0.0.1`; must be `0.0.0.0` in Docker |
| `JAVA_OPTS` | no | e.g. `-Xmx1g` |

## 2. Build and run

```bash
docker compose up -d --build     # uses docker-compose.yml (env passthrough)
```

The runtime image is a JDK (`eclipse-temurin:21-jdk`) — required because the app
compiles learner Java in-process (`javax.tools.JavaCompiler`).

## 3. Point Auth confirmation links at the app

```bash
deploro auth site-url https://<your-app-public-url>
```

Signups send a confirmation email (Cloudflare Email Sending); clicking the link
verifies the account and redirects back to this URL.

## 4. Data / migrations

- Schema lives in the Deploro Studio database, applied as migration `schema`
  (`deploro migrate list`).
- App data (modules, steps, progress, XP) is per-project via the Studio REST API.
- Resetting progress: wipe the tables or call `POST /api/reset` (signed in).

## 5. If the token expires

Create a new one and redeploy:

```bash
deploro token create --project gamified-java-prep --name gamified-java-prep-app --scopes read write
# put the new value in DEPLORO_API_TOKEN, restart the container
```

Old token can be deleted with `deploro token delete <id>`.