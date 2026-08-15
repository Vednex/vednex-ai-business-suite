package com.vednex.business_suite.events;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record EventEnvelope(
		UUID eventId,
		String eventType,
		int eventVersion,
		String producer,
		UUID companyId,
		UUID userId,
		UUID correlationId,
		Instant occurredAt,
		Map<String, Object> data
) {
	public EventEnvelope {
		if (eventId == null) {
			throw new IllegalArgumentException("eventId is required");
		}
		if (eventType == null || eventType.isBlank()) {
			throw new IllegalArgumentException("eventType is required");
		}
		if (eventVersion < 1) {
			throw new IllegalArgumentException("eventVersion must be positive");
		}
		if (producer == null || producer.isBlank()) {
			throw new IllegalArgumentException("producer is required");
		}
		if (correlationId == null) {
			throw new IllegalArgumentException("correlationId is required");
		}
		if (occurredAt == null) {
			throw new IllegalArgumentException("occurredAt is required");
		}
		data = data == null ? Map.of() : Map.copyOf(data);
	}
}
