package com.vednex.notification.delivery;

import java.util.UUID;

import com.vednex.notification.email.EmailMessage;
import com.vednex.notification.events.EventEnvelope;

public record NotificationDelivery(
		UUID eventId,
		String eventType,
		String notificationType,
		String channel,
		String recipient,
		String templateKey
) {

	public static NotificationDelivery from(EventEnvelope event, EmailMessage message) {
		return new NotificationDelivery(
				event.eventId(),
				event.eventType(),
				message.notificationType(),
				"EMAIL",
				message.recipient(),
				message.templateKey()
		);
	}
}
