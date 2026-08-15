package com.vednex.notification.events;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;

public record EventEnvelope(
		UUID eventId,
		String eventType,
		int eventVersion,
		String producer,
		UUID companyId,
		UUID userId,
		UUID correlationId,
		Instant occurredAt,
		JsonNode data
) {
}
