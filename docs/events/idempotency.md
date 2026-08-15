# Event Consumer Idempotency

All future event consumers must be idempotent because Vednex uses at-least-once event delivery.

Recommended consumer table:

```text
processed_events
```

Recommended fields:

```text
event_id UUID NOT NULL
consumer_name VARCHAR NOT NULL
processed_at TIMESTAMP WITH TIME ZONE NOT NULL
```

Use a unique key on:

```text
event_id, consumer_name
```

General consumer flow:

1. Start a database transaction.
2. Insert `(event_id, consumer_name)`.
3. If the insert conflicts, skip processing because the event was already handled.
4. Apply business changes.
5. Commit.
6. Acknowledge the RabbitMQ message.

Do not create consumer-specific idempotency tables inside Identity Service unless Identity itself becomes an event consumer.

Notification Service uses the same `processed_events` contract and also records delivery state in `notification_deliveries`.

Email provider calls cannot participate in the same database transaction as RabbitMQ acknowledgement. The current implementation prevents duplicate sends after an event is marked processed. A crash after SMTP acceptance but before the delivery is marked `SENT` can still lead to a duplicate if the broker redelivers the message. Future provider integrations should use provider-supported idempotency keys when available, using `eventId` as the key.
