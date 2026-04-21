# Runbook — Java 17 demo (Gateway / NiFi / RabbitMQ / PostgreSQL / Jira / GitHub / FCM / React FE)

## 1. Prerequisites

- JDK 17 and Maven 3.9+
- Docker + Docker Compose
- Node.js 20+ and npm (for the frontend)
- (Optional) A Firebase project with a service account JSON, a Jira Cloud API token, a GitHub PAT.

## 2. Start infrastructure

```bash
docker compose up -d
docker compose ps
```

Services exposed on localhost (see [docs/ports.md](ports.md)):

- PostgreSQL: `localhost:5432` (user `demo`, password `demo`, db `projectdemo`)
- RabbitMQ: AMQP `localhost:5672`, Management UI http://localhost:15672 (`demo` / `demo`)
- NiFi UI: https://localhost:8443/nifi (`admin` / `adminadminadmin`)

Each service uses its own Postgres schema (`registry`, `integration`, `notification`), created automatically by Flyway on first start.

## 3. Environment variables

### Shared (RabbitMQ + PostgreSQL)

- `SPRING_RABBITMQ_HOST` (default `localhost`)
- `SPRING_RABBITMQ_PORT` (default `5672`)
- `SPRING_RABBITMQ_USERNAME` (default `demo`)
- `SPRING_RABBITMQ_PASSWORD` (default `demo`)
- `SPRING_DATASOURCE_URL` (default `jdbc:postgresql://localhost:5432/projectdemo`)
- `SPRING_DATASOURCE_USERNAME` (default `demo`)
- `SPRING_DATASOURCE_PASSWORD` (default `demo`)

### `api-gateway`

- `SERVER_PORT` (default `8080`)
- `DEMOBASIC_API_TOKEN` (default `dev-token`) — every request must include `Authorization: Bearer <token>` except `OPTIONS` preflight and `/actuator/health`.
- `DEMOBASIC_CORS_ORIGIN` (default `http://localhost:5173`)
- `REGISTRY_HOST` / `REGISTRY_PORT` (defaults `localhost:8081`)
- `INTEGRATION_HOST` / `INTEGRATION_PORT` (defaults `localhost:8082`)
- `NOTIFICATION_HOST` / `NOTIFICATION_PORT` (defaults `localhost:8083`)

### `device-registry-service`

- `SERVER_PORT` (default `8081`)

### `integration-service`

- `SERVER_PORT` (default `8082`)
- Jira: `JIRA_BASE_URL`, `JIRA_USER_EMAIL`, `JIRA_API_TOKEN`, `JIRA_PROJECT_KEY`
- GitHub: `GITHUB_TOKEN`, `GITHUB_OWNER`, `GITHUB_REPO`, `GITHUB_BRANCH` (optional)
- Scheduler: `SYNC_SCHEDULER_ENABLED` (default `false`), `SYNC_INTERVAL_MS` (default `60000`)

### `notification-service`

- `SERVER_PORT` (default `8083`)
- `REGISTRY_BASE_URL` (default `http://localhost:8081`)
- `FCM_ENABLED` (default `false`)
- `FIREBASE_SERVICE_ACCOUNT_PATH` (required when `FCM_ENABLED=true`)

## 4. Build and run the services

```bash
mvn -q -DskipTests package

# Run each in its own terminal (order is not critical; gateway polls downstream lazily)
java -jar device-registry-service/target/device-registry-service-0.1.0-SNAPSHOT.jar
java -jar integration-service/target/integration-service-0.1.0-SNAPSHOT.jar
java -jar notification-service/target/notification-service-0.1.0-SNAPSHOT.jar
java -jar api-gateway/target/api-gateway-0.1.0-SNAPSHOT.jar
```

Run each service in its own terminal. When `FCM_ENABLED=false`, the notification service logs a warning instead of sending a push — useful for running the full pipeline without Firebase.

## 4b. Run the frontend (Vite dev server)

```bash
cd frontend
cp .env.example .env     # then tweak values
npm install              # first run only
npm run dev              # http://localhost:5173
```

The frontend talks only to the API gateway (`VITE_API_BASE_URL`, default `http://localhost:8080`) using `Authorization: Bearer ${VITE_API_TOKEN}`. Make sure `VITE_API_TOKEN` matches `DEMOBASIC_API_TOKEN` on the gateway.

To enable real browser push in the UI:

1. Set `VITE_FCM_ENABLED=true`.
2. Fill all `VITE_FIREBASE_*` variables (Firebase Console → Project settings → Web app config + Cloud Messaging → Web configuration → Generate key pair for VAPID).
3. On the notification service, set `FCM_ENABLED=true` and `FIREBASE_SERVICE_ACCOUNT_PATH=/path/to/serviceAccount.json`.
4. Restart `notification-service` and `npm run dev`; click **Enable push on this browser** on the Tokens tab, grant the permission prompt, then trigger a sync.

## 5. Test matrix

All external calls go through the gateway on `:8080`. Direct calls to `:8081/:8082/:8083` still work but are not recommended (no auth, no CORS).

### 5.0 Gateway auth sanity

```bash
# Missing token -> 401
curl -i http://localhost:8080/api/registry/internal/tokens

# With token -> 200
curl -H "Authorization: Bearer dev-token" http://localhost:8080/api/registry/internal/tokens
```

### 5.1 Register a (fake) device token (via gateway)

```bash
curl -X POST http://localhost:8080/api/registry/devices/register \
  -H "Authorization: Bearer dev-token" \
  -H "Content-Type: application/json" \
  -d '{"token":"FAKE-TOKEN-1","label":"demo"}'

curl -H "Authorization: Bearer dev-token" http://localhost:8080/api/registry/devices
```

### 5.2 Publish a mock event without Jira/GitHub credentials

```bash
curl -X POST -H "Authorization: Bearer dev-token" http://localhost:8080/api/integration/sync/mock
```

Expected: message appears in RabbitMQ queue `project.events.q` with routing key `demo.mock.tick`; the notification service logs `Received event source=MOCK ...` and (if FCM disabled) `would send '[MOCK] ...'`. The UI Events tab shows the new row within 3 s.

### 5.3 Trigger Jira / GitHub sync

```bash
curl -X POST -H "Authorization: Bearer dev-token" http://localhost:8080/api/integration/sync/jira
curl -X POST -H "Authorization: Bearer dev-token" http://localhost:8080/api/integration/sync/github
curl -X POST -H "Authorization: Bearer dev-token" http://localhost:8080/api/integration/sync
curl -H "Authorization: Bearer dev-token" http://localhost:8080/api/integration/sync/state
```

The response body contains the number of events published per source. `sync_state` in Postgres records the last Jira timestamp and the last GitHub SHA, so re-calling `/sync` immediately publishes zero new events.

### 5.4 NiFi flow (mock events)

Example flow in NiFi (UI at https://localhost:8443/nifi):

- `GenerateFlowFile` (run schedule 30 sec) producing a JSON body like:
  ```json
  {"source":"MOCK","eventType":"TaskUpdated","externalId":"NIFI-1","title":"NiFi mock","detail":"from NiFi","status":"InProgress","url":null,"occurredAt":"2026-04-21T10:00:00Z"}
  ```
- `PublishAMQP` (or AMQP 1.0 equivalent) configured to:
  - Host: `rabbitmq`, Port: `5672`, User: `demo`, Password: `demo`
  - Exchange: `project.events`, Routing key: `demo.mock.tick`

Alternative: use `InvokeHTTP` `POST http://host.docker.internal:8082/sync/mock` to let the Java service publish the message (secrets stay in Java).

### 5.5 Verify persistence

After stopping and restarting the stack:

```bash
docker compose down
docker compose up -d
curl -H "Authorization: Bearer dev-token" http://localhost:8080/api/registry/devices
```

The `pgdata` named volume keeps PostgreSQL data between restarts. Use `docker compose down -v` to wipe all data.

## 6. Troubleshooting

- **Gateway returns 401:** missing or wrong `Authorization: Bearer <token>`. Check `DEMOBASIC_API_TOKEN` and `VITE_API_TOKEN` match.
- **CORS error in browser console:** the FE must use `VITE_API_BASE_URL=http://localhost:8080`; CORS is only configured on the gateway. Do not call `:8081/:8082/:8083` from the browser.
- **Gateway 503 / connection refused downstream:** the corresponding backend is not up yet. Start the 3 downstream jars before or together with the gateway.
- **`connection refused` on app start:** Postgres or RabbitMQ not ready yet. `docker compose ps` and wait for `(healthy)`, then restart the app.
- **Flyway validation fails:** a migration was changed in place. For the demo you can drop the schema (`docker compose exec postgres psql -U demo -d projectdemo -c 'DROP SCHEMA registry CASCADE;'`) and restart.
- **NiFi cannot reach services on the host:** from a container, `localhost` means the container itself. Use `host.docker.internal` (Docker Desktop on Windows/macOS) or add `extra_hosts: ["host.docker.internal:host-gateway"]` on Linux.
- **RabbitMQ publish from NiFi fails:** the Apache NiFi image ships multiple AMQP processors (0-9-1 and 1.0). Pick `PublishAMQP` for classic 0-9-1 and point it at port `5672` on the `rabbitmq` service. If still failing, fall back to `InvokeHTTP` -> `POST /api/integration/sync/mock` on the gateway (remember the Bearer header).
- **FCM getToken fails with `messaging/token-subscribe-failed`:** usually a wrong VAPID key. Regenerate the Web push certificate in Firebase Console → Cloud Messaging.
- **FCM service worker 404:** ensure you are running the Vite dev server (`npm run dev`) and the file `frontend/public/firebase-messaging-sw.js` is present. The SW is served from `http://localhost:5173/firebase-messaging-sw.js`.
- **Duplicate FCM notifications:** `event_delivery_log` has a unique `(source, external_id)` index, so the listener deduplicates based on those fields. Re-delivered AMQP messages or idempotent publishers will therefore skip the second send.
- **Disable FCM for a dry run:** leave `FCM_ENABLED=false` on the notification service and `VITE_FCM_ENABLED=false` on the FE; the listener still writes to `event_delivery_log` and the UI works without push.

## 7. Clean up

```bash
docker compose down           # keep volumes (Postgres keeps tokens & sync state)
docker compose down -v        # wipe everything
```
