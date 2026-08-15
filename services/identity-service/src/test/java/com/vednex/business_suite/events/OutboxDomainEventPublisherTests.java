package com.vednex.business_suite.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class OutboxDomainEventPublisherTests {

	@Test
	void persistsOutboxEventEnvelopeWithCorrelationId() {
		OutboxJdbcRepository repository = mock(OutboxJdbcRepository.class);
		ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
		Clock clock = Clock.fixed(Instant.parse("2026-08-14T12:00:00Z"), ZoneOffset.UTC);
		OutboxDomainEventPublisher publisher = new OutboxDomainEventPublisher(repository, objectMapper, clock, "identity-service");
		UUID aggregateId = UUID.randomUUID();
		UUID companyId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		UUID correlationId = UUID.randomUUID();

		publisher.publish("User", aggregateId, EventTypes.IDENTITY_USER_REGISTERED_V1, 1,
				companyId, userId, correlationId, Map.of("email", "owner@example.com"));

		ArgumentCaptor<EventEnvelope> envelopeCaptor = ArgumentCaptor.forClass(EventEnvelope.class);
		ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
		verify(repository).save(eq("User"), eq(aggregateId), envelopeCaptor.capture(), payloadCaptor.capture());
		EventEnvelope envelope = envelopeCaptor.getValue();
		assertThat(envelope.eventType()).isEqualTo(EventTypes.IDENTITY_USER_REGISTERED_V1);
		assertThat(envelope.producer()).isEqualTo("identity-service");
		assertThat(envelope.companyId()).isEqualTo(companyId);
		assertThat(envelope.userId()).isEqualTo(userId);
		assertThat(envelope.correlationId()).isEqualTo(correlationId);
		assertThat(envelope.occurredAt()).isEqualTo(Instant.parse("2026-08-14T12:00:00Z"));
		assertThat(payloadCaptor.getValue()).contains("\"eventType\":\"identity.user.registered.v1\"");
		assertThat(payloadCaptor.getValue()).doesNotContain("@class");
	}
}
