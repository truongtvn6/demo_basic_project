# Runbook — Java 17 demo (NiFi / RabbitMQ / PostgreSQL / Jira / GitHub / FCM)

## 1. Prerequisites

- JDK 17 and Maven 3.9+
- Docker + Docker Compose
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

java -jar device-registry-service/target/device-registry-service-0.1.0-SNAPSHOT.jar
java -jar integration-service/target/integration-service-0.1.0-SNAPSHOT.jar
java -jar notification-service/target/notification-service-0.1.0-SNAPSHOT.jar
```

Run each service in its own terminal. When `FCM_ENABLED=false`, the notification service logs a warning instead of sending a push — useful for running the full pipeline without Firebase.

## 5. Test matrix

### 5.1 Register a (fake) device token

```bash
curl -X POST http://localhost:8081/devices/register \
  -H "Content-Type: application/json" \
  -d '{"token":"FAKE-TOKEN-1","label":"demo"}'

curl http://localhost:8081/internal/tokens
```

### 5.2 Publish a mock event without Jira/GitHub credentials

```bash
curl -X POST http://localhost:8082/sync/mock
```

Expected: message appears in RabbitMQ queue `project.events.q` with routing key `demo.mock.tick`; the notification service logs `Received event source=MOCK ...` and (if FCM disabled) `would send '[MOCK] ...'`.

### 5.3 Trigger Jira / GitHub sync

```bash
curl -X POST http://localhost:8082/sync/jira
curl -X POST http://localhost:8082/sync/github
curl -X POST http://localhost:8082/sync
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
curl http://localhost:8081/internal/tokens   # registered tokens still present
```

The `pgdata` named volume keeps PostgreSQL data between restarts. Use `docker compose down -v` to wipe all data.

## 6. Troubleshooting

- **`connection refused` on app start:** Postgres or RabbitMQ not ready yet. `docker compose ps` and wait for `(healthy)`, then restart the app.
- **Flyway validation fails:** a migration was changed in place. For the demo you can drop the schema (`docker compose exec postgres psql -U demo -d projectdemo -c 'DROP SCHEMA registry CASCADE;'`) and restart.
- **NiFi cannot reach services on the host:** from a container, `localhost` means the container itself. Use `host.docker.internal` (Docker Desktop on Windows/macOS) or add `extra_hosts: ["host.docker.internal:host-gateway"]` on Linux.
- **RabbitMQ publish from NiFi fails:** the Apache NiFi image ships multiple AMQP processors (0-9-1 and 1.0). Pick `PublishAMQP` for classic 0-9-1 and point it at port `5672` on the `rabbitmq` service. If still failing, fall back to `InvokeHTTP` -> `POST /sync/mock` on the integration service.
- **Duplicate FCM notifications:** `event_delivery_log` has a unique `(source, external_id)` index, so the listener deduplicates based on those fields. Re-delivered AMQP messages or idempotent publishers will therefore skip the second send.
- **Disable FCM for a dry run:** leave `FCM_ENABLED=false`. The listener still writes to `event_delivery_log` and logs the would-be notification.

## 7. Clean up

```bash
docker compose down           # keep volumes (Postgres keeps tokens & sync state)
docker compose down -v        # wipe everything
```
