package com.vednex.notification.events;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class EventEnvelopeValidatorTests {

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final EventEnvelopeValidator validator = new EventEnvelopeValidator();

	@Test
	void acceptsCompleteEnvelope() {
		EventEnvelope envelope = envelope();

		assertThatCode(() -> validator.validate(envelope)).doesNotThrowAnyException();
	}

	@Test
	void rejectsMissingEventId() {
		EventEnvelope envelope = new EventEnvelope(null, EventTypes.EMAIL_VERIFICATION_REQUESTED_V1, 1,
				"identity-service", null, UUID.randomUUID(), UUID.randomUUID(), Instant.now(), objectMapper.createObjectNode());

		assertThatThrownBy(() -> validator.validate(envelope)).isInstanceOf(EventValidationException.class)
				.hasMessageContaining("eventId");
	}

	@Test
	void rejectsMissingDataObject() {
		EventEnvelope envelope = new EventEnvelope(UUID.randomUUID(), EventTypes.EMAIL_VERIFICATION_REQUESTED_V1, 1,
				"identity-service", null, UUID.randomUUID(), UUID.randomUUID(), Instant.now(), null);

		assertThatThrownBy(() -> validator.validate(envelope)).isInstanceOf(EventValidationException.class)
				.hasMessageContaining("data");
	}

	private EventEnvelope envelope() {
		return new EventEnvelope(UUID.randomUUID(), EventTypes.EMAIL_VERIFICATION_REQUESTED_V1, 1,
				"identity-service", null, UUID.randomUUID(), UUID.randomUUID(), Instant.now(), objectMapper.createObjectNode());
	}
}
