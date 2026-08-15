package com.vednex.notification.delivery;

import com.fasterxml.jackson.databind.JsonNode;
import com.vednex.notification.email.EmailMessage;
import com.vednex.notification.email.EmailTemplateRenderer;
import com.vednex.notification.email.PermanentEmailDeliveryException;
import com.vednex.notification.events.EventEnvelope;
import com.vednex.notification.events.EventTypes;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventMapper {

	private final EmailTemplateRenderer templates;

	public NotificationEventMapper(EmailTemplateRenderer templates) {
		this.templates = templates;
	}

	public EmailMessage toEmailMessage(EventEnvelope event) {
		return switch (event.eventType()) {
			case EventTypes.EMAIL_VERIFICATION_REQUESTED_V1 -> {
				requireVersion(event, 1);
				yield templates.verificationEmail(requiredText(event.data(), "recipientEmail"),
						requiredText(event.data(), "verificationUrl"));
			}
			case EventTypes.PASSWORD_RESET_REQUESTED_V1 -> {
				requireVersion(event, 1);
				yield templates.passwordResetEmail(requiredText(event.data(), "recipientEmail"),
						requiredText(event.data(), "resetUrl"));
			}
			case EventTypes.USER_INVITED_V1 -> {
				requireVersion(event, 1);
				yield templates.invitationEmail(requiredText(event.data(), "recipientEmail"),
						requiredText(event.data(), "invitationUrl"));
			}
			default -> throw new PermanentEmailDeliveryException("Unsupported notification event type");
		};
	}

	private void requireVersion(EventEnvelope event, int expectedVersion) {
		if (event.eventVersion() != expectedVersion) {
			throw new PermanentEmailDeliveryException("Unsupported notification event version");
		}
	}

	private String requiredText(JsonNode data, String fieldName) {
		JsonNode value = data.get(fieldName);
		if (value == null || value.isNull() || value.asText().isBlank()) {
			throw new PermanentEmailDeliveryException("Missing required notification payload field: " + fieldName);
		}
		return value.asText();
	}
}
