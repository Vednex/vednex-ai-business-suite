# Vednex AI Business Suite

This repository contains the frontend, API Gateway, and the first extracted backend service.

## Structure

- `frontend/` - Next.js application.
- `services/api-gateway/` - Spring Cloud Gateway edge service.
- `services/identity-service/` - Spring Boot Identity Service.
- `services/notification-service/` - internal Spring Boot notification consumer.
- `infrastructure/` - infrastructure assets.
- `docs/` - project documentation.
- `docker-compose.yml` - local PostgreSQL dependency.

## Prerequisites

- JDK 17 for current Java services.
- Node.js/npm for the frontend.
- Docker Desktop for PostgreSQL, RabbitMQ, Mailpit, and containerized local runs. On Windows, Docker Desktop must be running with Linux containers and the `desktop-linux` context available.
- RabbitMQ for event publishing and notification delivery in local distributed runs.
- Mailpit for safe local email capture.

## Identity Service

The Identity Service owns authentication, users, companies, memberships, company settings, RBAC, invitations, tokens, audit logs, plans, and subscriptions.

Default local settings:

- Port: `8081`
- Database: `identity_db`
- Flyway migrations: `services/identity-service/src/main/resources/db/migration/`

## API Gateway

The API Gateway is the browser-facing backend entry point. It validates protected access JWTs with the RSA public key and forwards Identity APIs unchanged to the Identity Service.

Default local settings:

- Port: `8080`
- Identity route target: `IDENTITY_SERVICE_URL`, default `http://localhost:8081`
- Frontend origins: `FRONTEND_ALLOWED_ORIGINS`, default `http://localhost:3000`
- JWT public key: `JWT_PUBLIC_KEY` or `JWT_PUBLIC_KEY_FILE`

## Notification Service

Notification Service consumes Identity notification events from RabbitMQ and sends email. It is internal and is not routed through API Gateway.

Default local settings:

- Port: `8082`
- Database: `notification_db`
- Queue: `vednex.notification.identity`
- Development SMTP: `mailpit:1025`
- Development UI: `http://localhost:8025`
- Consumed routing keys:
  - `identity.email.verification.requested.v1`
  - `identity.password.reset.requested.v1`
  - `identity.user.invited.v1`

Notification owns email templates, provider delivery, `processed_events`, and `notification_deliveries`. Identity still owns verification/reset/invitation token generation and validation.

## RSA JWT Setup

Identity Service is the only service that signs access tokens. Configure it with:

```text
JWT_PRIVATE_KEY
JWT_PUBLIC_KEY
```

or file paths:

```text
JWT_PRIVATE_KEY_FILE
JWT_PUBLIC_KEY_FILE
```

API Gateway receives only:

```text
JWT_PUBLIC_KEY
JWT_PUBLIC_KEY_FILE
```

For Docker local development, place PEM files in ignored local paths:

```text
secrets/jwt-private.pem
secrets/jwt-public.pem
```

Do not commit real private keys. See `docs/security/jwt-security.md`.

## Local Startup Order

1. PostgreSQL
2. RabbitMQ
3. Mailpit
4. Identity Service on `localhost:8081`
5. Notification Service on `localhost:8082`
6. API Gateway on `localhost:8080`
7. Frontend on `localhost:3000`

## RabbitMQ And Outbox

RabbitMQ local defaults:

- AMQP: `localhost:5672`
- Management UI: `http://localhost:15672`
- Mailpit UI: `http://localhost:8025`
- Exchange: `vednex.events`
- Dead-letter exchange: `vednex.events.dlx`
- Bootstrap archive queue: `vednex.events.identity.archive`
- Notification queue: `vednex.notification.identity`

Configure with:

```text
RABBITMQ_HOST
RABBITMQ_PORT
RABBITMQ_USERNAME
RABBITMQ_PASSWORD
RABBITMQ_VHOST
```

Identity stores events in `outbox_events` inside the same PostgreSQL transaction as business data. A scheduled publisher later sends pending events to RabbitMQ. If RabbitMQ is down, authentication and registration data writes can still commit and events remain pending.

Notification stores consumed email event state in `notification_db`. Useful inspection query:

```sql
select event_type, notification_type, recipient, status, attempt_count, created_at, sent_at
from notification_deliveries
order by created_at desc;
```

Useful inspection query:

```sql
select id, event_type, status, retry_count, next_retry_at, created_at
from outbox_events
order by created_at desc;
```

Troubleshooting:

- Check RabbitMQ Management UI queues and exchanges.
- Check `/actuator/health` for `rabbitmqOutbox` details.
- Review `FAILED` rows in `outbox_events` after max retries.
- Check `notification_deliveries` for `FAILED` or `PERMANENT_FAILED` email deliveries.
- Check `vednex.events.dlq` for malformed or exhausted notification messages.
- Check Mailpit UI for development emails.
- If Docker CLI works but Java Testcontainers cannot start, verify that the current user can access Docker Desktop's Linux engine named pipe from Java, not only from `docker.exe`.
- If `next build` fails with `spawn EPERM` during page-data collection on Windows, retry from a normal non-restricted shell and check process-spawn restrictions, stale `.next` locks, security tooling, and directory permissions.

Event docs:

- `docs/events/event-naming.md`
- `docs/events/event-envelope.md`
- `docs/events/rabbitmq-outbox.md`
- `docs/events/event-versioning.md`
- `docs/events/idempotency.md`
- `docs/architecture/notification-service.md`
- `docs/architecture/foundation-e2e.md`
