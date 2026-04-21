# demobasic

Java 17 + Maven monorepo demonstrating an event-driven pipeline behind a single API Gateway, with a React admin UI and optional real FCM push:

```
Browser (React + FCM SW) --Bearer--> API Gateway ---> device-registry
                                                ---> integration (Jira / GitHub / Mock -> RabbitMQ)
                                                ---> notification (RabbitMQ consumer -> FCM)
                                           shared Postgres (registry / integration / notification schemas)
```

## Modules

- [`api-gateway`](api-gateway/) — Spring Cloud Gateway (Netty), port **8080**. Single entry point for the UI: routes `/api/registry|integration|notification/**` to downstream services, global CORS, Bearer token filter, request log.
- [`device-registry-service`](device-registry-service/) — REST API to register FCM device tokens. Persists to Postgres (schema `registry`).
- [`integration-service`](integration-service/) — polls Jira and GitHub via REST, publishes `ProjectEvent` messages to RabbitMQ; persists sync cursors (schema `integration`).
- [`notification-service`](notification-service/) — consumes RabbitMQ, sends Firebase Cloud Messaging push, logs deliveries (schema `notification`).
- [`frontend/`](frontend/) — Vite + React + TypeScript + Tailwind admin UI: manage tokens, trigger syncs, watch live events, opt into real FCM web push.

## Quick start

```bash
docker compose up -d
mvn -q -DskipTests package

# 4 terminals (or background)
java -jar device-registry-service/target/device-registry-service-0.1.0-SNAPSHOT.jar
java -jar integration-service/target/integration-service-0.1.0-SNAPSHOT.jar
java -jar notification-service/target/notification-service-0.1.0-SNAPSHOT.jar
java -jar api-gateway/target/api-gateway-0.1.0-SNAPSHOT.jar

cd frontend
cp .env.example .env
npm install
npm run dev            # http://localhost:5173
```

Smoke test:

```bash
# 401 without token
curl -i http://localhost:8080/api/registry/devices

# 200 with token
curl -H "Authorization: Bearer dev-token" http://localhost:8080/api/registry/devices

# Publish a mock event; watch the UI Events tab
curl -X POST -H "Authorization: Bearer dev-token" http://localhost:8080/api/integration/sync/mock
```

See [docs/runbook.md](docs/runbook.md) for environment variables, the full test matrix (including Jira, GitHub, NiFi, FCM Web push) and troubleshooting. See [docs/ports.md](docs/ports.md) for the port layout.
