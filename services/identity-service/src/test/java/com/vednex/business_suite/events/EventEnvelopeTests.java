package com.vednex.business_suite.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.Test;

class EventEnvelopeTests {

	private final ObjectMapper objectMapper = new ObjectMapper()
			.findAndRegisterModules()
			.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

	@Test
	void createsLanguageNeutralJsonEnvelope() throws Exception {
		UUID eventId = UUID.randomUUID();
		UUID companyId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		UUID correlationId = UUID.randomUUID();
		EventEnvelope envelope = new EventEnvelope(
				eventId,
				EventTypes.IDENTITY_USER_REGISTERED_V1,
				1,
				"identity-service",
				companyId,
				userId,
				correlationId,
				Instant.parse("2026-08-14T12:00:00Z"),
				Map.of("email", "owner@example.com", "status", "PENDING_VERIFICATION")
		);

		String json = objectMapper.writeValueAsString(envelope);

		assertThat(json).contains(
				"\"eventId\":\"" + eventId + "\"",
				"\"eventType\":\"identity.user.registered.v1\"",
				"\"eventVersion\":1",
				"\"producer\":\"identity-service\"",
				"\"companyId\":\"" + companyId + "\"",
				"\"userId\":\"" + userId + "\"",
				"\"correlationId\":\"" + correlationId + "\"",
				"\"occurredAt\":\"2026-08-14T12:00:00Z\"",
				"\"data\"");
		assertThat(json).doesNotContain("@class");
	}

	@Test
	void rejectsSensitivePayloadKeys() {
		assertThatThrownBy(() -> EventPayloadSanitizer.requireSafe(Map.of("resetToken", "secret-value")))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("forbidden key");
	}
}
