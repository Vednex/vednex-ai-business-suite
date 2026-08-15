package com.vednex.notification.delivery;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vednex.notification.email.EmailTemplateRenderer;
import com.vednex.notification.events.EventEnvelope;
import com.vednex.notification.events.EventEnvelopeValidator;
import com.vednex.notification.events.EventTypes;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationEventConsumerTests {

	private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
	private final NotificationDeliveryRepository repository = org.mockito.Mockito.mock(NotificationDeliveryRepository.class);
	private final EmailDeliveryService deliveryService = org.mockito.Mockito.mock(EmailDeliveryService.class);
	private final NotificationMetrics metrics = new NotificationMetrics(new SimpleMeterRegistry());
	private final NotificationEventConsumer consumer = new NotificationEventConsumer(
			objectMapper,
			new EventEnvelopeValidator(),
			new NotificationEventMapper(new EmailTemplateRenderer()),
			repository,
			deliveryService,
			metrics
	);

	@Test
	void validVerificationEventStartsDelivery() throws Exception {
		EventEnvelope event = verificationEvent();
		when(repository.isProcessed(event.eventId())).thenReturn(false);
		when(repository.begin(any())).thenReturn(true);

		consumer.consume(objectMapper.writeValueAsString(event));

		verify(deliveryService).send(any(), any());
	}

	@Test
	void duplicateEventDoesNotSendEmailAgain() throws Exception {
		EventEnvelope event = verificationEvent();
		when(repository.isProcessed(event.eventId())).thenReturn(true);

		consumer.consume(objectMapper.writeValueAsString(event));

		verify(deliveryService, never()).send(any(), any());
	}

	@Test
	void malformedJsonIsRejectedForDeadLettering() {
		assertThatThrownBy(() -> consumer.consume("{bad-json"))
				.isInstanceOf(AmqpRejectAndDontRequeueException.class);
	}

	@Test
	void unsupportedVersionIsRejectedForDeadLettering() throws Exception {
		EventEnvelope event = new EventEnvelope(UUID.randomUUID(), EventTypes.EMAIL_VERIFICATION_REQUESTED_V1, 2,
				"identity-service", null, UUID.randomUUID(), UUID.randomUUID(), Instant.now(), verificationData());

		assertThatThrownBy(() -> consumer.consume(objectMapper.writeValueAsString(event)))
				.isInstanceOf(AmqpRejectAndDontRequeueException.class);
	}

	private EventEnvelope verificationEvent() {
		return new EventEnvelope(UUID.randomUUID(), EventTypes.EMAIL_VERIFICATION_REQUESTED_V1, 1,
				"identity-service", null, UUID.randomUUID(), UUID.randomUUID(), Instant.now(), verificationData());
	}

	private ObjectNode verificationData() {
		return objectMapper.createObjectNode()
				.put("recipientEmail", "user@example.com")
				.put("verificationUrl", "http://localhost:3000/verify-email?token=secret")
				.put("expiresAt", Instant.now().toString());
	}
}
