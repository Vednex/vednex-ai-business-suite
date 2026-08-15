# Events

Identity publishes domain events through the transactional outbox. RabbitMQ routes them through the `vednex.events` topic exchange.

Current event-driven email flow:

```text
Identity business transaction
  -> identity_db business rows
  -> identity_db outbox_events
  -> RabbitMQ vednex.events
  -> vednex.notification.identity
  -> Notification Service
  -> notification_db processed_events / notification_deliveries
  -> Email provider
```

Notification Service currently consumes:

```text
identity.email.verification.requested.v1
identity.password.reset.requested.v1
identity.user.invited.v1
```

Identity still owns users, passwords, verification/reset/invitation token validation, company membership, RBAC, and audit logging. Notification owns email rendering, delivery attempts, idempotency records, and delivery status.
