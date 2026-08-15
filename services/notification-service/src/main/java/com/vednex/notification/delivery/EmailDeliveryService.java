package com.vednex.notification.delivery;

import java.util.UUID;

import com.vednex.notification.email.EmailSendResult;
import com.vednex.notification.email.EmailSender;
import com.vednex.notification.email.PermanentEmailDeliveryException;
import com.vednex.notification.email.TransientEmailDeliveryException;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailDeliveryService {

	private final EmailSender emailSender;
	private final NotificationDeliveryRepository repository;
	private final NotificationMetrics metrics;
	private final int maxAttempts;
	private final long retryBaseMs;

	public EmailDeliveryService(
			EmailSender emailSender,
			NotificationDeliveryRepository repository,
			NotificationMetrics metrics,
			@Value("${app.notifications.email-max-attempts}") int maxAttempts,
			@Value("${app.notifications.email-retry-base-ms}") long retryBaseMs) {
		this.emailSender = emailSender;
		this.repository = repository;
		this.metrics = metrics;
		this.maxAttempts = Math.max(1, maxAttempts);
		this.retryBaseMs = Math.max(0, retryBaseMs);
	}

	public void send(NotificationDelivery delivery, com.vednex.notification.email.EmailMessage message) {
		for (int attempt = 1; attempt <= maxAttempts; attempt++) {
			repository.incrementAttempt(delivery.eventId());
			try {
				EmailSendResult result = emailSender.send(message);
				repository.markSentAndProcessed(delivery.eventId(), result.providerMessageId());
				metrics.sent(delivery.notificationType());
				return;
			}
			catch (PermanentEmailDeliveryException ex) {
				repository.markFailed(delivery.eventId(), NotificationStatus.PERMANENT_FAILED, ex.getMessage());
				metrics.failed(delivery.notificationType());
				metrics.deadlettered(delivery.notificationType());
				throw new AmqpRejectAndDontRequeueException("Permanent notification failure", ex);
			}
			catch (TransientEmailDeliveryException ex) {
				if (attempt >= maxAttempts) {
					repository.markFailed(delivery.eventId(), NotificationStatus.FAILED, ex.getMessage());
					metrics.failed(delivery.notificationType());
					metrics.deadlettered(delivery.notificationType());
					throw new AmqpRejectAndDontRequeueException("Notification retry limit reached", ex);
				}
				sleepBeforeRetry(attempt);
			}
		}
		throw new AmqpRejectAndDontRequeueException("Notification retry limit reached for event " + safeEventId(delivery.eventId()));
	}

	private void sleepBeforeRetry(int attempt) {
		if (retryBaseMs == 0) {
			return;
		}
		try {
			Thread.sleep(retryBaseMs * attempt);
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new TransientEmailDeliveryException("Interrupted while retrying notification delivery", ex);
		}
	}

	private String safeEventId(UUID eventId) {
		return eventId == null ? "" : eventId.toString();
	}
}
