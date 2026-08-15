package com.vednex.business_suite.events;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class OutboxDomainEventPublisher implements DomainEventPublisher {

	private final OutboxJdbcRepository outboxRepository;
	private final ObjectMapper objectMapper;
	private final Clock clock;
	private final String producer;

	public OutboxDomainEventPublisher(
			OutboxJdbcRepository outboxRepository,
			ObjectMapper objectMapper,
			Clock clock,
			@Value("${app.events.producer}") String producer
	) {
		this.outboxRepository = outboxRepository;
		this.objectMapper = objectMapper;
		this.clock = clock;
		this.producer = producer;
	}

	@Override
	public void publish(String aggregateType, UUID aggregateId, String eventType, int eventVersion,
			UUID companyId, UUID userId, UUID correlationId, Map<String, Object> data) {
		try {
			Instant occurredAt = Instant.now(clock);
			EventEnvelope envelope = new EventEnvelope(
					UUID.randomUUID(),
					eventType,
					eventVersion,
					producer,
					companyId,
					userId,
					correlationId == null ? UUID.randomUUID() : correlationId,
					occurredAt,
					EventPayloadSanitizer.requireSafe(data)
			);
			outboxRepository.save(aggregateType, aggregateId, envelope, objectMapper.writeValueAsString(envelope));
		}
		catch (IllegalArgumentException ex) {
			throw ex;
		}
		catch (Exception ex) {
			throw new IllegalStateException("Unable to persist domain event", ex);
		}
	}
}
