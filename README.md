# demobasic

Java 17 + Maven monorepo demonstrating an event-driven pipeline:

```
Jira / GitHub / NiFi -> RabbitMQ -> notification-service -> FCM
                                      ^
                          device-registry-service
                                      |
                                 PostgreSQL
```

## Modules

- [`device-registry-service`](device-registry-service/) — REST API to register FCM device tokens. Persists to Postgres (schema `registry`).
- [`integration-service`](integration-service/) — polls Jira and GitHub via REST, publishes `ProjectEvent` messages to RabbitMQ. Persists sync cursors (schema `integration`).
- [`notification-service`](notification-service/) — consumes RabbitMQ, sends Firebase Cloud Messaging push, logs deliveries (schema `notification`).

## Quick start

```bash
docker compose up -d
mvn -q -DskipTests package
java -jar device-registry-service/target/device-registry-service-0.1.0-SNAPSHOT.jar &
java -jar integration-service/target/integration-service-0.1.0-SNAPSHOT.jar &
java -jar notification-service/target/notification-service-0.1.0-SNAPSHOT.jar &

curl -X POST http://localhost:8081/devices/register \
  -H "Content-Type: application/json" -d '{"token":"FAKE-1"}'
curl -X POST http://localhost:8082/sync/mock
```

See [docs/runbook.md](docs/runbook.md) for environment variables, the full test matrix (including Jira, GitHub, NiFi, FCM) and troubleshooting. See [docs/ports.md](docs/ports.md) for the port layout.
