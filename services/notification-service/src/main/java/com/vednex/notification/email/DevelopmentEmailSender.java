package com.vednex.notification.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DevelopmentEmailSender implements EmailSender {

	private static final Logger log = LoggerFactory.getLogger(DevelopmentEmailSender.class);

	private final boolean logTokenLinks;

	public DevelopmentEmailSender(boolean logTokenLinks) {
		this.logTokenLinks = logTokenLinks;
	}

	@Override
	public EmailSendResult send(EmailMessage message) {
		if (logTokenLinks) {
			log.info("Development {} email prepared for {} with subject '{}'",
					message.notificationType(), EmailAddressMasker.mask(message.recipient()), message.subject());
		}
		else {
			log.info("Development {} email prepared for {}. Token URLs are not logged.",
					message.notificationType(), EmailAddressMasker.mask(message.recipient()));
		}
		return EmailSendResult.accepted();
	}
}
