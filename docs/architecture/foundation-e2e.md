# Foundation E2E

This document describes the runnable microservice foundation before CRM work starts.

## Services

```text
Frontend              3000
API Gateway           8080
Identity Service      8081
Notification Service  8082
PostgreSQL            5432
RabbitMQ              5672
RabbitMQ UI           15672
Mailpit SMTP          1025
Mailpit UI            8025
```

## Databases

```text
identity_db
notification_db
```

Identity owns `identity_db`. Notification owns `notification_db`. Notification must not connect to `identity_db`.

## Startup Order

1. PostgreSQL
2. RabbitMQ
3. Mailpit
4. Identity Service
5. Notification Service
6. API Gateway
7. Frontend

## Registration Sequence

```text
Browser
  -> API Gateway /api/auth/register
  -> Identity Service
  -> identity_db users/companies/company_members/email_verification_tokens/outbox_events
  -> RabbitMQ vednex.events
  -> vednex.notification.identity
  -> Notification Service
  -> notification_db processed_events/notification_deliveries
  -> Mailpit
```

The API response does not wait for SMTP delivery. The verification token is still generated and validated by Identity. Notification only renders and sends the email.

## JWT Flow

Identity signs access JWTs with RS256 using the private key. Gateway validates JWTs locally with the public key and forwards the Authorization header unchanged. Identity validates the same token again before protected Identity endpoints execute tenant and permission checks.

## Refresh Flow

Refresh tokens remain opaque, hashed in Identity storage, rotated on use, and family-aware. Gateway does not own refresh-token state.

## Event Flow

Identity writes outbox rows in the same transaction as business data. The outbox publisher uses RabbitMQ publisher confirms before marking an event `PUBLISHED`. Sensitive notification event payload data is redacted in the outbox row after broker confirmation.

Current Notification bindings:

```text
identity.email.verification.requested.v1
identity.password.reset.requested.v1
identity.user.invited.v1
```

## Failure Recovery

- RabbitMQ down: Identity business writes still commit and outbox rows remain pending/retryable.
- Notification down: RabbitMQ keeps queued messages.
- Mailpit/SMTP down: Notification retries provider delivery and eventually dead-letters exhausted failures.
- Duplicate delivery: Notification checks `processed_events` and does not send again after successful processing.

## Local Blockers Observed

Docker Desktop must be running with the `desktop-linux` context reachable at `npipe:////./pipe/dockerDesktopLinuxEngine`. If the pipe is missing, start Docker Desktop manually and ensure the user has access to Docker Desktop logs and WSL integration.

On this Windows environment, Docker CLI access and Java Testcontainers access were different. `docker version` and Compose worked after Docker Desktop started, but Testcontainers still failed from Maven with an invalid/empty Docker info response from the Windows named pipe. Treat that as an environment issue until Java processes can access the Docker Desktop Linux engine directly.

Next.js 16 production build can fail with `spawn EPERM` during page-data collection when child process creation is restricted. Direct `tsc --noEmit` and `eslint` can still pass. Retry `npm run build` from a normal non-restricted shell, remove stale `.next` output if needed, and check Windows security tooling, file locks, and directory permissions.

## Validation Commands

```powershell
java -version
docker version
docker info
docker compose config --quiet
docker compose up -d
docker compose ps
```

```powershell
cd services/api-gateway
.\mvnw.cmd verify

cd ..\notification-service
.\mvnw.cmd verify

cd ..\identity-service
.\mvnw.cmd clean test
.\mvnw.cmd verify
```

```powershell
cd frontend
npm run lint
npm run typecheck
npm run build
```
