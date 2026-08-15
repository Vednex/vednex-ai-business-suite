# Event Envelope

All events published to RabbitMQ use a language-neutral JSON envelope:

```json
{
  "eventId": "uuid",
  "eventType": "identity.user.registered.v1",
  "eventVersion": 1,
  "producer": "identity-service",
  "companyId": "uuid-or-null",
  "userId": "uuid-or-null",
  "correlationId": "uuid",
  "occurredAt": "2026-08-14T17:00:00Z",
  "data": {}
}
```

Required metadata:

- `eventId`
- `eventType`
- `eventVersion`
- `producer`
- `correlationId`
- `occurredAt`
- `data`

Optional metadata:

- `companyId`
- `userId`

Do not include:

- Passwords
- Password hashes
- Access tokens
- Refresh tokens
- Verification or reset token values
- Verification or reset token hashes
- RSA private keys
- Database credentials
- Raw authorization headers

RabbitMQ headers duplicate safe routing metadata:

```text
eventId
eventType
eventVersion
correlationId
producer
```

The JSON envelope remains the source of truth.

Exception for notification request events:

- `identity.email.verification.requested.v1`
- `identity.password.reset.requested.v1`
- `identity.user.invited.v1`

These events may contain one-time email URLs with raw verification, reset, or invitation tokens because Notification Service must deliver the user-facing link. Treat those events as sensitive in transit and at rest:

- Do not log full URLs.
- Do not log raw token values.
- Do not copy token URLs into generic audit metadata.
- Use RabbitMQ credentials and TLS in production.
- Keep Identity token validation and hashed token storage unchanged.
- Identity redacts the sensitive `data` payload in `outbox_events` after RabbitMQ confirms publication.
