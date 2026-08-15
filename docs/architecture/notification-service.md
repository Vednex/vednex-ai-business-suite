# Notification Service

Notification Service is an internal event consumer. It is not exposed through API Gateway in this migration step.

## Responsibilities

- Consume selected Identity notification events.
- Render email templates.
- Send email through the configured provider.
- Store idempotency records in `processed_events`.
- Store delivery status in `notification_deliveries`.
- Retry transient provider failures with bounded attempts.
- Dead-letter permanently invalid or exhausted messages.

It does not own users, passwords, JWTs, refresh tokens, verification-token validation, reset-token validation, invitations, company membership, RBAC, or tenant authorization.

## Runtime

- Service: `services/notification-service`
- Port: `8082`
- Database: `notification_db`
- Queue: `vednex.notification.identity`
- Exchange: `vednex.events`
- DLX: `vednex.events.dlx`
- DLQ: `vednex.events.dlq`
- Development SMTP: Mailpit at `mailpit:1025`
- Development UI: `http://localhost:8025`

## Bindings

```text
identity.email.verification.requested.v1
identity.password.reset.requested.v1
identity.user.invited.v1
```

The service does not consume every `identity.#` event.

## Idempotency

`processed_events` has a primary key on `(event_id, consumer_name)`. Duplicate messages already marked processed are acknowledged without sending another email.

`notification_deliveries.event_id` is unique and tracks:

```text
PROCESSING
SENT
FAILED
PERMANENT_FAILED
```

## Retries And DLQ

Provider delivery uses bounded in-process attempts configured by:

```text
NOTIFICATION_EMAIL_MAX_ATTEMPTS
NOTIFICATION_EMAIL_RETRY_BASE_MS
```

Malformed events, unsupported event versions, permanent provider failures, and exhausted transient failures are rejected without requeue and dead-lettered through `vednex.events.dlx`.

## Secret Handling

Verification, reset, and invitation URLs contain raw one-time tokens. Notification Service may use those URLs to build email bodies, but must not log the full URL or token. Delivery logs mask recipients and record safe status metadata only.

`notification_deliveries` stores recipient, type, template key, provider ID, status, and safe error text. It does not store passwords, JWTs, refresh tokens, token hashes, token URLs, or RSA private keys.

Identity stores token URLs in `outbox_events` only until RabbitMQ confirms publication. After broker confirmation, Identity marks the event `PUBLISHED` and redacts the envelope `data` object for these sensitive notification events.

## Failure Scenarios

- RabbitMQ unavailable: Identity transactions still commit because events remain in `outbox_events`.
- Notification unavailable: RabbitMQ keeps queued messages.
- Email provider unavailable: Notification retries the provider call and then dead-letters after configured attempts.
- Duplicate RabbitMQ delivery: processed event IDs prevent repeated sends after successful processing.

Known limitation: SMTP does not provide a universal idempotency key. If Notification crashes after the provider accepts an email but before the service records `SENT`, broker redelivery may cause a duplicate send. Future provider integrations should use provider idempotency with `eventId`.
