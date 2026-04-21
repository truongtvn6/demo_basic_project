# Ports used by the demo stack

## Infrastructure (Docker Compose)

| Service   | Host port | Purpose                          |
|-----------|-----------|----------------------------------|
| postgres  | 5432      | PostgreSQL                       |
| rabbitmq  | 5672      | AMQP                             |
| rabbitmq  | 15672     | RabbitMQ Management UI           |
| nifi      | 8443      | Apache NiFi (HTTPS, self-signed) |

NiFi default credentials (from `docker-compose.yml`): `admin` / `adminadminadmin`.
RabbitMQ default credentials: `demo` / `demo`.
PostgreSQL default credentials: `demo` / `demo`, DB `projectdemo`.

## Java microservices (run on host)

| Service                  | Port | Base URL                          |
|--------------------------|------|-----------------------------------|
| device-registry-service  | 8081 | http://localhost:8081             |
| integration-service      | 8082 | http://localhost:8082             |
| notification-service     | 8083 | http://localhost:8083             |

## Management UIs

- RabbitMQ: http://localhost:15672
- NiFi:     https://localhost:8443/nifi (accept self-signed cert)
