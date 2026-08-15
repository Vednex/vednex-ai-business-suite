package com.vednex.notification.delivery;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import com.vednex.notification.email.EmailMessage;
import com.vednex.notification.email.EmailSendResult;
import com.vednex.notification.email.EmailSender;
import com.vednex.notification.email.PermanentEmailDeliveryException;
import com.vednex.notification.email.TransientEmailDeliveryException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;

class EmailDeliveryServiceTests {

	private final NotificationDeliveryRepository repository = org.mockito.Mockito.mock(NotificationDeliveryRepository.class);
	private final NotificationMetrics metrics = new NotificationMetrics(new SimpleMeterRegistry());
	private final NotificationDelivery delivery = new NotificationDelivery(
			UUID.randomUUID(),
			"identity.email.verification.requested.v1",
			"EMAIL_VERIFICATION",
			"EMAIL",
			"user@example.com",
			"email-verification-v1"
	);
	private final EmailMessage message = new EmailMessage("user@example.com", "Subject", "Body", "EMAIL_VERIFICATION", "email-verification-v1");

	@Test
	void successfulSendMarksEventProcessed() {
		EmailSender sender = ignored -> new EmailSendResult("provider-1");
		when(repository.incrementAttempt(delivery.eventId())).thenReturn(1);

		new EmailDeliveryService(sender, repository, metrics, 3, 0).send(delivery, message);

		verify(repository).markSentAndProcessed(delivery.eventId(), "provider-1");
	}

	@Test
	void transientFailureRetriesThenSucceeds() {
		AtomicInteger attempts = new AtomicInteger();
		EmailSender sender = ignored -> {
			if (attempts.incrementAndGet() == 1) {
				throw new TransientEmailDeliveryException("temporary", null);
			}
			return EmailSendResult.accepted();
		};
		when(repository.incrementAttempt(delivery.eventId())).thenReturn(1, 2);

		new EmailDeliveryService(sender, repository, metrics, 2, 0).send(delivery, message);

		verify(repository).markSentAndProcessed(delivery.eventId(), null);
	}

	@Test
	void permanentFailureMarksPermanentAndDeadLetters() {
		EmailSender sender = ignored -> {
			throw new PermanentEmailDeliveryException("invalid recipient");
		};
		when(repository.incrementAttempt(delivery.eventId())).thenReturn(1);

		assertThatThrownBy(() -> new EmailDeliveryService(sender, repository, metrics, 3, 0).send(delivery, message))
				.isInstanceOf(AmqpRejectAndDontRequeueException.class);

		verify(repository).markFailed(delivery.eventId(), NotificationStatus.PERMANENT_FAILED, "invalid recipient");
	}

	@Test
	void transientFailureStopsAfterConfiguredAttempts() {
		EmailSender sender = ignored -> {
			throw new TransientEmailDeliveryException("temporary", null);
		};
		when(repository.incrementAttempt(delivery.eventId())).thenReturn(1, 2);

		assertThatThrownBy(() -> new EmailDeliveryService(sender, repository, metrics, 2, 0).send(delivery, message))
				.isInstanceOf(AmqpRejectAndDontRequeueException.class);

		verify(repository).markFailed(delivery.eventId(), NotificationStatus.FAILED, "temporary");
	}
}
