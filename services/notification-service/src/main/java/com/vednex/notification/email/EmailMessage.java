package com.vednex.notification.email;

public record EmailMessage(
		String recipient,
		String subject,
		String body,
		String notificationType,
		String templateKey
) {
}
