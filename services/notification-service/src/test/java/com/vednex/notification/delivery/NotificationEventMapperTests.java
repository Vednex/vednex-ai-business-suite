package com.vednex.notification.delivery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vednex.notification.email.EmailTemplateRenderer;
import com.vednex.notification.email.PermanentEmailDeliveryException;
import com.vednex.notification.events.EventEnvelope;
import com.vednex.notification.events.EventTypes;
import org.junit.jupiter.api.Test;

class NotificationEventMapperTests {

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final NotificationEventMapper mapper = new NotificationEventMapper(new EmailTemplateRenderer());

	@Test
	void mapsVerificationEvent() {
		ObjectNode data = objectMapper.createObjectNode()
				.put("recipientEmail", "user@example.com")
				.put("verificationUrl", "http://localhost:3000/verify-email?token=secret")
				.put("expiresAt", Instant.now().toString());

		var message = mapper.toEmailMessage(envelope(EventTypes.EMAIL_VERIFICATION_REQUESTED_V1, 1, data));

		assertThat(message.recipient()).isEqualTo("user@example.com");
		assertThat(message.notificationType()).isEqualTo("EMAIL_VERIFICATION");
		assertThat(message.body()).contains("/verify-email?token=secret");
	}

	@Test
	void mapsPasswordResetEvent() {
		ObjectNode data = objectMapper.createObjectNode()
				.put("recipientEmail", "user@example.com")
				.put("resetUrl", "http://localhost:3000/reset-password?token=secret");

		var message = mapper.toEmailMessage(envelope(EventTypes.PASSWORD_RESET_REQUESTED_V1, 1, data));

		assertThat(message.notificationType()).isEqualTo("PASSWORD_RESET");
		assertThat(message.body()).contains("/reset-password?token=secret");
	}

	@Test
	void mapsInvitationEvent() {
		ObjectNode data = objectMapper.createObjectNode()
				.put("recipientEmail", "employee@example.com")
				.put("invitationUrl", "http://localhost:3000/invitations/accept?token=secret");

		var message = mapper.toEmailMessage(envelope(EventTypes.USER_INVITED_V1, 1, data));

		assertThat(message.notificationType()).isEqualTo("USER_INVITATION");
		assertThat(message.body()).contains("/invitations/accept?token=secret");
	}

	@Test
	void rejectsUnsupportedVersion() {
		ObjectNode data = objectMapper.createObjectNode()
				.put("recipientEmail", "user@example.com")
				.put("verificationUrl", "http://localhost");

		assertThatThrownBy(() -> mapper.toEmailMessage(envelope(EventTypes.EMAIL_VERIFICATION_REQUESTED_V1, 2, data)))
				.isInstanceOf(PermanentEmailDeliveryException.class);
	}

	private EventEnvelope envelope(String eventType, int version, ObjectNode data) {
		return new EventEnvelope(UUID.randomUUID(), eventType, version, "identity-service",
				null, UUID.randomUUID(), UUID.randomUUID(), Instant.now(), data);
	}
}
