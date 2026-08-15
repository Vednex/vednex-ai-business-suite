package com.vednex.notification.email;

import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

public class SmtpEmailSender implements EmailSender {

	private final JavaMailSender mailSender;
	private final String from;

	public SmtpEmailSender(JavaMailSender mailSender, String from) {
		this.mailSender = mailSender;
		this.from = from;
	}

	@Override
	public EmailSendResult send(EmailMessage message) {
		try {
			SimpleMailMessage mail = new SimpleMailMessage();
			mail.setFrom(from);
			mail.setTo(message.recipient());
			mail.setSubject(message.subject());
			mail.setText(message.body());
			mailSender.send(mail);
			return EmailSendResult.accepted();
		}
		catch (MailException ex) {
			throw new TransientEmailDeliveryException("Email provider rejected or failed delivery request", ex);
		}
	}
}
