# RabbitMQ And Transactional Outbox

## Topology

Domain events are published to one topic exchange:

```text
vednex.events
```

Dead-letter infrastructure:

```text
vednex.events.dlx
vednex.events.dlq
```

Current bootstrap archive queue:

```text
vednex.events.identity.archive
```

It is bound to:

```text
identity.#
```

The archive queue prevents current Identity events from being unrouted while no future service consumers exist yet. It is not a Notification Service.

Current Notification Service queue:

```text
vednex.notification.identity
```

It is bound only to:

```text
identity.email.verification.requested.v1
identity.password.reset.requested.v1
identity.user.invited.v1
```

Malformed notification events, unsupported versions, permanent delivery failures, and exhausted provider retries are rejected without requeue and routed to `vednex.events.dlq` through `vednex.events.dlx`.

## Transactional Outbox

Identity writes domain events to `outbox_events` inside the same PostgreSQL transaction as business data.

Flow:

```text
Business data write
Outbox row write
Commit
Scheduled publisher
RabbitMQ publish
Publisher confirm
Mark PUBLISHED
```

If RabbitMQ is unavailable, business transactions still commit and events remain `PENDING` for retry.

## Concurrency

The publisher uses PostgreSQL row locks:

```text
FOR UPDATE SKIP LOCKED
```

This allows multiple Identity instances to run publishers without processing the same row concurrently.

## Retry

Configuration:

```text
OUTBOX_POLL_INTERVAL_MS
OUTBOX_BATCH_SIZE
OUTBOX_MAX_RETRIES
OUTBOX_RETRY_BASE_SECONDS
OUTBOX_PUBLISH_CONFIRM_TIMEOUT_MS
```

Retries use bounded exponential backoff. After max retries, events move to `FAILED` for manual inspection or replay.

## Reliability Limitations

Publishing is at-least-once. If the process crashes after RabbitMQ accepts a message but before PostgreSQL marks it `PUBLISHED`, the event can be published again. Consumers must be idempotent.

## Health

RabbitMQ is currently an optional dependency for core Identity operations. The default RabbitMQ health contributor is disabled so a temporary broker outage does not make the Identity application health endpoint report the core auth service as down. Broker failures are visible through outbox retry/failed metrics, logs, RabbitMQ Management UI, and `outbox_events` rows.
