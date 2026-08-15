package com.vednex.notification.events;

import org.springframework.stereotype.Component;

@Component
public class EventEnvelopeValidator {

	public void validate(EventEnvelope event) {
		if (event == null) {
			throw new EventValidationException("Event envelope is required");
		}
		if (event.eventId() == null) {
			throw new EventValidationException("eventId is required");
		}
		if (event.eventType() == null || event.eventType().isBlank()) {
			throw new EventValidationException("eventType is required");
		}
		if (event.eventVersion() <= 0) {
			throw new EventValidationException("eventVersion is required");
		}
		if (event.producer() == null || event.producer().isBlank()) {
			throw new EventValidationException("producer is required");
		}
		if (event.correlationId() == null) {
			throw new EventValidationException("correlationId is required");
		}
		if (event.occurredAt() == null) {
			throw new EventValidationException("occurredAt is required");
		}
		if (event.data() == null || event.data().isNull() || !event.data().isObject()) {
			throw new EventValidationException("data object is required");
		}
	}
}
