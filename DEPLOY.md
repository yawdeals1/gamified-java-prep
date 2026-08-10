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
| `INVITE_MAILER_URL` | yes for invitations | Secret-protected Cloudflare email helper `/send` URL |
| `INVITE_MAILER_SECRET` | yes for invitations | Shared secret stored in both Wrangler and Deploro |
| `INVITE_FROM_EMAIL` | yes for invitations | Verified sender address (currently `invite@deploro.com`) |
| `BIND_ADDRESS` | no | Default `127.0.0.1`; must be `0.0.0.0` in Docker |
| `JAVA_OPTS` | no | e.g. `-Xmx1g` |
| `CODE_RUNNER_EXECUTION_ENABLED` | no | Enables compile-and-run; the provided Docker stack sets this to `true` |

## 2. Build and run

```bash
docker compose up -d --build     # uses docker-compose.yml (env passthrough)
```

The runtime image is a JDK (`eclipse-temurin:21-jdk`) — required because the app
compiles learner Java in-process (`javax.tools.JavaCompiler`).

The provided stack enables lesson code execution. The runner clears the child
environment, blocks host-access APIs, caps memory and output, and enforces a hard
timeout. Keep the application container isolated from host mounts and the Docker
socket. A dedicated sandbox service is still recommended for an untrusted public
multi-tenant deployment.

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
