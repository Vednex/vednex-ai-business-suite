package com.vednex.business_suite.events;

import java.time.Instant;
import java.util.UUID;

public record OutboxEvent(
		UUID id,
		String eventType,
		int eventVersion,
		String payload,
		UUID correlationId,
		int retryCount,
		Instant createdAt
) {
}
